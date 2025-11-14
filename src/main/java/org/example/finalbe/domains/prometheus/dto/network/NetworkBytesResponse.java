package org.example.finalbe.domains.prometheus.dto.network;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record NetworkBytesResponse(
        ZonedDateTime time,
        Double totalReceiveBytes,
        Double totalTransmitBytes
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static NetworkBytesResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);

        return new NetworkBytesResponse(
                timeKst,
                row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                row[2] != null ? ((Number) row[2]).doubleValue() : 0.0
        );
    }
}