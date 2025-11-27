/**
 * 작성자: 최산하
 * 네트워크 메트릭 데이터 조회 및 집계 처리 서비스
 */
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NetworkMetricService {

    private final NetworkMetricRepository networkMetricRepository;
    private final CpuMetricService cpuMetricService;

    /** 네트워크 섹션 전체 데이터 조회 */
    public NetworkSectionResponseDto getNetworkSectionData(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 네트워크 섹션 데이터 조회 - equipmentId={}, 기간: {} ~ {}", equipmentId, startTime, endTime);

        NetworkCurrentStatsDto currentStats = getCurrentNetworkStats(equipmentId, startTime, endTime);

        switch (aggregationLevel) {
            case MIN:
                return buildNetworkSectionFromAggregated(currentStats,
                        getNetworkAggregatedData1Minute(equipmentId, startTime, endTime));
            case MIN5:
                return buildNetworkSectionFromAggregated(currentStats,
                        getNetworkAggregatedData5Minutes(equipmentId, startTime, endTime));
            case HOUR:
                return buildNetworkSectionFromAggregated(currentStats,
                        getNetworkAggregatedData1Hour(equipmentId, startTime, endTime));
            case DAY:
                return buildNetworkSectionFromAggregated(currentStats,
                        getNetworkAggregatedData1Day(equipmentId, startTime, endTime));
            case RAW:
            default:
                return buildNetworkSectionFromRaw(currentStats,
                        networkMetricRepository.findByEquipmentIdAndTimeRange(equipmentId, startTime, endTime));
        }
    }

    /** 1일 단위 네트워크 집계 조회 */
    private List<NetworkAggregatedStatsDto> getNetworkAggregatedData1Day(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        return networkMetricRepository.getNetworkAggregatedStats1Day(equipmentId, startTime, endTime)
                .stream().map(this::mapToNetworkAggregatedStats).collect(Collectors.toList());
    }

    /** 최신 네트워크 상태 조회 (모든 NIC 합산) */
    public NetworkCurrentStatsDto getCurrentNetworkStats(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        List<NetworkMetric> latestMetrics = networkMetricRepository.findLatestByEquipmentId(equipmentId);

        if (latestMetrics.isEmpty()) {
            throw new RuntimeException("메트릭 데이터가 없습니다.");
        }

        double currentInBps = 0, currentOutBps = 0;
        long totalInErrors = 0, totalOutErrors = 0;
        LocalDateTime lastUpdated = latestMetrics.get(0).getGenerateTime();

        for (NetworkMetric metric : latestMetrics) {
            currentInBps += Optional.ofNullable(metric.getInBytesPerSec()).orElse(0.0);
            currentOutBps += Optional.ofNullable(metric.getOutBytesPerSec()).orElse(0.0);
            totalInErrors += Optional.ofNullable(metric.getInErrorPktsTot()).orElse(0L);
            totalOutErrors += Optional.ofNullable(metric.getOutErrorPktsTot()).orElse(0L);
        }

        Object[] stats = networkMetricRepository.getNetworkUsageStats(equipmentId, startTime, endTime);

        Double avgRx = 0.0, maxRx = 0.0, minRx = 0.0;

        if (stats != null && stats.length > 0) {
            Object first = stats[0];
            if (first instanceof Object[]) {
                Object[] arr = (Object[]) first;
                if (arr.length >= 3) {
                    avgRx = convertToDouble(arr[0]);
                    maxRx = convertToDouble(arr[1]);
                    minRx = convertToDouble(arr[2]);
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

    /** 안전한 Double 변환 */
    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    /** 1분 단위 집계 조회 */
    private List<NetworkAggregatedStatsDto> getNetworkAggregatedData1Minute(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        return networkMetricRepository.getNetworkAggregatedStats1Minute(equipmentId, startTime, endTime)
                .stream().map(this::mapToNetworkAggregatedStats).collect(Collectors.toList());
    }

    /** 5분 단위 집계 조회 */
    private List<NetworkAggregatedStatsDto> getNetworkAggregatedData5Minutes(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        return networkMetricRepository.getNetworkAggregatedStats5Minutes(equipmentId, startTime, endTime)
                .stream().map(this::mapToNetworkAggregatedStats).collect(Collectors.toList());
    }

    /** 1시간 단위 집계 조회 */
    private List<NetworkAggregatedStatsDto> getNetworkAggregatedData1Hour(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        return networkMetricRepository.getNetworkAggregatedStats1Hour(equipmentId, startTime, endTime)
                .stream().map(this::mapToNetworkAggregatedStats).collect(Collectors.toList());
    }

    /** Object[] → DTO 매핑 */
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

    /** RAW 데이터 기반 응답 생성 */
    private NetworkSectionResponseDto buildNetworkSectionFromRaw(
            NetworkCurrentStatsDto currentStats, List<NetworkMetric> metrics) {

        Map<LocalDateTime, List<NetworkMetric>> grouped =
                metrics.stream().collect(Collectors.groupingBy(NetworkMetric::getGenerateTime));

        List<NetworkTrafficPointDto> trafficTrend = new ArrayList<>();
        List<NetworkUsagePointDto> usageTrend = new ArrayList<>();
        List<NetworkErrorPointDto> errorTrend = new ArrayList<>();

        grouped.forEach((timestamp, list) -> {
            double sumIn = 0, sumOut = 0, sumRx = 0, sumTx = 0;
            long sumInErr = 0, sumOutErr = 0, sumInDisc = 0, sumOutDisc = 0;

            for (NetworkMetric m : list) {
                sumIn += Optional.ofNullable(m.getInBytesPerSec()).orElse(0.0);
                sumOut += Optional.ofNullable(m.getOutBytesPerSec()).orElse(0.0);
                sumRx += Optional.ofNullable(m.getRxUsage()).orElse(0.0);
                sumTx += Optional.ofNullable(m.getTxUsage()).orElse(0.0);
                sumInErr += Optional.ofNullable(m.getInErrorPktsTot()).orElse(0L);
                sumOutErr += Optional.ofNullable(m.getOutErrorPktsTot()).orElse(0L);
                sumInDisc += Optional.ofNullable(m.getInDiscardPktsTot()).orElse(0L);
                sumOutDisc += Optional.ofNullable(m.getOutDiscardPktsTot()).orElse(0L);
            }

            int count = list.size();

            trafficTrend.add(new NetworkTrafficPointDto(timestamp, sumIn, sumOut));
            usageTrend.add(new NetworkUsagePointDto(timestamp,
                    count > 0 ? sumRx / count : 0,
                    count > 0 ? sumTx / count : 0));
            errorTrend.add(new NetworkErrorPointDto(timestamp,
                    sumInErr, sumOutErr, sumInDisc, sumOutDisc));
        });

        return NetworkSectionResponseDto.builder()
                .currentStats(currentStats)
                .trafficTrend(trafficTrend)
                .usageTrend(usageTrend)
                .errorTrend(errorTrend)
                .build();
    }

    /** 집계 데이터 기반 응답 생성 */
    private NetworkSectionResponseDto buildNetworkSectionFromAggregated(
            NetworkCurrentStatsDto currentStats, List<NetworkAggregatedStatsDto> aggregatedData) {

        List<NetworkTrafficPointDto> trafficTrend = aggregatedData.stream()
                .map(a -> new NetworkTrafficPointDto(a.getTimestamp(), a.getTotalInBps(), a.getTotalOutBps()))
                .collect(Collectors.toList());

        List<NetworkUsagePointDto> usageTrend = aggregatedData.stream()
                .map(a -> new NetworkUsagePointDto(a.getTimestamp(), a.getAvgRxUsage(), a.getAvgTxUsage()))
                .collect(Collectors.toList());

        return NetworkSectionResponseDto.builder()
                .currentStats(currentStats)
                .trafficTrend(trafficTrend)
                .usageTrend(usageTrend)
                .errorTrend(null)
                .build();
    }

    /** 최적 집계 레벨 자동 선택 */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime, LocalDateTime endTime) {
        return cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
    }

    /** 여러 장비의 네트워크 상태 일괄 조회 */
    public NetworkCurrentStatsBatchDto getCurrentNetworkStatsBatch(List<Long> equipmentIds) {

        log.info("📊 네트워크 상태 일괄 조회 - {}개 장비", equipmentIds.size());

        List<NetworkStatsWithEquipmentDto> result = new ArrayList<>();
        int success = 0, failure = 0;

        List<NetworkMetric> latestMetrics = networkMetricRepository.findLatestByEquipmentIds(equipmentIds);
        Map<Long, List<NetworkMetric>> metricsByEquipment =
                latestMetrics.stream().collect(Collectors.groupingBy(NetworkMetric::getEquipmentId));

        List<Object[]> statsRows = networkMetricRepository.getNetworkUsageStatsBatch(equipmentIds, 60);
        Map<Long, Object[]> statsMap = statsRows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> new Object[]{r[1], r[2], r[3]}
        ));

        for (Long equipmentId : equipmentIds) {
            try {
                List<NetworkMetric> latest = metricsByEquipment.get(equipmentId);

                if (latest == null || latest.isEmpty()) {
                    result.add(NetworkStatsWithEquipmentDto.builder()
                            .equipmentId(equipmentId).success(false)
                            .errorMessage("메트릭 데이터 없음").build());
                    failure++;
                    continue;
                }

                double sumIn = 0, sumOut = 0;
                long errIn = 0, errOut = 0;
                LocalDateTime lastUpdated = latest.get(0).getGenerateTime();

                for (NetworkMetric m : latest) {
                    sumIn += Optional.ofNullable(m.getInBytesPerSec()).orElse(0.0);
                    sumOut += Optional.ofNullable(m.getOutBytesPerSec()).orElse(0.0);
                    errIn += Optional.ofNullable(m.getInErrorPktsTot()).orElse(0L);
                    errOut += Optional.ofNullable(m.getOutErrorPktsTot()).orElse(0L);
                }

                Object[] stats = statsMap.get(equipmentId);
                Double avgRx = 0.0, maxRx = 0.0, minRx = 0.0;

                if (stats != null && stats[0] != null) {
                    avgRx = convertToDouble(stats[0]);
                    maxRx = convertToDouble(stats[1]);
                    minRx = convertToDouble(stats[2]);
                } else {
                    double tmp = latest.stream()
                            .mapToDouble(m -> Optional.ofNullable(m.getRxUsage()).orElse(0.0))
                            .average().orElse(0.0);
                    avgRx = maxRx = minRx = tmp;
                }

                NetworkCurrentStatsDto statsDto = NetworkCurrentStatsDto.builder()
                        .currentInBps(sumIn)
                        .currentOutBps(sumOut)
                        .avgRxUsage(avgRx)
                        .maxRxUsage(maxRx)
                        .minRxUsage(minRx)
                        .totalInErrors(errIn)
                        .totalOutErrors(errOut)
                        .lastUpdated(lastUpdated)
                        .build();

                result.add(NetworkStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(true)
                        .networkStats(statsDto)
                        .build());
                success++;

            } catch (Exception e) {
                log.error("❌ 네트워크 상태 조회 실패: {}", equipmentId, e);
                result.add(NetworkStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                failure++;
            }
        }

        return new NetworkCurrentStatsBatchDto(success, failure, result);
    }
}
