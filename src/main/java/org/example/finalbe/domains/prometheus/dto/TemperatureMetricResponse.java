package org.example.finalbe.domains.prometheus.dto;

import org.example.finalbe.domains.prometheus.domain.PrometheusTemperatureMetric;

import java.time.Instant;

public record TemperatureMetricResponse(
        Instant time,
        String instance,
        String chip,
        String sensor,
        Double tempCelsius
) {
    public static TemperatureMetricResponse from(PrometheusTemperatureMetric entity) {
        return new TemperatureMetricResponse(
                entity.getTime(),
                entity.getInstance(),
                entity.getChip(),
                entity.getSensor(),
                entity.getTempCelsius()
        );
    }
}