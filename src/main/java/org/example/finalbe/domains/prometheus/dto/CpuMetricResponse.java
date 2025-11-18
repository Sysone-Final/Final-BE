package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;
import org.example.finalbe.domains.prometheus.domain.PrometheusCpuMetric;

import java.time.Instant;

@Builder
public record CpuMetricResponse(
        Instant time,
        String instance,
        Double cpuUsagePercent,
        Double userPercent,
        Double systemPercent,
        Double iowaitPercent,
        Double idlePercent,
        Double nicePercent,
        Double irqPercent,
        Double softirqPercent,
        Double stealPercent,
        Double contextSwitchesPerSec,
        Double loadAvg1,
        Double loadAvg5,
        Double loadAvg15
) {
    public static CpuMetricResponse from(PrometheusCpuMetric metric) {
        return CpuMetricResponse.builder()
                .time(metric.getTime())
                .instance(metric.getInstance())
                .cpuUsagePercent(metric.getCpuUsagePercent())
                .userPercent(metric.getUserPercent())
                .systemPercent(metric.getSystemPercent())
                .iowaitPercent(metric.getIowaitPercent())
                .idlePercent(metric.getIdlePercent())
                .nicePercent(metric.getNicePercent())
                .irqPercent(metric.getIrqPercent())
                .softirqPercent(metric.getSoftirqPercent())
                .stealPercent(metric.getStealPercent())
                .contextSwitchesPerSec(metric.getContextSwitchesPerSec())
                .loadAvg1(metric.getLoadAvg1())
                .loadAvg5(metric.getLoadAvg5())
                .loadAvg15(metric.getLoadAvg15())
                .build();
    }
}