package org.example.finalbe.domains.prometheus.dto.cpu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuMetricsResponse {
    private Double currentCpuUsage;
    private List<CpuUsageResponse> cpuUsageTrend;
    private List<CpuModeDistributionResponse> cpuModeDistribution;
    private List<LoadAverageResponse> loadAverageTrend;
    private List<ContextSwitchResponse> contextSwitchTrend;
}