package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;
import org.example.finalbe.domains.prometheus.domain.PrometheusNetworkMetric;

import java.time.Instant;

@Builder
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
    public static NetworkMetricResponse from(PrometheusNetworkMetric metric) {
        return NetworkMetricResponse.builder()
                .time(metric.getTime())
                .instance(metric.getInstance())
                .device(metric.getDevice())
                .rxUsagePercent(metric.getRxUsagePercent())
                .txUsagePercent(metric.getTxUsagePercent())
                .totalUsagePercent(metric.getTotalUsagePercent())
                .rxPacketsTotal(metric.getRxPacketsTotal())
                .txPacketsTotal(metric.getTxPacketsTotal())
                .rxBytesTotal(metric.getRxBytesTotal())
                .txBytesTotal(metric.getTxBytesTotal())
                .rxBytesPerSec(metric.getRxBytesPerSec())
                .txBytesPerSec(metric.getTxBytesPerSec())
                .rxPacketsPerSec(metric.getRxPacketsPerSec())
                .txPacketsPerSec(metric.getTxPacketsPerSec())
                .rxErrorsTotal(metric.getRxErrorsTotal())
                .txErrorsTotal(metric.getTxErrorsTotal())
                .rxDroppedTotal(metric.getRxDroppedTotal())
                .txDroppedTotal(metric.getTxDroppedTotal())
                .interfaceUp(metric.getInterfaceUp())
                .build();
    }
}