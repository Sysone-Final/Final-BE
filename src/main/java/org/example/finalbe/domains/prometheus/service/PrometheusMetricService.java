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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusMetricService {

    private final PrometheusCpuMetricQueryService prometheusCpuMetricQueryService;
    private final PrometheusMemoryMetricQueryService prometheusMemoryMetricQueryService;
    private final PrometheusNetworkMetricQueryService prometheusNetworkMetricQueryService;
    private final PrometheusDiskMetricQueryService prometheusDiskMetricQueryService;
    private final PrometheusTemperatureMetricQueryService prometheusTemperatureMetricQueryService;

    public ServerRoomMetricsResponse getAllMetrics(Instant startTime, Instant endTime) {
        log.info("전체 메트릭 조회 시작 - startTime: {}, endTime: {}", startTime, endTime);

        CpuMetricsResponse cpu = prometheusCpuMetricQueryService.getCpuMetrics(startTime, endTime);
        MemoryMetricsResponse memory = prometheusMemoryMetricQueryService.getMemoryMetrics(startTime, endTime);
        NetworkMetricsResponse network = prometheusNetworkMetricQueryService.getNetworkMetrics(startTime, endTime);
        DiskMetricsResponse disk = prometheusDiskMetricQueryService.getDiskMetrics(startTime, endTime);
        TemperatureMetricsResponse temperature = prometheusTemperatureMetricQueryService.getTemperatureMetrics(startTime, endTime);

        return ServerRoomMetricsResponse.builder()
                .cpu(cpu)
                .memory(memory)
                .network(network)
                .disk(disk)
                .temperature(temperature)
                .build();
    }
}