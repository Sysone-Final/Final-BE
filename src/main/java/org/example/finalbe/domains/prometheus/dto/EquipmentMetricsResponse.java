package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record EquipmentMetricsResponse(
        Long equipmentId,
        String instance,
        List<CpuMetricResponse> cpu,
        List<MemoryMetricResponse> memory,
        List<NetworkMetricResponse> network,
        List<DiskMetricResponse> disk,
        List<TemperatureMetricResponse> temperature,
        Instant timestamp
) {
}