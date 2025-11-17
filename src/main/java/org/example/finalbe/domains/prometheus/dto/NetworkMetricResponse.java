package org.example.finalbe.domains.prometheus.dto;

import org.example.finalbe.domains.prometheus.domain.PrometheusNetworkMetric;

import java.time.Instant;

public record NetworkMetricResponse(
        Instant time,
        String instance,
        String device,
        Double rxUsagePercent,
        Double txUsagePercent,
        Double totalUsagePercent,
        Long rxPacketsTotal,
        Long txPacketsTotal,
        Long rxBytesTotal,
        Long txBytesTotal,
        Double rxBytesPerSec,
        Double txBytesPerSec,
        Double rxPacketsPerSec,
        Double txPacketsPerSec,
        Long rxErrorsTotal,
        Long txErrorsTotal,
        Long rxDroppedTotal,
        Long txDroppedTotal,
        Boolean interfaceUp
) {
    public static NetworkMetricResponse from(PrometheusNetworkMetric entity) {
        return new NetworkMetricResponse(
                entity.getTime(),
                entity.getInstance(),
                entity.getDevice(),
                entity.getRxUsagePercent(),
                entity.getTxUsagePercent(),
                entity.getTotalUsagePercent(),
                entity.getRxPacketsTotal(),
                entity.getTxPacketsTotal(),
                entity.getRxBytesTotal(),
                entity.getTxBytesTotal(),
                entity.getRxBytesPerSec(),
                entity.getTxBytesPerSec(),
                entity.getRxPacketsPerSec(),
                entity.getTxPacketsPerSec(),
                entity.getRxErrorsTotal(),
                entity.getTxErrorsTotal(),
                entity.getRxDroppedTotal(),
                entity.getTxDroppedTotal(),
                entity.getInterfaceUp()
        );
    }
}