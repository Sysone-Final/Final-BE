package org.example.finalbe.domains.prometheus.dto;

import org.example.finalbe.domains.prometheus.domain.PrometheusCpuMetric;

import java.time.Instant;

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
        Double loadAvg1,
        Double loadAvg5,
        Double loadAvg15,
        Double contextSwitchesPerSec
) {
    public static CpuMetricResponse from(PrometheusCpuMetric entity) {
        return new CpuMetricResponse(
                entity.getTime(),
                entity.getInstance(),
                entity.getCpuUsagePercent(),
                entity.getUserPercent(),
                entity.getSystemPercent(),
                entity.getIowaitPercent(),
                entity.getIdlePercent(),
                entity.getNicePercent(),
                entity.getIrqPercent(),
                entity.getSoftirqPercent(),
                entity.getStealPercent(),
                entity.getLoadAvg1(),
                entity.getLoadAvg5(),
                entity.getLoadAvg15(),
                entity.getContextSwitchesPerSec()
        );
    }
}