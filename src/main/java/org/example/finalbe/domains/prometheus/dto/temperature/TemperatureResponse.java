package org.example.finalbe.domains.prometheus.dto.temperature;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record TemperatureResponse(
        ZonedDateTime time,
        Double avgTemperature,
        Double maxTemperature,
        Double minTemperature
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static TemperatureResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);

        return new TemperatureResponse(
                timeKst,
                row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0
        );
    }
}