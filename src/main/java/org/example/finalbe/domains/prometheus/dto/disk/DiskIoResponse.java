package org.example.finalbe.domains.prometheus.dto.disk;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record DiskIoResponse(
        ZonedDateTime time,
        Double readBytesPerSec,
        Double writeBytesPerSec,
        Double readIops,
        Double writeIops,
        Double ioUtilizationPercent
) {
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public static DiskIoResponse from(Object[] ioSpeed, Object[] iops, Object[] utilization) {
        Instant instant = (Instant) ioSpeed[0];
        ZonedDateTime timeKst = instant.atZone(KST_ZONE);

        return new DiskIoResponse(
                timeKst,
                ioSpeed[1] != null ? ((Number) ioSpeed[1]).doubleValue() : 0.0,
                ioSpeed[2] != null ? ((Number) ioSpeed[2]).doubleValue() : 0.0,
                iops != null && iops[1] != null ? ((Number) iops[1]).doubleValue() : 0.0,
                iops != null && iops[2] != null ? ((Number) iops[2]).doubleValue() : 0.0,
                utilization != null && utilization[1] != null ? ((Number) utilization[1]).doubleValue() : 0.0
        );
    }
}