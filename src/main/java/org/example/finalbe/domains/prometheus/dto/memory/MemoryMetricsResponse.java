package org.example.finalbe.domains.prometheus.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMetricsResponse {
    private Double currentMemoryUsagePercent;
    private List<MemoryUsageResponse> memoryUsageTrend;
    private List<MemoryCompositionResponse> memoryComposition;
    private List<SwapUsageResponse> swapUsageTrend;
}