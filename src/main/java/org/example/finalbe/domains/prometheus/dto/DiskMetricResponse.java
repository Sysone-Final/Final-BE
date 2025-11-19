package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;
import org.example.finalbe.domains.prometheus.domain.PrometheusDiskMetric;

import java.time.Instant;

@Builder
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
    public static DiskMetricResponse from(PrometheusDiskMetric metric) {
        return DiskMetricResponse.builder()
                .time(metric.getTime())
                .instance(metric.getInstance())
                .device(metric.getDevice())
                .mountpoint(metric.getMountpoint())
                .totalBytes(metric.getTotalBytes())
                .usedBytes(metric.getUsedBytes())
                .freeBytes(metric.getFreeBytes())
                .usagePercent(metric.getUsagePercent())
                .readBytesPerSec(metric.getReadBytesPerSec())
                .writeBytesPerSec(metric.getWriteBytesPerSec())
                .totalIoBytesPerSec(metric.getTotalIoBytesPerSec())
                .readIops(metric.getReadIops())
                .writeIops(metric.getWriteIops())
                .ioUtilizationPercent(metric.getIoUtilizationPercent())
                .readTimePercent(metric.getReadTimePercent())
                .writeTimePercent(metric.getWriteTimePercent())
                .totalInodes(metric.getTotalInodes())
                .usedInodes(metric.getUsedInodes())
                .freeInodes(metric.getFreeInodes())
                .inodeUsagePercent(metric.getInodeUsagePercent())
                .build();
    }
}