package org.example.finalbe.domains.prometheus.dto;

import java.time.Instant;
import java.util.List;

public record MetricsApiResponse(
        Instant timestamp,
        String timeRange,
        CpuMetricsData cpu,
        MemoryMetricsData memory,
        NetworkMetricsData network,
        DiskMetricsData disk,
        Integer totalRecords
) {
    public record CpuMetricsData(
            List<CpuMetricResponse> metrics,
            Integer count
    ) {
        public static CpuMetricsData of(List<CpuMetricResponse> metrics) {
            return new CpuMetricsData(metrics, metrics.size());
        }
    }

    public record MemoryMetricsData(
            List<MemoryMetricResponse> metrics,
            Integer count
    ) {
        public static MemoryMetricsData of(List<MemoryMetricResponse> metrics) {
            return new MemoryMetricsData(metrics, metrics.size());
        }
    }

    public record NetworkMetricsData(
            List<NetworkMetricResponse> metrics,
            Integer count
    ) {
        public static NetworkMetricsData of(List<NetworkMetricResponse> metrics) {
            return new NetworkMetricsData(metrics, metrics.size());
        }
    }

    public record DiskMetricsData(
            List<DiskMetricResponse> metrics,
            Integer count
    ) {
        public static DiskMetricsData of(List<DiskMetricResponse> metrics) {
            return new DiskMetricsData(metrics, metrics.size());
        }
    }

    public static MetricsApiResponse of(
            String timeRange,
            List<CpuMetricResponse> cpuMetrics,
            List<MemoryMetricResponse> memoryMetrics,
            List<NetworkMetricResponse> networkMetrics,
            List<DiskMetricResponse> diskMetrics
    ) {
        int total = cpuMetrics.size() + memoryMetrics.size()
                + networkMetrics.size() + diskMetrics.size();

        return new MetricsApiResponse(
                Instant.now(),
                timeRange,
                CpuMetricsData.of(cpuMetrics),
                MemoryMetricsData.of(memoryMetrics),
                NetworkMetricsData.of(networkMetrics),
                DiskMetricsData.of(diskMetrics),
                total
        );
    }
}