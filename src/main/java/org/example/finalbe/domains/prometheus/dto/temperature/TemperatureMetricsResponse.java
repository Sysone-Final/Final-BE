package org.example.finalbe.domains.prometheus.dto.temperature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureMetricsResponse {
    private Double currentTemperature;
    private List<TemperatureResponse> temperatureTrend;
}