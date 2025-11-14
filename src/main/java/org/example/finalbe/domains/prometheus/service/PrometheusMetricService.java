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

        // 각 메트릭을 독립적으로 조회하여 일부 실패해도 다른 메트릭은 반환
        CpuMetricsResponse cpu = null;
        MemoryMetricsResponse memory = null;
        NetworkMetricsResponse network = null;
        DiskMetricsResponse disk = null;
        TemperatureMetricsResponse temperature = null;

        try {
            cpu = prometheusCpuMetricQueryService.getCpuMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("CPU 메트릭 조회 실패", e);
        }

        try {
            memory = prometheusMemoryMetricQueryService.getMemoryMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("Memory 메트릭 조회 실패", e);
        }

        try {
            network = prometheusNetworkMetricQueryService.getNetworkMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("Network 메트릭 조회 실패", e);
        }

        try {
            disk = prometheusDiskMetricQueryService.getDiskMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("Disk 메트릭 조회 실패", e);
        }

        try {
            temperature = prometheusTemperatureMetricQueryService.getTemperatureMetrics(startTime, endTime);
        } catch (Exception e) {
            log.error("Temperature 메트릭 조회 실패", e);
        }

        return ServerRoomMetricsResponse.builder()
                .cpu(cpu)
                .memory(memory)
                .network(network)
                .disk(disk)
                .temperature(temperature)
                .build();
    }
}