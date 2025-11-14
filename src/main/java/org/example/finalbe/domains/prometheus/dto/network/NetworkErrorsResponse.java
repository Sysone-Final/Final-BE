package org.example.finalbe.domains.prometheus.dto.network;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record NetworkErrorsResponse(
        ZonedDateTime time,
        Double rxErrors,
        Double txErrors,
        Double rxDrops,
        Double txDrops
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static NetworkErrorsResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);

        return new NetworkErrorsResponse(
                timeKst,
                row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0,
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0
        );
    }
}