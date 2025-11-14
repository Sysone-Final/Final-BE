package org.example.finalbe.domains.prometheus.dto.cpu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record LoadAverageResponse(
        ZonedDateTime time,
        Double load1,
        Double load5,
        Double load15
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static LoadAverageResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);
        Double load1 = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        Double load5 = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        Double load15 = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;

        return new LoadAverageResponse(timeKst, load1, load5, load15);
    }
}