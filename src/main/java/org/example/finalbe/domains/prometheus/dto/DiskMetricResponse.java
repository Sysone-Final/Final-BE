package org.example.finalbe.domains.prometheus.dto;

import org.example.finalbe.domains.prometheus.domain.PrometheusDiskMetric;

import java.time.Instant;

public record DiskMetricResponse(
        Instant time,
        String instance,
        String device,
        String mountpoint,
        Long totalBytes,
        Long usedBytes,
        Long freeBytes,
        Double usagePercent,
        Double readBytesPerSec,
        Double writeBytesPerSec,
        Double totalIoBytesPerSec,
        Double readIops,
        Double writeIops,
        Double ioUtilizationPercent,
        Double readTimePercent,
        Double writeTimePercent,
        Long totalInodes,
        Long usedInodes,
        Long freeInodes,
        Double inodeUsagePercent
) {
    public static DiskMetricResponse from(PrometheusDiskMetric entity) {
        return new DiskMetricResponse(
                entity.getTime(),
                entity.getInstance(),
                entity.getDevice(),
                entity.getMountpoint(),
                entity.getTotalBytes(),
                entity.getUsedBytes(),
                entity.getFreeBytes(),
                entity.getUsagePercent(),
                entity.getReadBytesPerSec(),
                entity.getWriteBytesPerSec(),
                entity.getTotalIoBytesPerSec(),
                entity.getReadIops(),
                entity.getWriteIops(),
                entity.getIoUtilizationPercent(),
                entity.getReadTimePercent(),
                entity.getWriteTimePercent(),
                entity.getTotalInodes(),
                entity.getUsedInodes(),
                entity.getFreeInodes(),
                entity.getInodeUsagePercent()
        );
    }
}