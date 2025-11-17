package org.example.finalbe.domains.prometheus.dto;

import org.example.finalbe.domains.prometheus.domain.*;

import java.time.Instant;
import java.util.List;

public record MetricsResponse(
        Instant timestamp,
        List<PrometheusCpuMetric> cpuMetrics,
        List<PrometheusMemoryMetric> memoryMetrics,
        List<PrometheusNetworkMetric> networkMetrics,
        List<PrometheusDiskMetric> diskMetrics,
        Integer totalRecords
) {
    public static MetricsResponse of(
            Instant timestamp,
            List<PrometheusCpuMetric> cpuMetrics,
            List<PrometheusMemoryMetric> memoryMetrics,
            List<PrometheusNetworkMetric> networkMetrics,
            List<PrometheusDiskMetric> diskMetrics
    ) {
        int total = cpuMetrics.size() + memoryMetrics.size()
                + networkMetrics.size() + diskMetrics.size();

        return new MetricsResponse(
                timestamp,
                cpuMetrics,
                memoryMetrics,
                networkMetrics,
                diskMetrics,
                total
        );
    }

    public static MetricsResponse empty() {
        return new MetricsResponse(
                Instant.now(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0
        );
    }
}