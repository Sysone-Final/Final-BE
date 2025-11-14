package org.example.finalbe.domains.prometheus.dto.memory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record MemoryCompositionResponse(
        ZonedDateTime time,
        Double active,
        Double inactive,
        Double buffers,
        Double cached,
        Double free
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static MemoryCompositionResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);

        return new MemoryCompositionResponse(
                timeKst,
                row[1] != null ? ((Number) row[1]).doubleValue() : 0.0,
                row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0,
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0,
                row[5] != null ? ((Number) row[5]).doubleValue() : 0.0
        );
    }
}