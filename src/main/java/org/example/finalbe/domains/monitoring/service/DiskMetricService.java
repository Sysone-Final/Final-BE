/**
 * 작성자: 최산하
 * 디스크 메트릭 서비스를 제공하는 클래스
 */
package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.AggregationLevel;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.dto.*;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiskMetricService {

    private final DiskMetricRepository diskMetricRepository;
    private final CpuMetricService cpuMetricService;

    /** 디스크 섹션 전체 데이터 조회 */
    public DiskSectionResponseDto getDiskSectionData(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 디스크 섹션 조회 - equipmentId={}, 기간={}~{}, 집계={}",
                equipmentId, startTime, endTime, aggregationLevel);

        DiskCurrentStatsDto currentStats = getCurrentDiskStats(equipmentId, startTime, endTime);

        switch (aggregationLevel) {
            case MIN:
                return buildDiskSectionFromAggregated(
                        currentStats,
                        getDiskAggregatedData1Minute(equipmentId, startTime, endTime)
                );
            case MIN5:
                return buildDiskSectionFromAggregated(
                        currentStats,
                        getDiskAggregatedData5Minutes(equipmentId, startTime, endTime)
                );
            case HOUR:
                return buildDiskSectionFromAggregated(
                        currentStats,
                        getDiskAggregatedData1Hour(equipmentId, startTime, endTime)
                );
            case DAY:
                return buildDiskSectionFromAggregated(
                        currentStats,
                        getDiskAggregatedData1Day(equipmentId, startTime, endTime)
                );
            case RAW:
            default:
                return buildDiskSectionFromRaw(
                        currentStats,
                        diskMetricRepository.findByEquipmentIdAndTimeRange(equipmentId, startTime, endTime)
                );
        }
    }

    /** 현재 디스크 상태 조회 */
    public DiskCurrentStatsDto getCurrentDiskStats(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        DiskMetric latest = diskMetricRepository.findLatestByEquipmentId(equipmentId)
                .orElseThrow(() -> new RuntimeException("메트릭 데이터가 없습니다."));

        Object[] stats = diskMetricRepository.getDiskUsageStats(equipmentId, startTime, endTime);

        Double avgUsage = 0.0;
        Double maxUsage = 0.0;
        Double minUsage = 0.0;

        if (stats != null && stats.length > 0) {
            Object first = stats[0];
            if (first instanceof Object[]) {
                Object[] arr = (Object[]) first;
                if (arr.length >= 3) {
                    avgUsage = convertToDouble(arr[0]);
                    maxUsage = convertToDouble(arr[1]);
                    minUsage = convertToDouble(arr[2]);
                }
            } else if (stats.length >= 3) {
                avgUsage = convertToDouble(stats[0]);
                maxUsage = convertToDouble(stats[1]);
                minUsage = convertToDouble(stats[2]);
            }
        }

        return DiskCurrentStatsDto.builder()
                .currentUsagePercent(latest.getUsedPercentage())
                .avgUsagePercent(avgUsage)
                .maxUsagePercent(maxUsage)
                .minUsagePercent(minUsage)
                .currentInodeUsagePercent(latest.getUsedInodePercentage())
                .currentIoTimePercent(latest.getIoTimePercentage())
                .usedBytes(latest.getUsedBytes())
                .totalBytes(latest.getTotalBytes())
                .lastUpdated(latest.getGenerateTime())
                .build();
    }

    /** 숫자 변환 헬퍼 */
    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: {}", value);
            return 0.0;
        }
    }

    /** 1분 단위 집계 조회 */
    private List<DiskAggregatedStatsDto> getDiskAggregatedData1Minute(
            Long equipmentId, LocalDateTime start, LocalDateTime end) {

        return diskMetricRepository.getDiskAggregatedStats1Minute(equipmentId, start, end)
                .stream().map(this::mapToDiskAggregatedStats).collect(Collectors.toList());
    }

    /** 5분 단위 집계 조회 */
    private List<DiskAggregatedStatsDto> getDiskAggregatedData5Minutes(
            Long equipmentId, LocalDateTime start, LocalDateTime end) {

        return diskMetricRepository.getDiskAggregatedStats5Minutes(equipmentId, start, end)
                .stream().map(this::mapToDiskAggregatedStats).collect(Collectors.toList());
    }

    /** 1시간 단위 집계 조회 */
    private List<DiskAggregatedStatsDto> getDiskAggregatedData1Hour(
            Long equipmentId, LocalDateTime start, LocalDateTime end) {

        return diskMetricRepository.getDiskAggregatedStats1Hour(equipmentId, start, end)
                .stream().map(this::mapToDiskAggregatedStats).collect(Collectors.toList());
    }

    /** 1일 단위 집계 조회 */
    private List<DiskAggregatedStatsDto> getDiskAggregatedData1Day(
            Long equipmentId, LocalDateTime start, LocalDateTime end) {

        return diskMetricRepository.getDiskAggregatedStats1Day(equipmentId, start, end)
                .stream().map(this::mapToDiskAggregatedStats).collect(Collectors.toList());
    }

    /** Object[] → Dto 매핑 */
    private DiskAggregatedStatsDto mapToDiskAggregatedStats(Object[] row) {
        return DiskAggregatedStatsDto.builder()
                .timestamp(((Timestamp) row[0]).toLocalDateTime())
                .avgUsagePercent(convertToDouble(row[1]))
                .avgInodeUsagePercent(convertToDouble(row[2]))
                .avgReadBps(convertToDouble(row[3]))
                .avgWriteBps(convertToDouble(row[4]))
                .avgIoTimePercent(convertToDouble(row[5]))
                .sampleCount(row[6] != null ? ((Number) row[6]).intValue() : 0)
                .build();
    }

    /** RAW 데이터 기반 디스크 섹션 생성 */
    private DiskSectionResponseDto buildDiskSectionFromRaw(
            DiskCurrentStatsDto currentStats,
            List<DiskMetric> metrics) {

        List<DiskUsagePointDto> diskUsageTrend = new ArrayList<>();
        List<DiskIoPointDto> diskIoTrend = new ArrayList<>();
        List<DiskInodeUsagePointDto> inodeUsageTrend = new ArrayList<>();

        for (DiskMetric metric : metrics) {
            diskUsageTrend.add(DiskUsagePointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .usagePercent(metric.getUsedPercentage())
                    .build());

            diskIoTrend.add(DiskIoPointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .readBps(metric.getIoReadBps())
                    .writeBps(metric.getIoWriteBps())
                    .build());

            inodeUsageTrend.add(DiskInodeUsagePointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .inodeUsagePercent(metric.getUsedInodePercentage())
                    .build());
        }

        return DiskSectionResponseDto.builder()
                .currentStats(currentStats)
                .diskUsageTrend(diskUsageTrend)
                .diskIoTrend(diskIoTrend)
                .inodeUsageTrend(inodeUsageTrend)
                .build();
    }

    /** 집계 데이터 기반 디스크 섹션 생성 */
    private DiskSectionResponseDto buildDiskSectionFromAggregated(
            DiskCurrentStatsDto currentStats,
            List<DiskAggregatedStatsDto> aggregatedData) {

        List<DiskUsagePointDto> diskUsageTrend = aggregatedData.stream()
                .map(agg -> DiskUsagePointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .usagePercent(agg.getAvgUsagePercent())
                        .build())
                .collect(Collectors.toList());

        List<DiskIoPointDto> diskIoTrend = aggregatedData.stream()
                .map(agg -> DiskIoPointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .readBps(agg.getAvgReadBps())
                        .writeBps(agg.getAvgWriteBps())
                        .build())
                .collect(Collectors.toList());

        List<DiskInodeUsagePointDto> inodeUsageTrend = aggregatedData.stream()
                .map(agg -> DiskInodeUsagePointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .inodeUsagePercent(agg.getAvgInodeUsagePercent())
                        .build())
                .collect(Collectors.toList());

        return DiskSectionResponseDto.builder()
                .currentStats(currentStats)
                .diskUsageTrend(diskUsageTrend)
                .diskIoTrend(diskIoTrend)
                .inodeUsageTrend(inodeUsageTrend)
                .build();
    }

    /** 집계 레벨 자동 결정 */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
    }

    /** 여러 장비의 현재 디스크 상태 일괄 조회 */
    public DiskCurrentStatsBatchDto getCurrentDiskStatsBatch(List<Long> equipmentIds) {

        log.info("📊 일괄 디스크 상태 조회 시작 - {}개 장비", equipmentIds.size());

        List<DiskStatsWithEquipmentDto> equipmentStatsList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        List<DiskMetric> latestMetrics = diskMetricRepository.findLatestByEquipmentIds(equipmentIds);

        Map<Long, DiskMetric> latestMetricMap = latestMetrics.stream()
                .collect(Collectors.toMap(DiskMetric::getEquipmentId, m -> m));

        List<Object[]> statsResults = diskMetricRepository
                .getDiskUsageStatsBatch(equipmentIds, 60);

        Map<Long, Object[]> statsMap = statsResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> new Object[]{row[1], row[2], row[3]}
                ));

        for (Long eqId : equipmentIds) {
            try {
                DiskMetric latest = latestMetricMap.get(eqId);

                if (latest == null) {
                    failureCount++;
                    equipmentStatsList.add(DiskStatsWithEquipmentDto.builder()
                            .equipmentId(eqId)
                            .success(false)
                            .errorMessage("메트릭 데이터 없음")
                            .build());
                    continue;
                }

                Object[] stats = statsMap.get(eqId);
                Double current = latest.getUsedPercentage();
                Double avg = current, max = current, min = current;

                if (stats != null && stats[0] != null) {
                    avg = convertToDouble(stats[0]);
                    max = convertToDouble(stats[1]);
                    min = convertToDouble(stats[2]);
                }

                DiskCurrentStatsDto diskStats = DiskCurrentStatsDto.builder()
                        .currentUsagePercent(current)
                        .avgUsagePercent(avg)
                        .maxUsagePercent(max)
                        .minUsagePercent(min)
                        .currentInodeUsagePercent(latest.getUsedInodePercentage())
                        .currentIoTimePercent(latest.getIoTimePercentage())
                        .usedBytes(latest.getUsedBytes())
                        .totalBytes(latest.getTotalBytes())
                        .lastUpdated(latest.getGenerateTime())
                        .build();

                equipmentStatsList.add(DiskStatsWithEquipmentDto.builder()
                        .equipmentId(eqId)
                        .success(true)
                        .diskStats(diskStats)
                        .build());

                successCount++;

            } catch (Exception e) {
                failureCount++;
                equipmentStatsList.add(DiskStatsWithEquipmentDto.builder()
                        .equipmentId(eqId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return DiskCurrentStatsBatchDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .equipmentStats(equipmentStatsList)
                .build();
    }
}
