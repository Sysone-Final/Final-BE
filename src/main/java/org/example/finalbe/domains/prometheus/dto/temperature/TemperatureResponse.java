package org.example.finalbe.domains.prometheus.dto.temperature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureResponse {
    private Instant time;
    private Double avgTemperature;
    private Double maxTemperature;
    private Double minTemperature;
}