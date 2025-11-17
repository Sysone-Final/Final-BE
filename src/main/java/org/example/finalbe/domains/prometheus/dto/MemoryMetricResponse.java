package org.example.finalbe.domains.prometheus.dto;

import org.example.finalbe.domains.prometheus.domain.PrometheusMemoryMetric;

import java.time.Instant;

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
    public static MemoryMetricResponse from(PrometheusMemoryMetric entity) {
        return new MemoryMetricResponse(
                entity.getTime(),
                entity.getInstance(),
                entity.getTotalBytes(),
                entity.getUsedBytes(),
                entity.getFreeBytes(),
                entity.getAvailableBytes(),
                entity.getUsagePercent(),
                entity.getBuffersBytes(),
                entity.getCachedBytes(),
                entity.getActiveBytes(),
                entity.getInactiveBytes(),
                entity.getSwapTotalBytes(),
                entity.getSwapUsedBytes(),
                entity.getSwapFreeBytes(),
                entity.getSwapUsagePercent()
        );
    }
}