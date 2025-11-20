package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.AggregationLevel;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.monitoring.dto.*;
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 메모리 메트릭 서비스
 * 메모리 관련 대시보드 데이터 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoryMetricService {

    private final SystemMetricRepository systemMetricRepository;

    /**
     * 메모리 섹션 전체 데이터 조회
     */
    public MemorySectionResponseDto getMemorySectionData(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 메모리 섹션 데이터 조회 시작 - 장비 ID: {}, 기간: {} ~ {}, 집계: {}",
                equipmentId, startTime, endTime, aggregationLevel);

        // 1. 현재 상태 조회
        MemoryCurrentStatsDto currentStats = getCurrentMemoryStats(equipmentId, startTime, endTime);

        // 2. 집계 레벨에 따른 데이터 조회
        List<SystemMetric> metrics;
        List<MemoryAggregatedStatsDto> aggregatedData;

        switch (aggregationLevel) {
            case MIN:
                aggregatedData = getMemoryAggregatedData1Minute(equipmentId, startTime, endTime);
                return buildMemorySectionFromAggregated(currentStats, aggregatedData);
            case MIN5:
                aggregatedData = getMemoryAggregatedData5Minutes(equipmentId, startTime, endTime);
                return buildMemorySectionFromAggregated(currentStats, aggregatedData);
            case HOUR:
                aggregatedData = getMemoryAggregatedData1Hour(equipmentId, startTime, endTime);
                return buildMemorySectionFromAggregated(currentStats, aggregatedData);
            case DAY:
                aggregatedData = getMemoryAggregatedData1Day(equipmentId, startTime, endTime);
                return buildMemorySectionFromAggregated(currentStats, aggregatedData);
            case RAW:
            default:
                metrics = systemMetricRepository.findByEquipmentIdAndTimeRange(
                        equipmentId, startTime, endTime);
                return buildMemorySectionFromRaw(currentStats, metrics);
        }
    }

    /**
     * 현재 메모리/스왑 상태 조회 (게이지용)
     */
    public MemoryCurrentStatsDto getCurrentMemoryStats(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // 최신 메트릭 조회
        SystemMetric latest = systemMetricRepository
                .findLatestByEquipmentId(equipmentId)
                .orElseThrow(() -> new RuntimeException("메트릭 데이터가 없습니다."));

        // 통계 조회
        Object[] stats = systemMetricRepository.getMemoryUsageStats(equipmentId, startTime, endTime);

        Double avgMem = 0.0;
        Double maxMem = 0.0;
        Double minMem = 0.0;

        if (stats != null && stats.length > 0) {
            Object firstElement = stats[0];

            if (firstElement instanceof Object[]) {
                Object[] innerArray = (Object[]) firstElement;
                if (innerArray.length >= 3) {
                    avgMem = convertToDouble(innerArray[0]);
                    maxMem = convertToDouble(innerArray[1]);
                    minMem = convertToDouble(innerArray[2]);
                }
            } else if (stats.length >= 3) {
                avgMem = convertToDouble(stats[0]);
                maxMem = convertToDouble(stats[1]);
                minMem = convertToDouble(stats[2]);
            }
        } else {
            log.warn("메모리 통계 쿼리 결과가 null이거나 비어있습니다.");
        }

        return MemoryCurrentStatsDto.builder()
                .currentMemoryUsage(latest.getUsedMemoryPercentage())
                .avgMemoryUsage(avgMem)
                .maxMemoryUsage(maxMem)
                .minMemoryUsage(minMem)
                .currentSwapUsage(latest.getUsedSwapPercentage())
                .usedMemoryBytes(latest.getUsedMemory())
                .totalMemoryBytes(latest.getTotalMemory())
                .lastUpdated(latest.getGenerateTime())
                .build();
    }

    /**
     * Object를 Double로 안전하게 변환
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: {}", value);
            return 0.0;
        }
    }

    /**
     * 1분 단위 집계 데이터 조회
     */
    private List<MemoryAggregatedStatsDto> getMemoryAggregatedData1Minute(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = systemMetricRepository.getMemoryAggregatedStats1Minute(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToMemoryAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 5분 단위 집계 데이터 조회
     */
    private List<MemoryAggregatedStatsDto> getMemoryAggregatedData5Minutes(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = systemMetricRepository.getMemoryAggregatedStats5Minutes(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToMemoryAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 1시간 단위 집계 데이터 조회
     */
    private List<MemoryAggregatedStatsDto> getMemoryAggregatedData1Hour(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = systemMetricRepository.getMemoryAggregatedStats1Hour(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToMemoryAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 1일 단위 집계 데이터 조회
     */
    private List<MemoryAggregatedStatsDto> getMemoryAggregatedData1Day(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = systemMetricRepository.getMemoryAggregatedStats1Day(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToMemoryAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * Object[] → MemoryAggregatedStatsDto 매핑
     */
    private MemoryAggregatedStatsDto mapToMemoryAggregatedStats(Object[] row) {
        return MemoryAggregatedStatsDto.builder()
                .timestamp(((Timestamp) row[0]).toLocalDateTime())
                .avgMemoryUsage(convertToDouble(row[1]))
                .maxMemoryUsage(convertToDouble(row[2]))
                .minMemoryUsage(convertToDouble(row[3]))
                .avgSwapUsage(convertToDouble(row[4]))
                .sampleCount(row[5] != null ? ((Number) row[5]).intValue() : 0)
                .build();
    }

    /**
     * RAW 데이터로부터 메모리 섹션 응답 생성
     */
    private MemorySectionResponseDto buildMemorySectionFromRaw(
            MemoryCurrentStatsDto currentStats,
            List<SystemMetric> metrics) {

        List<MemoryUsagePointDto> memoryUsageTrend = new ArrayList<>();
        List<MemoryCompositionPointDto> memoryCompositionTrend = new ArrayList<>();
        List<SwapUsagePointDto> swapUsageTrend = new ArrayList<>();

        for (SystemMetric metric : metrics) {
            // 2.1 메모리 사용률 추이
            memoryUsageTrend.add(MemoryUsagePointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .memoryUsagePercent(metric.getUsedMemoryPercentage())
                    .build());

            // 2.2 메모리 구성 요소
            memoryCompositionTrend.add(MemoryCompositionPointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .active(metric.getMemoryActive())
                    .inactive(metric.getMemoryInactive())
                    .buffers(metric.getMemoryBuffers())
                    .cached(metric.getMemoryCached())
                    .free(metric.getFreeMemory())
                    .build());

            // 2.3 스왑 사용률
            swapUsageTrend.add(SwapUsagePointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .swapUsagePercent(metric.getUsedSwapPercentage())
                    .build());
        }

        return MemorySectionResponseDto.builder()
                .currentStats(currentStats)
                .memoryUsageTrend(memoryUsageTrend)
                .memoryCompositionTrend(memoryCompositionTrend)
                .swapUsageTrend(swapUsageTrend)
                .build();
    }

    /**
     * 집계 데이터로부터 메모리 섹션 응답 생성
     */
    private MemorySectionResponseDto buildMemorySectionFromAggregated(
            MemoryCurrentStatsDto currentStats,
            List<MemoryAggregatedStatsDto> aggregatedData) {

        List<MemoryUsagePointDto> memoryUsageTrend = aggregatedData.stream()
                .map(agg -> MemoryUsagePointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .memoryUsagePercent(agg.getAvgMemoryUsage())
                        .build())
                .collect(Collectors.toList());

        List<SwapUsagePointDto> swapUsageTrend = aggregatedData.stream()
                .map(agg -> SwapUsagePointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .swapUsagePercent(agg.getAvgSwapUsage())
                        .build())
                .collect(Collectors.toList());

        return MemorySectionResponseDto.builder()
                .currentStats(currentStats)
                .memoryUsageTrend(memoryUsageTrend)
                .memoryCompositionTrend(null) // 집계 데이터에서는 제공 불가
                .swapUsageTrend(swapUsageTrend)
                .build();
    }

    /**
     * 시간 범위에 따른 최적 집계 레벨 자동 선택
     */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        long hours = java.time.Duration.between(startTime, endTime).toHours();
        long days = java.time.Duration.between(startTime, endTime).toDays();

        if (days < 1) { // 24시간 이내 조회
            if (hours <= 1) {
                return AggregationLevel.RAW;  // 1시간 이내
            } else if (hours <= 6) {
                return AggregationLevel.MIN;  // 6시간 이내
            } else {
                return AggregationLevel.MIN5; // 24시간 이내
            }
        } else if (days <= 30) { // 30일 이내 조회
            return AggregationLevel.HOUR; // 1시간 단위
        } else { // 30일 초과 조회
            return AggregationLevel.DAY; // 1일 단위
        }
    }

    /**
     * 여러 장비의 현재 메모리 상태 일괄 조회
     */
    public MemoryCurrentStatsBatchDto getCurrentMemoryStatsBatch(List<Long> equipmentIds) {

        log.info("📊 일괄 메모리 상태 조회 시작 - 장비 개수: {}", equipmentIds.size());

        List<MemoryStatsWithEquipmentDto> equipmentStatsList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // 1. 모든 장비의 최신 메트릭 일괄 조회
        List<SystemMetric> latestMetrics = systemMetricRepository
                .findLatestByEquipmentIds(equipmentIds);

        Map<Long, SystemMetric> latestMetricMap = latestMetrics.stream()
                .collect(Collectors.toMap(SystemMetric::getEquipmentId, metric -> metric));

        // 2. 모든 장비의 통계 일괄 조회 (최근 60개 데이터 기준)
        List<Object[]> statsResults = systemMetricRepository
                .getMemoryUsageStatsBatch(equipmentIds, 60);

        Map<Long, Object[]> statsMap = statsResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),  // equipment_id
                        row -> new Object[]{row[1], row[2], row[3]}  // avg, max, min
                ));

        // 3. 각 장비별 데이터 조합
        for (Long equipmentId : equipmentIds) {
            try {
                SystemMetric latest = latestMetricMap.get(equipmentId);

                if (latest == null) {
                    equipmentStatsList.add(MemoryStatsWithEquipmentDto.builder()
                            .equipmentId(equipmentId)
                            .success(false)
                            .errorMessage("메트릭 데이터가 없습니다.")
                            .build());
                    failureCount++;
                    continue;
                }

                Object[] stats = statsMap.get(equipmentId);
                Double currentMemory = latest.getUsedMemoryPercentage();
                Double avgMemory = currentMemory;
                Double maxMemory = currentMemory;
                Double minMemory = currentMemory;

                if (stats != null && stats[0] != null) {
                    avgMemory = convertToDouble(stats[0]);
                    maxMemory = convertToDouble(stats[1]);
                    minMemory = convertToDouble(stats[2]);
                } else {
                    log.warn("⚠️ 장비 {}의 통계 데이터 없음, 현재값으로 대체", equipmentId);
                }

                MemoryCurrentStatsDto memoryStats = MemoryCurrentStatsDto.builder()
                        .currentMemoryUsage(currentMemory)
                        .avgMemoryUsage(avgMemory)
                        .maxMemoryUsage(maxMemory)
                        .minMemoryUsage(minMemory)
                        .currentSwapUsage(latest.getUsedSwapPercentage())
                        .usedMemoryBytes(latest.getUsedMemory())
                        .totalMemoryBytes(latest.getTotalMemory())
                        .lastUpdated(latest.getGenerateTime())
                        .build();

                equipmentStatsList.add(MemoryStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(true)
                        .memoryStats(memoryStats)
                        .build());

                successCount++;

            } catch (Exception e) {
                log.error("❌ 장비 {} 메모리 상태 조회 실패: {}", equipmentId, e.getMessage());
                equipmentStatsList.add(MemoryStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        log.info("✅ 일괄 메모리 상태 조회 완료 - 성공: {}, 실패: {}", successCount, failureCount);

        return MemoryCurrentStatsBatchDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .equipmentStats(equipmentStatsList)
                .build();
    }
}