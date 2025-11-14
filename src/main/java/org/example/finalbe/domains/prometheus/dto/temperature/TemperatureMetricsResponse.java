package org.example.finalbe.domains.prometheus.dto.temperature;

import java.util.List;

public record TemperatureMetricsResponse(
        Double currentTemperature,
        List<TemperatureResponse> temperatureTrend
) {
}