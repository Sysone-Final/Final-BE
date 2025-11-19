package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AggregatedMetricsResponse(
        String aggregationType,  // "rack", "serverRoom", "dataCenter"
        Long aggregationId,
        Integer equipmentCount,
        Double avgCpuUsagePercent,
        Double avgMemoryUsagePercent,
        Double avgNetworkUsagePercent,
        Double avgDiskUsagePercent,
        Double avgTemperatureCelsius,
        Instant timestamp
) {
    public static AggregatedMetricsResponse empty(String aggregationType, Long aggregationId) {
        return AggregatedMetricsResponse.builder()
                .aggregationType(aggregationType)
                .aggregationId(aggregationId)
                .equipmentCount(0)
                .avgCpuUsagePercent(0.0)
                .avgMemoryUsagePercent(0.0)
                .avgNetworkUsagePercent(0.0)
                .avgDiskUsagePercent(0.0)
                .avgTemperatureCelsius(0.0)
                .timestamp(Instant.now())
                .build();
    }
}