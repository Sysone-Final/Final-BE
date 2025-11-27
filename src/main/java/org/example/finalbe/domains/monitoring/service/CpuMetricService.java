/**
 * 작성자: 황요한
 * CPU 메트릭 조회 및 대시보드 데이터 제공 서비스
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
public class CpuMetricService {

    private final SystemMetricRepository systemMetricRepository;

    // CPU 섹션 전체 데이터 조회
    public CpuSectionResponseDto getCpuSectionData(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime, AggregationLevel aggregationLevel) {

        log.info("📊 CPU 섹션 데이터 조회 시작 - 장비 ID: {}, 기간: {} ~ {}, 집계: {}",
                equipmentId, startTime, endTime, aggregationLevel);

        CpuCurrentStatsDto currentStats = getCurrentCpuStats(equipmentId, startTime, endTime);

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
            case DAY:
                aggregatedData = getCpuAggregatedData1Day(equipmentId, startTime, endTime);
                return buildCpuSectionFromAggregated(currentStats, aggregatedData);
            case RAW:
            default:
                metrics = systemMetricRepository.findByEquipmentIdAndTimeRange(equipmentId, startTime, endTime);
                return buildCpuSectionFromRaw(currentStats, metrics);
        }
    }

    // 1일 단위 집계 조회
    private List<CpuAggregatedStatsDto> getCpuAggregatedData1Day(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        return systemMetricRepository.getCpuAggregatedStats1Day(equipmentId, startTime, endTime)
                .stream().map(this::mapToCpuAggregatedStats).collect(Collectors.toList());
    }

    // 현재 CPU 상태 조회
    public CpuCurrentStatsDto getCurrentCpuStats(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        SystemMetric latest = systemMetricRepository.findLatestByEquipmentId(equipmentId)
                .orElseThrow(() -> new RuntimeException("메트릭 데이터가 없습니다."));

        Object[] stats = systemMetricRepository.getCpuUsageStats(equipmentId, startTime, endTime);

        Double avgCpu = 0.0, maxCpu = 0.0, minCpu = 0.0;

        if (stats != null && stats.length > 0) {
            Object first = stats[0];
            if (first instanceof Object[]) {
                Object[] arr = (Object[]) first;
                if (arr.length >= 3) {
                    avgCpu = convertToDouble(arr[0]);
                    maxCpu = convertToDouble(arr[1]);
                    minCpu = convertToDouble(arr[2]);
                }
            } else if (stats.length >= 3) {
                avgCpu = convertToDouble(stats[0]);
                maxCpu = convertToDouble(stats[1]);
                minCpu = convertToDouble(stats[2]);
            }
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

    // Object를 Double로 변환
    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    // 1분 단위 집계 조회
    private List<CpuAggregatedStatsDto> getCpuAggregatedData1Minute(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        return systemMetricRepository.getCpuAggregatedStats1Minute(equipmentId, startTime, endTime)
                .stream().map(this::mapToCpuAggregatedStats).collect(Collectors.toList());
    }

    // 5분 단위 집계 조회
    private List<CpuAggregatedStatsDto> getCpuAggregatedData5Minutes(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        return systemMetricRepository.getCpuAggregatedStats5Minutes(equipmentId, startTime, endTime)
                .stream().map(this::mapToCpuAggregatedStats).collect(Collectors.toList());
    }

    // 1시간 단위 집계 조회
    private List<CpuAggregatedStatsDto> getCpuAggregatedData1Hour(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime) {

        return systemMetricRepository.getCpuAggregatedStats1Hour(equipmentId, startTime, endTime)
                .stream().map(this::mapToCpuAggregatedStats).collect(Collectors.toList());
    }

    // 집계 결과 매핑
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

    // RAW 데이터 기반 CPU 섹션 생성
    private CpuSectionResponseDto buildCpuSectionFromRaw(
            CpuCurrentStatsDto currentStats, List<SystemMetric> metrics) {

        List<CpuUsagePointDto> cpuUsageTrend = new ArrayList<>();
        List<CpuModeDistributionDto> cpuModeDistribution = new ArrayList<>();
        List<LoadAveragePointDto> loadAverageTrend = new ArrayList<>();
        List<ContextSwitchPointDto> contextSwitchTrend = new ArrayList<>();

        SystemMetric prev = null;

        for (SystemMetric m : metrics) {

            cpuUsageTrend.add(CpuUsagePointDto.builder()
                    .timestamp(m.getGenerateTime())
                    .cpuUsagePercent(100 - (m.getCpuIdle() != null ? m.getCpuIdle() : 0.0))
                    .build());

            cpuModeDistribution.add(CpuModeDistributionDto.builder()
                    .timestamp(m.getGenerateTime())
                    .userPercent(m.getCpuUser())
                    .systemPercent(m.getCpuSystem())
                    .iowaitPercent(m.getCpuWait())
                    .irqPercent(m.getCpuIrq())
                    .softirqPercent(m.getCpuSoftirq())
                    .nicePercent(m.getCpuNice())
                    .stealPercent(m.getCpuSteal())
                    .idlePercent(m.getCpuIdle())
                    .build());

            loadAverageTrend.add(LoadAveragePointDto.builder()
                    .timestamp(m.getGenerateTime())
                    .loadAvg1(m.getLoadAvg1())
                    .loadAvg5(m.getLoadAvg5())
                    .loadAvg15(m.getLoadAvg15())
                    .build());

            if (prev != null && m.getContextSwitches() != null && prev.getContextSwitches() != null) {
                long diff = m.getContextSwitches() - prev.getContextSwitches();
                long sec = java.time.Duration.between(prev.getGenerateTime(), m.getGenerateTime()).getSeconds();
                long perSec = sec > 0 ? diff / sec : 0;

                contextSwitchTrend.add(ContextSwitchPointDto.builder()
                        .timestamp(m.getGenerateTime())
                        .contextSwitchesPerSec(perSec)
                        .build());
            }

            prev = m;
        }

        return CpuSectionResponseDto.builder()
                .currentStats(currentStats)
                .cpuUsageTrend(cpuUsageTrend)
                .cpuModeDistribution(cpuModeDistribution)
                .loadAverageTrend(loadAverageTrend)
                .contextSwitchTrend(contextSwitchTrend)
                .build();
    }

    // 집계 데이터 기반 CPU 섹션 생성
    private CpuSectionResponseDto buildCpuSectionFromAggregated(
            CpuCurrentStatsDto currentStats, List<CpuAggregatedStatsDto> aggregated) {

        List<CpuUsagePointDto> cpuUsageTrend = aggregated.stream()
                .map(a -> CpuUsagePointDto.builder()
                        .timestamp(a.getTimestamp())
                        .cpuUsagePercent(a.getAvgCpuUsage())
                        .build())
                .collect(Collectors.toList());

        List<LoadAveragePointDto> loadAverageTrend = aggregated.stream()
                .map(a -> LoadAveragePointDto.builder()
                        .timestamp(a.getTimestamp())
                        .loadAvg1(a.getAvgLoadAvg1())
                        .build())
                .collect(Collectors.toList());

        List<ContextSwitchPointDto> contextSwitchTrend = aggregated.stream()
                .filter(a -> a.getSampleCount() != null && a.getSampleCount() > 0)
                .map(a -> ContextSwitchPointDto.builder()
                        .timestamp(a.getTimestamp())
                        .contextSwitchesPerSec(a.getTotalContextSwitches() / a.getSampleCount())
                        .build())
                .collect(Collectors.toList());

        return CpuSectionResponseDto.builder()
                .currentStats(currentStats)
                .cpuUsageTrend(cpuUsageTrend)
                .cpuModeDistribution(null)
                .loadAverageTrend(loadAverageTrend)
                .contextSwitchTrend(contextSwitchTrend)
                .build();
    }

    // 조회 구간에 따른 자동 집계 레벨 결정
    public AggregationLevel determineOptimalAggregationLevel(LocalDateTime startTime, LocalDateTime endTime) {
        long hours = java.time.Duration.between(startTime, endTime).toHours();
        long days = java.time.Duration.between(startTime, endTime).toDays();

        if (days < 1) {
            if (hours <= 1) return AggregationLevel.RAW;
            if (hours <= 6) return AggregationLevel.MIN;
            return AggregationLevel.MIN5;
        }
        if (days <= 30) return AggregationLevel.HOUR;
        return AggregationLevel.DAY;
    }

    // 여러 장비의 현재 CPU 상태 일괄 조회
    public CpuCurrentStatsBatchDto getCurrentCpuStatsBatch(List<Long> equipmentIds) {

        log.info("📊 일괄 CPU 상태 조회 시작 - 장비 개수: {}", equipmentIds.size());

        List<CpuStatsWithEquipmentDto> equipmentStats = new ArrayList<>();
        int success = 0, fail = 0;

        List<SystemMetric> latestList = systemMetricRepository.findLatestByEquipmentIds(equipmentIds);
        Map<Long, SystemMetric> latestMap = latestList.stream()
                .collect(Collectors.toMap(SystemMetric::getEquipmentId, m -> m));

        List<Object[]> statsList = systemMetricRepository.getCpuUsageStatsBatch(equipmentIds, 60);
        Map<Long, Object[]> statsMap = statsList.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> new Object[]{row[1], row[2], row[3]}
                ));

        for (Long equipmentId : equipmentIds) {
            try {
                SystemMetric latest = latestMap.get(equipmentId);

                if (latest == null) {
                    equipmentStats.add(CpuStatsWithEquipmentDto.builder()
                            .equipmentId(equipmentId)
                            .success(false)
                            .errorMessage("메트릭 데이터가 없습니다.")
                            .build());
                    fail++;
                    continue;
                }

                Object[] stats = statsMap.get(equipmentId);
                Double currentCpu = 100 - (latest.getCpuIdle() != null ? latest.getCpuIdle() : 0.0);
                Double avgCpu = currentCpu, maxCpu = currentCpu, minCpu = currentCpu;

                if (stats != null) {
                    avgCpu = convertToDouble(stats[0]);
                    maxCpu = convertToDouble(stats[1]);
                    minCpu = convertToDouble(stats[2]);
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

                equipmentStats.add(CpuStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(true)
                        .cpuStats(cpuStats)
                        .build());
                success++;

            } catch (Exception e) {
                equipmentStats.add(CpuStatsWithEquipmentDto.builder()
                        .equipmentId(equipmentId)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                fail++;
            }
        }

        return CpuCurrentStatsBatchDto.builder()
                .successCount(success)
                .failureCount(fail)
                .equipmentStats(equipmentStats)
                .build();
    }
}
