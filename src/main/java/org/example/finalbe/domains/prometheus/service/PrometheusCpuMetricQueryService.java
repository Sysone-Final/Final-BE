package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.cpu.*;
import org.example.finalbe.domains.prometheus.dto.cpu.CpuMetricsResponse;
import org.example.finalbe.domains.prometheus.repository.cpu.PrometheusCpuMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusCpuMetricQueryService {

    private final PrometheusCpuMetricRepository prometheusCpuMetricRepository;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public CpuMetricsResponse getCpuMetrics(Instant startTime, Instant endTime) {
        ZonedDateTime startKst = startTime.atZone(KST_ZONE);
        ZonedDateTime endKst = endTime.atZone(KST_ZONE);

        log.info("CPU 메트릭 조회 시작 (KST) - startTime: {}, endTime: {}", startKst, endKst);

        Double currentCpuUsage = getCurrentCpuUsage();
        List<CpuUsageResponse> cpuUsageTrend = getCpuUsageTrend(startTime, endTime);
        List<CpuModeDistributionResponse> cpuModeDistribution = getCpuModeDistribution(startTime, endTime);
        List<LoadAverageResponse> loadAverageTrend = getLoadAverage(startTime, endTime);
        List<ContextSwitchResponse> contextSwitchTrend = getContextSwitchTrend(startTime, endTime);

        return CpuMetricsResponse.builder()
                .currentCpuUsage(currentCpuUsage)
                .cpuUsageTrend(cpuUsageTrend)
                .cpuModeDistribution(cpuModeDistribution)
                .loadAverageTrend(loadAverageTrend)
                .contextSwitchTrend(contextSwitchTrend)
                .build();
    }

    private Double getCurrentCpuUsage() {
        try {
            Double result = prometheusCpuMetricRepository.getCurrentCpuUsage();
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            log.error("현재 CPU 사용률 조회 실패", e);
        }
        return 0.0;
    }

    private List<CpuUsageResponse> getCpuUsageTrend(Instant startTime, Instant endTime) {
        List<CpuUsageResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusCpuMetricRepository.getCpuUsageTrend(startTime, endTime);
            for (Object[] row : rows) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(CpuUsageResponse.builder()
                        .time(timeKst)
                        .cpuUsagePercent(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("CPU 사용률 추이 조회 실패", e);
        }
        return result;
    }

    private List<CpuModeDistributionResponse> getCpuModeDistribution(Instant startTime, Instant endTime) {
        List<CpuModeDistributionResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusCpuMetricRepository.getCpuModeDistribution(startTime, endTime);
            for (Object[] row : rows) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(CpuModeDistributionResponse.builder()
                        .time(timeKst)
                        .userMode(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .systemMode(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .iowaitMode(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .irqMode(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                        .softirqMode(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("CPU 모드별 분포 조회 실패", e);
        }
        return result;
    }

    private List<LoadAverageResponse> getLoadAverage(Instant startTime, Instant endTime) {
        List<LoadAverageResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusCpuMetricRepository.getLoadAverage(startTime, endTime);
            for (Object[] row : rows) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(LoadAverageResponse.builder()
                        .time(timeKst)
                        .load1(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .load5(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .load15(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("시스템 부하 조회 실패", e);
        }
        return result;
    }

    private List<ContextSwitchResponse> getContextSwitchTrend(Instant startTime, Instant endTime) {
        List<ContextSwitchResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusCpuMetricRepository.getContextSwitchTrend(startTime, endTime);
            for (Object[] row : rows) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(ContextSwitchResponse.builder()
                        .time(timeKst)
                        .contextSwitchesPerSec(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("컨텍스트 스위치 추이 조회 실패", e);
        }
        return result;
    }
}