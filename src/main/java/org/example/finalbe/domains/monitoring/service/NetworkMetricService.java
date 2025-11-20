package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.AggregationLevel;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.dto.*;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 네트워크 메트릭 서비스
 * 네트워크 관련 대시보드 데이터 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NetworkMetricService {

    private final NetworkMetricRepository networkMetricRepository;
    private final CpuMetricService cpuMetricService; // AggregationLevel 헬퍼용

    /**
     * 네트워크 섹션 전체 데이터 조회
     */
    public NetworkSectionResponseDto getNetworkSectionData(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 네트워크 섹션 데이터 조회 시작 - 장비 ID: {}, 기간: {} ~ {}, 집계: {}",
                equipmentId, startTime, endTime, aggregationLevel);

        // 1. 현재 상태 조회
        NetworkCurrentStatsDto currentStats = getCurrentNetworkStats(equipmentId, startTime, endTime);

        // 2. 집계 레벨에 따른 데이터 조회
        List<NetworkMetric> metrics;
        List<NetworkAggregatedStatsDto> aggregatedData;

        switch (aggregationLevel) {
            case MIN:
                aggregatedData = getNetworkAggregatedData1Minute(equipmentId, startTime, endTime);
                return buildNetworkSectionFromAggregated(currentStats, aggregatedData);
            case MIN5:
                aggregatedData = getNetworkAggregatedData5Minutes(equipmentId, startTime, endTime);
                return buildNetworkSectionFromAggregated(currentStats, aggregatedData);
            case HOUR:
                aggregatedData = getNetworkAggregatedData1Hour(equipmentId, startTime, endTime);
                return buildNetworkSectionFromAggregated(currentStats, aggregatedData);
            case DAY: 
                aggregatedData = getNetworkAggregatedData1Day(equipmentId, startTime, endTime);
                return buildNetworkSectionFromAggregated(currentStats, aggregatedData);
            case RAW:
            default:
                metrics = networkMetricRepository.findByEquipmentIdAndTimeRange(
                        equipmentId, startTime, endTime);
                return buildNetworkSectionFromRaw(currentStats, metrics);
        }
    }

    private List<NetworkAggregatedStatsDto> getNetworkAggregatedData1Day(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = networkMetricRepository.getNetworkAggregatedStats1Day(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToNetworkAggregatedStats) // 기존 매퍼 재활용
                .collect(Collectors.toList());
    }

    /**
     * 현재 네트워크 상태 조회 (게이지용) - 모든 NIC 합산/평균
     */
    public NetworkCurrentStatsDto getCurrentNetworkStats(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // 최신 메트릭 조회 (장비의 모든 NIC 포함)
        List<NetworkMetric> latestMetrics = networkMetricRepository
                .findLatestByEquipmentId(equipmentId);

        if (latestMetrics.isEmpty()) {
            throw new RuntimeException("메트릭 데이터가 없습니다.");
        }

        // 최신 값 합산/평균
        double currentInBps = 0;
        double currentOutBps = 0;
        long totalInErrors = 0;
        long totalOutErrors = 0;
        LocalDateTime lastUpdated = latestMetrics.get(0).getGenerateTime();

        for (NetworkMetric metric : latestMetrics) {
            currentInBps += (metric.getInBytesPerSec() != null ? metric.getInBytesPerSec() : 0);
            currentOutBps += (metric.getOutBytesPerSec() != null ? metric.getOutBytesPerSec() : 0);
            totalInErrors += (metric.getInErrorPktsTot() != null ? metric.getInErrorPktsTot() : 0);
            totalOutErrors += (metric.getOutErrorPktsTot() != null ? metric.getOutErrorPktsTot() : 0);
        }

        // 통계 조회 (사용률 기준)
        Object[] stats = networkMetricRepository.getNetworkUsageStats(equipmentId, startTime, endTime);

        Double avgRx = 0.0;
        Double maxRx = 0.0;
        Double minRx = 0.0;

        if (stats != null && stats.length > 0) {
            Object firstElement = stats[0];
            if (firstElement instanceof Object[]) {
                Object[] innerArray = (Object[]) firstElement;
                if (innerArray.length >= 3) {
                    avgRx = convertToDouble(innerArray[0]);
                    maxRx = convertToDouble(innerArray[1]);
                    minRx = convertToDouble(innerArray[2]);
                }
            } else if (stats.length >= 3) {
                avgRx = convertToDouble(stats[0]);
                maxRx = convertToDouble(stats[1]);
                minRx = convertToDouble(stats[2]);
            }
        }

        return NetworkCurrentStatsDto.builder()
                .currentInBps(currentInBps)
                .currentOutBps(currentOutBps)
                .avgRxUsage(avgRx)
                .maxRxUsage(maxRx)
                .minRxUsage(minRx)
                .totalInErrors(totalInErrors)
                .totalOutErrors(totalOutErrors)
                .lastUpdated(lastUpdated)
                .build();
    }

    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 1분 단위 집계 데이터 조회
     */
    private List<NetworkAggregatedStatsDto> getNetworkAggregatedData1Minute(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {
        return networkMetricRepository.getNetworkAggregatedStats1Minute(equipmentId, startTime, endTime)
                .stream()
                .map(this::mapToNetworkAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 5분 단위 집계 데이터 조회
     */
    private List<NetworkAggregatedStatsDto> getNetworkAggregatedData5Minutes(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {
        return networkMetricRepository.getNetworkAggregatedStats5Minutes(equipmentId, startTime, endTime)
                .stream()
                .map(this::mapToNetworkAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 1시간 단위 집계 데이터 조회
     */
    private List<NetworkAggregatedStatsDto> getNetworkAggregatedData1Hour(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {
        return networkMetricRepository.getNetworkAggregatedStats1Hour(equipmentId, startTime, endTime)
                .stream()
                .map(this::mapToNetworkAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * Object[] → NetworkAggregatedStatsDto 매핑
     */
    private NetworkAggregatedStatsDto mapToNetworkAggregatedStats(Object[] row) {
        return NetworkAggregatedStatsDto.builder()
                .timestamp(((Timestamp) row[0]).toLocalDateTime())
                .totalInBps(convertToDouble(row[1]))
                .totalOutBps(convertToDouble(row[2]))
                .avgRxUsage(convertToDouble(row[3]))
                .avgTxUsage(convertToDouble(row[4]))
                .sampleCount(row[5] != null ? ((Number) row[5]).intValue() : 0)
                .build();
    }

    /**
     * RAW 데이터로부터 네트워크 섹션 응답 생성
     * (동일 시간대의 모든 NIC 데이터를 합산/평균)
     */
    private NetworkSectionResponseDto buildNetworkSectionFromRaw(
            NetworkCurrentStatsDto currentStats,
            List<NetworkMetric> metrics) {

        // 1. 타임스탬프 기준으로 모든 NIC 데이터를 그룹화
        Map<LocalDateTime, List<NetworkMetric>> metricsByTime = metrics.stream()
                .collect(Collectors.groupingBy(NetworkMetric::getGenerateTime));

        List<NetworkTrafficPointDto> trafficTrend = new ArrayList<>();
        List<NetworkUsagePointDto> usageTrend = new ArrayList<>();
        List<NetworkErrorPointDto> errorTrend = new ArrayList<>();

        // 2. 타임스탬프별로 루프를 돌며 합산/평균
        for (Map.Entry<LocalDateTime, List<NetworkMetric>> entry : metricsByTime.entrySet()) {
            LocalDateTime timestamp = entry.getKey();
            List<NetworkMetric> nicsAtTime = entry.getValue();

            double sumInBps = 0, sumOutBps = 0, sumRxUsage = 0, sumTxUsage = 0;
            long sumInErrors = 0, sumOutErrors = 0, sumInDiscards = 0, sumOutDiscards = 0;
            int nicCount = nicsAtTime.size();

            for (NetworkMetric nicMetric : nicsAtTime) {
                sumInBps += (nicMetric.getInBytesPerSec() != null ? nicMetric.getInBytesPerSec() : 0);
                sumOutBps += (nicMetric.getOutBytesPerSec() != null ? nicMetric.getOutBytesPerSec() : 0);
                sumRxUsage += (nicMetric.getRxUsage() != null ? nicMetric.getRxUsage() : 0);
                sumTxUsage += (nicMetric.getTxUsage() != null ? nicMetric.getTxUsage() : 0);
                sumInErrors += (nicMetric.getInErrorPktsTot() != null ? nicMetric.getInErrorPktsTot() : 0);
                sumOutErrors += (nicMetric.getOutErrorPktsTot() != null ? nicMetric.getOutErrorPktsTot() : 0);
                sumInDiscards += (nicMetric.getInDiscardPktsTot() != null ? nicMetric.getInDiscardPktsTot() : 0);
                sumOutDiscards += (nicMetric.getOutDiscardPktsTot() != null ? nicMetric.getOutDiscardPktsTot() : 0);
            }

            // 3.7 트래픽 (합산)
            trafficTrend.add(NetworkTrafficPointDto.builder()
                    .timestamp(timestamp)
                    .inBps(sumInBps)
                    .outBps(sumOutBps)
                    .build());

            // 3.1, 3.2 사용률 (평균)
            usageTrend.add(NetworkUsagePointDto.builder()
                    .timestamp(timestamp)
                    .rxUsage(nicCount > 0 ? sumRxUsage / nicCount : 0)
                    .txUsage(nicCount > 0 ? sumTxUsage / nicCount : 0)
                    .build());

            // 3.8 에러 (합산, 누적값)
            errorTrend.add(NetworkErrorPointDto.builder()
                    .timestamp(timestamp)
                    .inErrors(sumInErrors)
                    .outErrors(sumOutErrors)
                    .inDiscards(sumInDiscards)
                    .outDiscards(sumOutDiscards)
                    .build());
        }

        return NetworkSectionResponseDto.builder()
                .currentStats(currentStats)
                .trafficTrend(trafficTrend)
                .usageTrend(usageTrend)
                .errorTrend(errorTrend)
                .build();
    }

    /**
     * 집계 데이터로부터 네트워크 섹션 응답 생성
     */
    private NetworkSectionResponseDto buildNetworkSectionFromAggregated(
            NetworkCurrentStatsDto currentStats,
            List<NetworkAggregatedStatsDto> aggregatedData) {

        List<NetworkTrafficPointDto> trafficTrend = aggregatedData.stream()
                .map(agg -> NetworkTrafficPointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .inBps(agg.getTotalInBps())
                        .outBps(agg.getTotalOutBps())
                        .build())
                .collect(Collectors.toList());

        List<NetworkUsagePointDto> usageTrend = aggregatedData.stream()
                .map(agg -> NetworkUsagePointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .rxUsage(agg.getAvgRxUsage())
                        .txUsage(agg.getAvgTxUsage())
                        .build())
                .collect(Collectors.toList());

        return NetworkSectionResponseDto.builder()
                .currentStats(currentStats)
                .trafficTrend(trafficTrend)
                .usageTrend(usageTrend)
                .errorTrend(null) // 집계 데이터에서는 제공 불가
                .build();
    }

    /**
     * (CpuMetricService의 로직 재활용)
     */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
    }

    /**
     * 여러 장비의 현재 네트워크 상태 일괄 조회
     */
    public NetworkCurrentStatsBatchDto getCurrentNetworkStatsBatch(List<Long> equipmentIds) {

        log.info("📊 일괄 네트워크 상태 조회 시작 - 장비 개수: {}", equipmentIds.size());

        List<NetworkStatsWithEquipmentDto> equipmentStatsList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // 1. 모든 장비의 최신 메트릭 일괄 조회 (모든 NIC 포함)
        List<NetworkMetric> latestMetrics = networkMetricRepository
                .findLatestByEquipmentIds(equipmentIds);

        // 장비 ID별로 최신 NIC 리스트를 그룹화
        Map<Long, List<NetworkMetric>> latestMetricsByEquipment = latestMetrics.stream()
                .collect(Collectors.groupingBy(NetworkMetric::getEquipmentId));

        // 2. 모든 장비의 통계 일괄 조회 (사용률 기준, 최근 60개)
        List<Object[]> statsResults = networkMetricRepository
                .getNetworkUsageStatsBatch(equipmentIds, 60);

        Map<Long, Object[]> statsMap = statsResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> new Object[]{row[1], row[2], row[3]} // avgRx, maxRx, minRx
                ));

        // 3. 각 장비별 데이터 조합
        for (Long equipmentId : equipmentIds) {
            try {
                List<NetworkMetric> latestNics = latestMetricsByEquipment.get(equipmentId);

                if (latestNics == null || latestNics.isEmpty()) {
                    equipmentStatsList.add(NetworkStatsWithEquipmentDto.builder()
                            .equipmentId(equipmentId)
                            .success(false)
                            .errorMessage("메트릭 데이터가 없습니다.")
                            .build());
                    failureCount++;
                    continue;
                }

                // 최신 값 합산
                double currentInBps = 0, currentOutBps = 0;
                long totalInErrors = 0, totalOutErrors = 0;
                LocalDateTime lastUpdated = latestNics.get(0).getGenerateTime();

                for (NetworkMetric metric : latestNics) {
                    currentInBps += (metric.getInBytesPerSec() != null ? metric.getInBytesPerSec() : 0);
                    currentOutBps += (metric.getOutBytesPerSec() != null ? metric.getOutBytesPerSec() : 0);
                    totalInErrors += (metric.getInErrorPktsTot() != null ? metric.getInErrorPktsTot() : 0);
                    totalOutErrors += (metric.getOutErrorPktsTot() != null ? metric.getOutErrorPktsTot() : 0);
                }

                // 통계 값 매핑
                Object[] stats = statsMap.get(equipmentId);
                Double avgRx = 0.0, maxRx = 0.0, minRx = 0.0;

                if (stats != null && stats[0] != null) {
                    avgRx = convertToDouble(stats[0]);
                    maxRx = convertToDouble(stats[1]);
                    minRx = convertToDouble(stats[2]);
                } else {
                    log.warn("⚠️ 장비 {}의 네트워크 통계 데이터 없음", equipmentId);
                    // 통계가 없으면 현재 사용률 평균으로 대체
                    double currentRxAvg = 0;
                    for(NetworkMetric m : latestNics) currentRxAvg += (m.getRxUsage() != null ? m.getRxUsage() : 0);
                    avgRx = maxRx = minRx = (latestNics.size() > 0 ? currentRxAvg / latestNics.size() : 0);
                }

                NetworkCurrentStatsDto networkStats = NetworkCurrentStatsDto.builder()
                        .currentInBps(currentInBps)
                        .currentOutBps(currentOutBps)
                        .avgRxUsage(avgRx)
                        .maxRxUsage(maxRx)
                        .minRxUsage(minRx)
                        .totalInErrors(totalInErrors)
                        .totalOutErrors(totalOutErrors)
                        .lastUpdated(lastUpdated)
                        .build();

                equipmentStatsList.add(NetworkStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(true)
                        .networkStats(networkStats)
                        .build());

                successCount++;

            } catch (Exception e) {
                log.error("❌ 장비 {} 네트워크 상태 조회 실패: {}", equipmentId, e.getMessage());
                equipmentStatsList.add(NetworkStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        log.info("✅ 일괄 네트워크 상태 조회 완료 - 성공: {}, 실패: {}", successCount, failureCount);

        return NetworkCurrentStatsBatchDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .equipmentStats(equipmentStatsList)
                .build();
    }
}