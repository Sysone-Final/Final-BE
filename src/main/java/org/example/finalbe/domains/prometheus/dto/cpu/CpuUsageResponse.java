package org.example.finalbe.domains.prometheus.dto.cpu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record CpuUsageResponse(
        ZonedDateTime time,
        Double cpuUsagePercent
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static CpuUsageResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);
        Double cpuUsage = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;

        return new CpuUsageResponse(timeKst, cpuUsage);
    }
}