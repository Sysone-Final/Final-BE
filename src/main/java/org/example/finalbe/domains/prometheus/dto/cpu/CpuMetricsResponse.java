package org.example.finalbe.domains.prometheus.dto.cpu;

import java.util.List;

public record CpuMetricsResponse(
        Double currentCpuUsage,
        List<CpuUsageResponse> cpuUsageTrend,
        List<CpuModeDistributionResponse> cpuModeDistribution,
        List<LoadAverageResponse> loadAverageTrend,
        List<ContextSwitchResponse> contextSwitchTrend
) {
    public static CpuMetricsResponse of(
            Double currentCpuUsage,
            List<CpuUsageResponse> cpuUsageTrend,
            List<CpuModeDistributionResponse> cpuModeDistribution,
            List<LoadAverageResponse> loadAverageTrend,
            List<ContextSwitchResponse> contextSwitchTrend
    ) {
        return new CpuMetricsResponse(
                currentCpuUsage,
                cpuUsageTrend,
                cpuModeDistribution,
                loadAverageTrend,
                contextSwitchTrend
        );
    }
}