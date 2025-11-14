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

/**
 * 디스크 메트릭 서비스
 * 디스크 관련 대시보드 데이터 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiskMetricService {

    private final DiskMetricRepository diskMetricRepository;

    // CpuMetricService에서 가져온 헬퍼 서비스 (AggregationLevel 결정을 위함)
    private final CpuMetricService cpuMetricService;

    /**
     * 디스크 섹션 전체 데이터 조회
     */
    public DiskSectionResponseDto getDiskSectionData(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 디스크 섹션 데이터 조회 시작 - 장비 ID: {}, 기간: {} ~ {}, 집계: {}",
                equipmentId, startTime, endTime, aggregationLevel);

        // 1. 현재 상태 조회
        DiskCurrentStatsDto currentStats = getCurrentDiskStats(equipmentId, startTime, endTime);

        // 2. 집계 레벨에 따른 데이터 조회
        List<DiskMetric> metrics;
        List<DiskAggregatedStatsDto> aggregatedData;

        switch (aggregationLevel) {
            case MIN:
                aggregatedData = getDiskAggregatedData1Minute(equipmentId, startTime, endTime);
                return buildDiskSectionFromAggregated(currentStats, aggregatedData);
            case MIN5:
                aggregatedData = getDiskAggregatedData5Minutes(equipmentId, startTime, endTime);
                return buildDiskSectionFromAggregated(currentStats, aggregatedData);
            case HOUR:
                aggregatedData = getDiskAggregatedData1Hour(equipmentId, startTime, endTime);
                return buildDiskSectionFromAggregated(currentStats, aggregatedData);
            case DAY:
                aggregatedData = getDiskAggregatedData1Day(equipmentId, startTime, endTime);
                return buildDiskSectionFromAggregated(currentStats, aggregatedData);
            case RAW:
            default:
                metrics = diskMetricRepository.findByEquipmentIdAndTimeRange(
                        equipmentId, startTime, endTime);
                return buildDiskSectionFromRaw(currentStats, metrics);
        }
    }

    /**
     * 현재 디스크 상태 조회 (게이지용)
     */
    public DiskCurrentStatsDto getCurrentDiskStats(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // 최신 메트릭 조회
        DiskMetric latest = diskMetricRepository
                .findLatestByEquipmentId(equipmentId)
                .orElseThrow(() -> new RuntimeException("메트릭 데이터가 없습니다."));

        // 통계 조회
        Object[] stats = diskMetricRepository.getDiskUsageStats(equipmentId, startTime, endTime);

        Double avgUsage = 0.0;
        Double maxUsage = 0.0;
        Double minUsage = 0.0;

        if (stats != null && stats.length > 0) {
            Object firstElement = stats[0];

            if (firstElement instanceof Object[]) {
                Object[] innerArray = (Object[]) firstElement;
                if (innerArray.length >= 3) {
                    avgUsage = convertToDouble(innerArray[0]);
                    maxUsage = convertToDouble(innerArray[1]);
                    minUsage = convertToDouble(innerArray[2]);
                }
            } else if (stats.length >= 3) {
                avgUsage = convertToDouble(stats[0]);
                maxUsage = convertToDouble(stats[1]);
                minUsage = convertToDouble(stats[2]);
            }
        } else {
            log.warn("디스크 통계 쿼리 결과가 null이거나 비어있습니다.");
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
    private List<DiskAggregatedStatsDto> getDiskAggregatedData1Minute(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = diskMetricRepository.getDiskAggregatedStats1Minute(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToDiskAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 5분 단위 집계 데이터 조회
     */
    private List<DiskAggregatedStatsDto> getDiskAggregatedData5Minutes(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = diskMetricRepository.getDiskAggregatedStats5Minutes(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToDiskAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 1시간 단위 집계 데이터 조회
     */
    private List<DiskAggregatedStatsDto> getDiskAggregatedData1Hour(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = diskMetricRepository.getDiskAggregatedStats1Hour(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToDiskAggregatedStats)
                .collect(Collectors.toList());
    }
    /**
     * 1일 단위 집계 데이터 조회 (새로 추가)
     */
    private List<DiskAggregatedStatsDto> getDiskAggregatedData1Day(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = diskMetricRepository.getDiskAggregatedStats1Day(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToDiskAggregatedStats) // 기존 매퍼 재활용
                .collect(Collectors.toList());
    }

    /**
     * Object[] → DiskAggregatedStatsDto 매핑
     */
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

    /**
     * RAW 데이터로부터 디스크 섹션 응답 생성
     */
    private DiskSectionResponseDto buildDiskSectionFromRaw(
            DiskCurrentStatsDto currentStats,
            List<DiskMetric> metrics) {

        List<DiskUsagePointDto> diskUsageTrend = new ArrayList<>();
        List<DiskIoPointDto> diskIoTrend = new ArrayList<>();
        List<DiskInodeUsagePointDto> inodeUsageTrend = new ArrayList<>();

        for (DiskMetric metric : metrics) {
            // 4.1 디스크 사용률
            diskUsageTrend.add(DiskUsagePointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .usagePercent(metric.getUsedPercentage())
                    .build());

            // 4.2 디스크 I/O
            diskIoTrend.add(DiskIoPointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .readBps(metric.getIoReadBps())
                    .writeBps(metric.getIoWriteBps())
                    .build());

            // 4.6 Inode 사용률
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

    /**
     * 집계 데이터로부터 디스크 섹션 응답 생성
     */
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

    /**
     * (CpuMetricService의 로직 재활용)
     * 시간 범위에 따른 최적 집계 레벨 자동 선택
     */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        // 이미 구현된 CpuMetricService의 로직을 호출
        return cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
    }

    /**
     * 여러 장비의 현재 디스크 상태 일괄 조회
     */
    public DiskCurrentStatsBatchDto getCurrentDiskStatsBatch(List<Long> equipmentIds) {

        log.info("📊 일괄 디스크 상태 조회 시작 - 장비 개수: {}", equipmentIds.size());

        List<DiskStatsWithEquipmentDto> equipmentStatsList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // 1. 모든 장비의 최신 메트릭 일괄 조회
        List<DiskMetric> latestMetrics = diskMetricRepository
                .findLatestByEquipmentIds(equipmentIds);

        Map<Long, DiskMetric> latestMetricMap = latestMetrics.stream()
                .collect(Collectors.toMap(DiskMetric::getEquipmentId, metric -> metric));

        // 2. 모든 장비의 통계 일괄 조회 (최근 60개 데이터 기준)
        List<Object[]> statsResults = diskMetricRepository
                .getDiskUsageStatsBatch(equipmentIds, 60);

        Map<Long, Object[]> statsMap = statsResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),  // equipment_id
                        row -> new Object[]{row[1], row[2], row[3]}  // avg, max, min
                ));

        // 3. 각 장비별 데이터 조합
        for (Long equipmentId : equipmentIds) {
            try {
                DiskMetric latest = latestMetricMap.get(equipmentId);

                if (latest == null) {
                    equipmentStatsList.add(DiskStatsWithEquipmentDto.builder()
                            .equipmentId(equipmentId)
                            .success(false)
                            .errorMessage("메트릭 데이터가 없습니다.")
                            .build());
                    failureCount++;
                    continue;
                }

                Object[] stats = statsMap.get(equipmentId);
                Double currentUsage = latest.getUsedPercentage();
                Double avgUsage = currentUsage;
                Double maxUsage = currentUsage;
                Double minUsage = currentUsage;

                if (stats != null && stats[0] != null) {
                    avgUsage = convertToDouble(stats[0]);
                    maxUsage = convertToDouble(stats[1]);
                    minUsage = convertToDouble(stats[2]);
                } else {
                    log.warn("⚠️ 장비 {}의 디스크 통계 데이터 없음, 현재값으로 대체", equipmentId);
                }

                DiskCurrentStatsDto diskStats = DiskCurrentStatsDto.builder()
                        .currentUsagePercent(currentUsage)
                        .avgUsagePercent(avgUsage)
                        .maxUsagePercent(maxUsage)
                        .minUsagePercent(minUsage)
                        .currentInodeUsagePercent(latest.getUsedInodePercentage())
                        .currentIoTimePercent(latest.getIoTimePercentage())
                        .usedBytes(latest.getUsedBytes())
                        .totalBytes(latest.getTotalBytes())
                        .lastUpdated(latest.getGenerateTime())
                        .build();

                equipmentStatsList.add(DiskStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(true)
                        .diskStats(diskStats)
                        .build());

                successCount++;

            } catch (Exception e) {
                log.error("❌ 장비 {} 디스크 상태 조회 실패: {}", equipmentId, e.getMessage());
                equipmentStatsList.add(DiskStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        log.info("✅ 일괄 디스크 상태 조회 완료 - 성공: {}, 실패: {}", successCount, failureCount);

        return DiskCurrentStatsBatchDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .equipmentStats(equipmentStatsList)
                .build();
    }
}