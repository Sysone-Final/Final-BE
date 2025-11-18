package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;
import org.example.finalbe.domains.prometheus.domain.PrometheusMemoryMetric;

import java.time.Instant;

@Builder
public record MemoryMetricResponse(
        Instant time,
        String instance,
        Long totalBytes,
        Long usedBytes,
        Long freeBytes,
        Long availableBytes,
        Double usagePercent,
        Long buffersBytes,
        Long cachedBytes,
        Long activeBytes,
        Long inactiveBytes,
        Long swapTotalBytes,
        Long swapUsedBytes,
        Long swapFreeBytes,
        Double swapUsagePercent
) {
    public static MemoryMetricResponse from(PrometheusMemoryMetric metric) {
        return MemoryMetricResponse.builder()
                .time(metric.getTime())
                .instance(metric.getInstance())
                .totalBytes(metric.getTotalBytes())
                .usedBytes(metric.getUsedBytes())
                .freeBytes(metric.getFreeBytes())
                .availableBytes(metric.getAvailableBytes())
                .usagePercent(metric.getUsagePercent())
                .buffersBytes(metric.getBuffersBytes())
                .cachedBytes(metric.getCachedBytes())
                .activeBytes(metric.getActiveBytes())
                .inactiveBytes(metric.getInactiveBytes())
                .swapTotalBytes(metric.getSwapTotalBytes())
                .swapUsedBytes(metric.getSwapUsedBytes())
                .swapFreeBytes(metric.getSwapFreeBytes())
                .swapUsagePercent(metric.getSwapUsagePercent())
                .build();
    }
}