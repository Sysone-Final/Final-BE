package org.example.finalbe.domains.prometheus.dto.memory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record TopNMemoryUsageResponse(
        Long instanceId,
        String instanceName,
        ZonedDateTime time,
        Double totalMemory,
        Double usedMemory,
        Double memoryUsagePercent
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static TopNMemoryUsageResponse from(Object[] row) {
        Instant instant = (Instant) row[2];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);

        return new TopNMemoryUsageResponse(
                row[0] != null ? ((Number) row[0]).longValue() : null,
                row[1] != null ? (String) row[1] : "Unknown",
                timeKst,
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0,
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0,
                row[5] != null ? ((Number) row[5]).doubleValue() : 0.0
        );
    }
}