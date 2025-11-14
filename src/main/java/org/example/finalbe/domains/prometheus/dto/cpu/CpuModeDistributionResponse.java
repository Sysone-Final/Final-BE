package org.example.finalbe.domains.prometheus.dto.cpu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record CpuModeDistributionResponse(
        ZonedDateTime time,
        Double userMode,
        Double systemMode,
        Double iowaitMode,
        Double irqMode,
        Double softirqMode
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static CpuModeDistributionResponse from(Object[] row) {
        Instant instant = (Instant) row[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);
        Double userMode = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        Double systemMode = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        Double iowaitMode = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        Double irqMode = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
        Double softirqMode = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;

        return new CpuModeDistributionResponse(timeKst, userMode, systemMode, iowaitMode, irqMode, softirqMode);
    }
}