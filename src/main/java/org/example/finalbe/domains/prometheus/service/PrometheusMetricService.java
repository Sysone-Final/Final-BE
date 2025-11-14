package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.cpu.CpuMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.disk.DiskMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.memory.MemoryMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.network.NetworkMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.serverroom.ServerRoomMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.temperature.TemperatureMetricsResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusMetricService {

    private final PrometheusCpuMetricQueryService prometheusCpuMetricQueryService;
    private final PrometheusMemoryMetricQueryService prometheusMemoryMetricQueryService;
    private final PrometheusNetworkMetricQueryService prometheusNetworkMetricQueryService;
    private final PrometheusDiskMetricQueryService prometheusDiskMetricQueryService;
    private final PrometheusTemperatureMetricQueryService prometheusTemperatureMetricQueryService;

    public ServerRoomMetricsResponse getAllMetrics(Instant startTime, Instant endTime) {
        ZonedDateTime startKst = startTime.atZone(ZoneId.of("Asia/Seoul"));
        ZonedDateTime endKst = endTime.atZone(ZoneId.of("Asia/Seoul"));

        log.info("전체 메트릭 조회 시작 (KST) - startTime: {}, endTime: {}", startKst, endKst);

        return new ServerRoomMetricsResponse(
                getCpuMetrics(startTime, endTime),
                getMemoryMetrics(startTime, endTime),
                getNetworkMetrics(startTime, endTime),
                getDiskMetrics(startTime, endTime),
                getTemperatureMetrics(startTime, endTime)
        );
    }

    private CpuMetricsResponse getCpuMetrics(Instant startTime, Instant endTime) {
        try {
            return prometheusCpuMetricQueryService.getCpuMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("CPU 메트릭 조회 실패", e);
            return null;
        }
    }

    private MemoryMetricsResponse getMemoryMetrics(Instant startTime, Instant endTime) {
        try {
            return prometheusMemoryMetricQueryService.getMemoryMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("Memory 메트릭 조회 실패", e);
            return null;
        }
    }

    private NetworkMetricsResponse getNetworkMetrics(Instant startTime, Instant endTime) {
        try {
            return prometheusNetworkMetricQueryService.getNetworkMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("Network 메트릭 조회 실패", e);
            return null;
        }
    }

    private DiskMetricsResponse getDiskMetrics(Instant startTime, Instant endTime) {
        try {
            return prometheusDiskMetricQueryService.getDiskMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("Disk 메트릭 조회 실패", e);
            return null;
        }
    }

    private TemperatureMetricsResponse getTemperatureMetrics(Instant startTime, Instant endTime) {
        try {
            return prometheusTemperatureMetricQueryService.getTemperatureMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("Temperature 메트릭 조회 실패", e);
            return null;
        }
    }
}