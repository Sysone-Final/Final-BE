package org.example.finalbe.domains.prometheus.dto.memory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record MemoryUsageResponse(
        ZonedDateTime time,
        Double totalMemory,
        Double availableMemory,
        Double usedMemory,
        Double memoryUsagePercent
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static MemoryUsageResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);
        Double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        Double available = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

        return new MemoryUsageResponse(
                timeKst,
                total,
                available,
                total - available,
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0
        );
    }
}