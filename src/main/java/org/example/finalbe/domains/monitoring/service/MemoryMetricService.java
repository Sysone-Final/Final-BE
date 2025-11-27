/**
 * 작성자: 최산하
 * 메모리/스왑 메트릭 처리를 담당하는 서비스
 */
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoryMetricService {

    private final SystemMetricRepository systemMetricRepository;

    /** 메모리 섹션 전체 데이터 조회 */
    public MemorySectionResponseDto getMemorySectionData(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 메모리 섹션 조회 - equipmentId={}, 기간={}~{}, 집계={}",
                equipmentId, startTime, endTime, aggregationLevel);

        MemoryCurrentStatsDto currentStats = getCurrentMemoryStats(equipmentId, startTime, endTime);

        switch (aggregationLevel) {
            case MIN:
                return buildMemorySectionFromAggregated(
                        currentStats,
                        getMemoryAggregatedData1Minute(equipmentId, startTime, endTime)
                );
            case MIN5:
                return buildMemorySectionFromAggregated(
                        currentStats,
                        getMemoryAggregatedData5Minutes(equipmentId, startTime, endTime)
                );
            case HOUR:
                return buildMemorySectionFromAggregated(
                        currentStats,
                        getMemoryAggregatedData1Hour(equipmentId, startTime, endTime)
                );
            case DAY:
                return buildMemorySectionFromAggregated(
                        currentStats,
                        getMemoryAggregatedData1Day(equipmentId, startTime, endTime)
                );
            case RAW:
            default:
                return buildMemorySectionFromRaw(
                        currentStats,
                        systemMetricRepository.findByEquipmentIdAndTimeRange(
                                equipmentId, startTime, endTime)
                );
        }
    }

    /** 현재 메모리/스왑 상태 조회 */
    public MemoryCurrentStatsDto getCurrentMemoryStats(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        SystemMetric latest = systemMetricRepository
                .findLatestByEquipmentId(equipmentId)
                .orElseThrow(() -> new RuntimeException("메트릭 데이터가 없습니다."));

        Object[] stats = systemMetricRepository.getMemoryUsageStats(equipmentId, startTime, endTime);

        Double avgMem = 0.0, maxMem = 0.0, minMem = 0.0;

        if (stats != null && stats.length > 0) {
            Object first = stats[0];

            if (first instanceof Object[]) {
                Object[] arr = (Object[]) first;
                if (arr.length >= 3) {
                    avgMem = convertToDouble(arr[0]);
                    maxMem = convertToDouble(arr[1]);
                    minMem = convertToDouble(arr[2]);
                }
            } else if (stats.length >= 3) {
                avgMem = convertToDouble(stats[0]);
                maxMem = convertToDouble(stats[1]);
                minMem = convertToDouble(stats[2]);
            }
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

    /** Object → Double 변환 */
    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (Exception e) { return 0.0; }
    }

    /** 1분 단위 집계 조회 */
    private List<MemoryAggregatedStatsDto> getMemoryAggregatedData1Minute(
            Long equipmentId, LocalDateTime start, LocalDateTime end) {

        return systemMetricRepository.getMemoryAggregatedStats1Minute(equipmentId, start, end)
                .stream().map(this::mapToMemoryAggregatedStats).collect(Collectors.toList());
    }

    /** 5분 단위 집계 조회 */
    private List<MemoryAggregatedStatsDto> getMemoryAggregatedData5Minutes(
            Long equipmentId, LocalDateTime start, LocalDateTime end) {

        return systemMetricRepository.getMemoryAggregatedStats5Minutes(equipmentId, start, end)
                .stream().map(this::mapToMemoryAggregatedStats).collect(Collectors.toList());
    }

    /** 1시간 단위 집계 조회 */
    private List<MemoryAggregatedStatsDto> getMemoryAggregatedData1Hour(
            Long equipmentId, LocalDateTime start, LocalDateTime end) {

        return systemMetricRepository.getMemoryAggregatedStats1Hour(equipmentId, start, end)
                .stream().map(this::mapToMemoryAggregatedStats).collect(Collectors.toList());
    }

    /** 1일 단위 집계 조회 */
    private List<MemoryAggregatedStatsDto> getMemoryAggregatedData1Day(
            Long equipmentId, LocalDateTime start, LocalDateTime end) {

        return systemMetricRepository.getMemoryAggregatedStats1Day(equipmentId, start, end)
                .stream().map(this::mapToMemoryAggregatedStats).collect(Collectors.toList());
    }

    /** Object[] → DTO 매핑 */
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

    /** RAW 데이터 기반 메모리 섹션 생성 */
    private MemorySectionResponseDto buildMemorySectionFromRaw(
            MemoryCurrentStatsDto currentStats,
            List<SystemMetric> metrics) {

        List<MemoryUsagePointDto> memoryUsageTrend = new ArrayList<>();
        List<MemoryCompositionPointDto> memoryCompositionTrend = new ArrayList<>();
        List<SwapUsagePointDto> swapUsageTrend = new ArrayList<>();

        for (SystemMetric m : metrics) {
            memoryUsageTrend.add(MemoryUsagePointDto.builder()
                    .timestamp(m.getGenerateTime())
                    .memoryUsagePercent(m.getUsedMemoryPercentage())
                    .build());

            memoryCompositionTrend.add(MemoryCompositionPointDto.builder()
                    .timestamp(m.getGenerateTime())
                    .active(m.getMemoryActive())
                    .inactive(m.getMemoryInactive())
                    .buffers(m.getMemoryBuffers())
                    .cached(m.getMemoryCached())
                    .free(m.getFreeMemory())
                    .build());

            swapUsageTrend.add(SwapUsagePointDto.builder()
                    .timestamp(m.getGenerateTime())
                    .swapUsagePercent(m.getUsedSwapPercentage())
                    .build());
        }

        return MemorySectionResponseDto.builder()
                .currentStats(currentStats)
                .memoryUsageTrend(memoryUsageTrend)
                .memoryCompositionTrend(memoryCompositionTrend)
                .swapUsageTrend(swapUsageTrend)
                .build();
    }

    /** 집계 데이터 기반 메모리 섹션 생성 */
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
                .memoryCompositionTrend(null)
                .swapUsageTrend(swapUsageTrend)
                .build();
    }

    /** 시간 범위에 따른 최적 집계 레벨 자동 결정 */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime, LocalDateTime endTime) {

        long hours = java.time.Duration.between(startTime, endTime).toHours();
        long days = java.time.Duration.between(startTime, endTime).toDays();

        if (days < 1) {
            if (hours <= 1) return AggregationLevel.RAW;
            else if (hours <= 6) return AggregationLevel.MIN;
            else return AggregationLevel.MIN5;
        } else if (days <= 30) {
            return AggregationLevel.HOUR;
        } else {
            return AggregationLevel.DAY;
        }
    }

    /** 여러 장비의 메모리 상태 일괄 조회 */
    public MemoryCurrentStatsBatchDto getCurrentMemoryStatsBatch(List<Long> equipmentIds) {

        log.info("📊 일괄 메모리 조회 시작 - {}개 장비", equipmentIds.size());

        List<MemoryStatsWithEquipmentDto> equipmentStatsList = new ArrayList<>();
        int successCount = 0, failureCount = 0;

        List<SystemMetric> latestMetrics = systemMetricRepository.findLatestByEquipmentIds(equipmentIds);

        Map<Long, SystemMetric> latestMetricMap = latestMetrics.stream()
                .collect(Collectors.toMap(SystemMetric::getEquipmentId, m -> m));

        List<Object[]> statsResults =
                systemMetricRepository.getMemoryUsageStatsBatch(equipmentIds, 60);

        Map<Long, Object[]> statsMap = statsResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> new Object[]{row[1], row[2], row[3]}
                ));

        for (Long equipmentId : equipmentIds) {
            try {
                SystemMetric latest = latestMetricMap.get(equipmentId);

                if (latest == null) {
                    failureCount++;
                    equipmentStatsList.add(MemoryStatsWithEquipmentDto.builder()
                            .equipmentId(equipmentId)
                            .success(false)
                            .errorMessage("메트릭 데이터 없음")
                            .build());
                    continue;
                }

                Object[] stats = statsMap.get(equipmentId);

                Double cur = latest.getUsedMemoryPercentage();
                Double avg = cur, max = cur, min = cur;

                if (stats != null && stats[0] != null) {
                    avg = convertToDouble(stats[0]);
                    max = convertToDouble(stats[1]);
                    min = convertToDouble(stats[2]);
                }

                MemoryCurrentStatsDto memoryStats = MemoryCurrentStatsDto.builder()
                        .currentMemoryUsage(cur)
                        .avgMemoryUsage(avg)
                        .maxMemoryUsage(max)
                        .minMemoryUsage(min)
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
                failureCount++;
                equipmentStatsList.add(MemoryStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return MemoryCurrentStatsBatchDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .equipmentStats(equipmentStatsList)
                .build();
    }
}
