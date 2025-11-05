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
 * CPU 메트릭 서비스
 * CPU 관련 대시보드 데이터 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CpuMetricService {

    private final SystemMetricRepository systemMetricRepository;

    /**
     * CPU 섹션 전체 데이터 조회
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param aggregationLevel 집계 레벨 (RAW, MIN, MIN5, HOUR)
     * @return CPU 섹션 응답 데이터
     */
    public CpuSectionResponseDto getCpuSectionData(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 CPU 섹션 데이터 조회 시작 - 장비 ID: {}, 기간: {} ~ {}, 집계: {}",
                equipmentId, startTime, endTime, aggregationLevel);

        // 1. 현재 상태 조회
        CpuCurrentStatsDto currentStats = getCurrentCpuStats(equipmentId, startTime, endTime);

        // 2. 집계 레벨에 따른 데이터 조회
        List<SystemMetric> metrics;
        List<CpuAggregatedStatsDto> aggregatedData;

        switch (aggregationLevel) {
            case MIN:
                aggregatedData = getCpuAggregatedData1Minute(equipmentId, startTime, endTime);
                return buildCpuSectionFromAggregated(currentStats, aggregatedData);
            case MIN5:
                aggregatedData = getCpuAggregatedData5Minutes(equipmentId, startTime, endTime);
                return buildCpuSectionFromAggregated(currentStats, aggregatedData);
            case HOUR:
                aggregatedData = getCpuAggregatedData1Hour(equipmentId, startTime, endTime);
                return buildCpuSectionFromAggregated(currentStats, aggregatedData);
            case RAW:
            default:
                metrics = systemMetricRepository.findByEquipmentIdAndTimeRange(
                        equipmentId, startTime, endTime);
                return buildCpuSectionFromRaw(currentStats, metrics);
        }
    }

    /**
     * 현재 CPU 상태 조회 (게이지용)
     */
    public CpuCurrentStatsDto getCurrentCpuStats(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // 최신 메트릭 조회
        SystemMetric latest = systemMetricRepository
                .findLatestByEquipmentId(equipmentId)
                .orElseThrow(() -> new RuntimeException("메트릭 데이터가 없습니다."));

        // 통계 조회
        Object[] stats = systemMetricRepository.getCpuUsageStats(equipmentId, startTime, endTime);

        Double avgCpu = 0.0;
        Double maxCpu = 0.0;
        Double minCpu = 0.0;

        if (stats != null && stats.length >= 3) {
            avgCpu = convertToDouble(stats[0]);
            maxCpu = convertToDouble(stats[1]);
            minCpu = convertToDouble(stats[2]);
        } else {
            log.warn("CPU 통계 쿼리 결과가 비정상입니다.");
        }

        return CpuCurrentStatsDto.builder()
                .currentCpuUsage(100 - (latest.getCpuIdle() != null ? latest.getCpuIdle() : 0.0))
                .avgCpuUsage(avgCpu)
                .maxCpuUsage(maxCpu)
                .minCpuUsage(minCpu)
                .currentLoadAvg1(latest.getLoadAvg1())
                .currentLoadAvg5(latest.getLoadAvg5())
                .currentLoadAvg15(latest.getLoadAvg15())
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
    private List<CpuAggregatedStatsDto> getCpuAggregatedData1Minute(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = systemMetricRepository.getCpuAggregatedStats1Minute(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToCpuAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 5분 단위 집계 데이터 조회
     */
    private List<CpuAggregatedStatsDto> getCpuAggregatedData5Minutes(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = systemMetricRepository.getCpuAggregatedStats5Minutes(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToCpuAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 1시간 단위 집계 데이터 조회
     */
    private List<CpuAggregatedStatsDto> getCpuAggregatedData1Hour(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        List<Object[]> results = systemMetricRepository.getCpuAggregatedStats1Hour(
                equipmentId, startTime, endTime);

        return results.stream()
                .map(this::mapToCpuAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * Object[] → CpuAggregatedStatsDto 매핑
     */
    private CpuAggregatedStatsDto mapToCpuAggregatedStats(Object[] row) {
        return CpuAggregatedStatsDto.builder()
                .timestamp(((Timestamp) row[0]).toLocalDateTime())
                .avgCpuUsage(convertToDouble(row[1]))
                .maxCpuUsage(convertToDouble(row[2]))
                .minCpuUsage(convertToDouble(row[3]))
                .avgLoadAvg1(convertToDouble(row[4]))
                .totalContextSwitches(row[5] != null ? ((Number) row[5]).longValue() : 0L)
                .sampleCount(row[6] != null ? ((Number) row[6]).intValue() : 0)
                .build();
    }

    /**
     * RAW 데이터로부터 CPU 섹션 응답 생성
     */
    private CpuSectionResponseDto buildCpuSectionFromRaw(
            CpuCurrentStatsDto currentStats,
            List<SystemMetric> metrics) {

        List<CpuUsagePointDto> cpuUsageTrend = new ArrayList<>();
        List<CpuModeDistributionDto> cpuModeDistribution = new ArrayList<>();
        List<LoadAveragePointDto> loadAverageTrend = new ArrayList<>();
        List<ContextSwitchPointDto> contextSwitchTrend = new ArrayList<>();

        SystemMetric prevMetric = null;

        for (SystemMetric metric : metrics) {
            // 1.1 CPU 사용률 추이
            cpuUsageTrend.add(CpuUsagePointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .cpuUsagePercent(100 - (metric.getCpuIdle() != null ? metric.getCpuIdle() : 0.0))
                    .build());

            // 1.2 CPU 모드별 분포
            cpuModeDistribution.add(CpuModeDistributionDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .userPercent(metric.getCpuUser())
                    .systemPercent(metric.getCpuSystem())
                    .iowaitPercent(metric.getCpuWait())
                    .irqPercent(metric.getCpuIrq())
                    .softirqPercent(metric.getCpuSoftirq())
                    .nicePercent(metric.getCpuNice())
                    .stealPercent(metric.getCpuSteal())
                    .idlePercent(metric.getCpuIdle())
                    .build());

            // 1.3 시스템 부하
            loadAverageTrend.add(LoadAveragePointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .loadAvg1(metric.getLoadAvg1())
                    .loadAvg5(metric.getLoadAvg5())
                    .loadAvg15(metric.getLoadAvg15())
                    .build());

            // 1.4 컨텍스트 스위치 (초당 계산)
            if (prevMetric != null && metric.getContextSwitches() != null && prevMetric.getContextSwitches() != null) {
                long contextSwitchDiff = metric.getContextSwitches() - prevMetric.getContextSwitches();
                long timeDiffSeconds = java.time.Duration.between(
                        prevMetric.getGenerateTime(),
                        metric.getGenerateTime()
                ).getSeconds();

                long contextSwitchPerSec = timeDiffSeconds > 0 ?
                        contextSwitchDiff / timeDiffSeconds : 0;

                contextSwitchTrend.add(ContextSwitchPointDto.builder()
                        .timestamp(metric.getGenerateTime())
                        .contextSwitchesPerSec(contextSwitchPerSec)
                        .build());
            }

            prevMetric = metric;
        }

        return CpuSectionResponseDto.builder()
                .currentStats(currentStats)
                .cpuUsageTrend(cpuUsageTrend)
                .cpuModeDistribution(cpuModeDistribution)
                .loadAverageTrend(loadAverageTrend)
                .contextSwitchTrend(contextSwitchTrend)
                .build();
    }

    /**
     * 집계 데이터로부터 CPU 섹션 응답 생성
     * (집계 데이터에서는 CPU 모드별 분포와 컨텍스트 스위치는 제공 불가)
     */
    private CpuSectionResponseDto buildCpuSectionFromAggregated(
            CpuCurrentStatsDto currentStats,
            List<CpuAggregatedStatsDto> aggregatedData) {

        List<CpuUsagePointDto> cpuUsageTrend = aggregatedData.stream()
                .map(agg -> CpuUsagePointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .cpuUsagePercent(agg.getAvgCpuUsage())
                        .build())
                .collect(Collectors.toList());

        List<LoadAveragePointDto> loadAverageTrend = aggregatedData.stream()
                .map(agg -> LoadAveragePointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .loadAvg1(agg.getAvgLoadAvg1())
                        .loadAvg5(null)  // 집계 데이터에는 1분 평균만 포함
                        .loadAvg15(null)
                        .build())
                .collect(Collectors.toList());

        List<ContextSwitchPointDto> contextSwitchTrend = aggregatedData.stream()
                .filter(agg -> agg.getSampleCount() != null && agg.getSampleCount() > 0)
                .map(agg -> ContextSwitchPointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .contextSwitchesPerSec(agg.getTotalContextSwitches() / agg.getSampleCount())
                        .build())
                .collect(Collectors.toList());

        return CpuSectionResponseDto.builder()
                .currentStats(currentStats)
                .cpuUsageTrend(cpuUsageTrend)
                .cpuModeDistribution(null)  // 집계 데이터에서는 제공 불가
                .loadAverageTrend(loadAverageTrend)
                .contextSwitchTrend(contextSwitchTrend)
                .build();
    }

    /**
     * 시간 범위에 따른 최적 집계 레벨 자동 선택
     */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        long hours = java.time.Duration.between(startTime, endTime).toHours();

        if (hours <= 1) {
            return AggregationLevel.RAW;
        } else if (hours <= 6) {
            return AggregationLevel.MIN;
        } else if (hours <= 24) {
            return AggregationLevel.MIN5;
        } else {
            return AggregationLevel.HOUR;
        }
    }

    /**
     * 여러 장비의 현재 CPU 상태 일괄 조회
     *
     * @param equipmentIds 장비 ID 리스트
     * @return 각 장비별 CPU 상태
     */
    public CpuCurrentStatsBatchDto getCurrentCpuStatsBatch(List<Long> equipmentIds) {

        log.info("📊 일괄 CPU 상태 조회 시작 - 장비 개수: {}", equipmentIds.size());

        List<CpuStatsWithEquipmentDto> equipmentStatsList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // 1. 모든 장비의 최신 메트릭 일괄 조회
        List<SystemMetric> latestMetrics = systemMetricRepository
                .findLatestByEquipmentIds(equipmentIds);

        // 2. 장비 ID별로 최신 메트릭 매핑
        Map<Long, SystemMetric> latestMetricMap = latestMetrics.stream()
                .collect(Collectors.toMap(
                        SystemMetric::getEquipmentId,
                        metric -> metric
                ));

        // 3. 모든 장비의 통계 일괄 조회 (최근 60개 데이터 기준)
        List<Object[]> statsResults = systemMetricRepository
                .getCpuUsageStatsBatch(equipmentIds, 60);

        // 4. 장비 ID별로 통계 매핑
        Map<Long, Object[]> statsMap = statsResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),  // equipment_id
                        row -> new Object[]{row[1], row[2], row[3]}  // avg, max, min
                ));

        // 5. 각 장비별 데이터 조합
        for (Long equipmentId : equipmentIds) {
            try {
                SystemMetric latest = latestMetricMap.get(equipmentId);

                if (latest == null) {
                    // 데이터가 없는 장비
                    equipmentStatsList.add(CpuStatsWithEquipmentDto.builder()
                            .equipmentId(equipmentId)
                            .success(false)
                            .errorMessage("메트릭 데이터가 없습니다.")
                            .build());
                    failureCount++;
                    continue;
                }

                Object[] stats = statsMap.get(equipmentId);
                Double currentCpu = 100 - (latest.getCpuIdle() != null ? latest.getCpuIdle() : 0.0);
                Double avgCpu = currentCpu;
                Double maxCpu = currentCpu;
                Double minCpu = currentCpu;

                if (stats != null && stats[0] != null) {
                    avgCpu = convertToDouble(stats[0]);
                    maxCpu = convertToDouble(stats[1]);
                    minCpu = convertToDouble(stats[2]);
                } else {
                    log.warn("⚠️ 장비 {}의 통계 데이터 없음, 현재값으로 대체", equipmentId);
                }

                CpuCurrentStatsDto cpuStats = CpuCurrentStatsDto.builder()
                        .currentCpuUsage(currentCpu)
                        .avgCpuUsage(avgCpu)
                        .maxCpuUsage(maxCpu)
                        .minCpuUsage(minCpu)
                        .currentLoadAvg1(latest.getLoadAvg1())
                        .currentLoadAvg5(latest.getLoadAvg5())
                        .currentLoadAvg15(latest.getLoadAvg15())
                        .lastUpdated(latest.getGenerateTime())
                        .build();

                equipmentStatsList.add(CpuStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(true)
                        .cpuStats(cpuStats)
                        .build());

                successCount++;

            } catch (Exception e) {
                log.error("❌ 장비 {} CPU 상태 조회 실패: {}", equipmentId, e.getMessage());
                equipmentStatsList.add(CpuStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        log.info("✅ 일괄 CPU 상태 조회 완료 - 성공: {}, 실패: {}", successCount, failureCount);

        return CpuCurrentStatsBatchDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .equipmentStats(equipmentStatsList)
                .build();
    }
}