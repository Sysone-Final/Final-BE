package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.cpu.*;
import org.example.finalbe.domains.prometheus.repository.cpu.PrometheusCpuMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
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

        return CpuMetricsResponse.of(
                getCurrentCpuUsage(),
                getCpuUsageTrend(startTime, endTime),
                getCpuModeDistribution(startTime, endTime),
                getLoadAverage(startTime, endTime),
                getContextSwitchTrend(startTime, endTime)
        );
    }

    private Double getCurrentCpuUsage() {
        try {
            Double result = prometheusCpuMetricRepository.getCurrentCpuUsage();
            return result != null ? result : 0.0;
        } catch (Exception e) {
            log.error("현재 CPU 사용률 조회 실패", e);
            return 0.0;
        }
    }

    private List<CpuUsageResponse> getCpuUsageTrend(Instant startTime, Instant endTime) {
        try {
            return prometheusCpuMetricRepository.getCpuUsageTrend(startTime, endTime)
                    .stream()
                    .map(CpuUsageResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("CPU 사용률 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<CpuModeDistributionResponse> getCpuModeDistribution(Instant startTime, Instant endTime) {
        try {
            return prometheusCpuMetricRepository.getCpuModeDistribution(startTime, endTime)
                    .stream()
                    .map(CpuModeDistributionResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("CPU 모드별 분포 조회 실패", e);
            return List.of();
        }
    }

    private List<LoadAverageResponse> getLoadAverage(Instant startTime, Instant endTime) {
        try {
            return prometheusCpuMetricRepository.getLoadAverage(startTime, endTime)
                    .stream()
                    .map(LoadAverageResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("시스템 부하 조회 실패", e);
            return List.of();
        }
    }

    private List<ContextSwitchResponse> getContextSwitchTrend(Instant startTime, Instant endTime) {
        try {
            return prometheusCpuMetricRepository.getContextSwitchTrend(startTime, endTime)
                    .stream()
                    .map(ContextSwitchResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("컨텍스트 스위치 추이 조회 실패", e);
            return List.of();
        }
    }
}