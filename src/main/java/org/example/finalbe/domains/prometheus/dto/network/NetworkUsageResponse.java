package org.example.finalbe.domains.prometheus.dto.network;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record NetworkUsageResponse(
        ZonedDateTime time,
        Double rxBytesPerSec,
        Double txBytesPerSec
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static NetworkUsageResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);

        return new NetworkUsageResponse(
                timeKst,
                row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                row[2] != null ? ((Number) row[2]).doubleValue() : 0.0
        );
    }
}