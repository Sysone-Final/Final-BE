package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;
import org.example.finalbe.domains.prometheus.domain.PrometheusTemperatureMetric;

import java.time.Instant;

@Builder
public record TemperatureMetricResponse(
        Instant time,
        String instance,
        String chip,
        String sensor,
        Double tempCelsius
) {
    public static TemperatureMetricResponse from(PrometheusTemperatureMetric metric) {
        return TemperatureMetricResponse.builder()
                .time(metric.getTime())
                .instance(metric.getInstance())
                .chip(metric.getChip())
                .sensor(metric.getSensor())
                .tempCelsius(metric.getTempCelsius())
                .build();
    }
}