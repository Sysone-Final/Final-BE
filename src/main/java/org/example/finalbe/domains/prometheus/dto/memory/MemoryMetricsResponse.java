package org.example.finalbe.domains.prometheus.dto.memory;

import java.util.List;

public record MemoryMetricsResponse(
        Double currentMemoryUsagePercent,
        List<MemoryUsageResponse> memoryUsageTrend,
        List<MemoryCompositionResponse> memoryComposition,
        List<SwapUsageResponse> swapUsageTrend
) {
}