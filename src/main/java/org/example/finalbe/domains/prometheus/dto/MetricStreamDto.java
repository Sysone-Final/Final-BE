package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record MetricStreamDto(
        Long equipmentId,
        String instance,
        LocalDateTime timestamp,
        SystemMetricDto system,
        DiskMetricDto disk,
        NetworkMetricDto network,
        EnvironmentMetricDto environment
) {
    @Builder
    public record SystemMetricDto(
            Map<String, Double> cpu,
            MemoryDto memory,
            SwapDto swap,
            LoadDto load,
            Long contextSwitches
    ) {}

    @Builder
    public record MemoryDto(
            Long total,
            Long used,
            Long free,
            Double usedPercentage
    ) {}

    @Builder
    public record SwapDto(
            Long total,
            Long used,
            Double usedPercentage
    ) {}

    @Builder
    public record LoadDto(
            Double avg1,
            Double avg5,
            Double avg15
    ) {}

    @Builder
    public record DiskMetricDto(
            Long totalBytes,
            Long usedBytes,
            Long freeBytes,
            Double usedPercentage,
            IoDto io
    ) {}

    @Builder
    public record IoDto(
            Double readBps,
            Double writeBps,
            Double timePercentage
    ) {}

    @Builder
    public record NetworkMetricDto(
            Double rxBps,
            Double txBps,
            Double rxPps,
            Double txPps,
            Integer operStatus
    ) {}

    @Builder
    public record EnvironmentMetricDto(
            Double temperature,
            Double humidity
    ) {}

    public static MetricStreamDto from(org.example.finalbe.domains.prometheus.dto.MetricRawData raw) {
        return MetricStreamDto.builder()
                .equipmentId(raw.getEquipmentId())
                .instance(raw.getInstance())
                .timestamp(raw.getTimestamp() != null
                        ? LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(raw.getTimestamp()),
                        java.time.ZoneId.systemDefault())
                        : LocalDateTime.now())
                .system(SystemMetricDto.builder()
                        .cpu(raw.getCpuModes())
                        .memory(MemoryDto.builder()
                                .total(raw.getTotalMemory())
                                .used(raw.getTotalMemory() != null && raw.getFreeMemory() != null
                                        ? raw.getTotalMemory() - raw.getFreeMemory() : null)
                                .free(raw.getFreeMemory())
                                .usedPercentage(raw.getTotalMemory() != null && raw.getTotalMemory() > 0
                                        && raw.getFreeMemory() != null
                                        ? ((raw.getTotalMemory() - raw.getFreeMemory()) * 100.0) / raw.getTotalMemory()
                                        : null)
                                .build())
                        .swap(SwapDto.builder()
                                .total(raw.getTotalSwap())
                                .used(raw.getUsedSwap())  // ✅ getFreeSwap() → getUsedSwap()
                                .usedPercentage(raw.getTotalSwap() != null && raw.getTotalSwap() > 0
                                        && raw.getUsedSwap() != null
                                        ? (raw.getUsedSwap() * 100.0) / raw.getTotalSwap()
                                        : null)
                                .build())
                        .load(LoadDto.builder()
                                .avg1(raw.getLoadAvg1())
                                .avg5(raw.getLoadAvg5())
                                .avg15(raw.getLoadAvg15())
                                .build())
                        .contextSwitches(raw.getContextSwitches())
                        .build())
                .disk(DiskMetricDto.builder()
                        .totalBytes(raw.getTotalDisk())       // ✅ getDiskTotalBytes() → getTotalDisk()
                        .usedBytes(raw.getUsedDisk())         // ✅ getDiskUsedBytes() → getUsedDisk()
                        .freeBytes(raw.getFreeDisk())         // ✅ getDiskFreeBytes() → getFreeDisk()
                        .usedPercentage(raw.getTotalDisk() != null && raw.getTotalDisk() > 0
                                && raw.getUsedDisk() != null
                                ? (raw.getUsedDisk() * 100.0) / raw.getTotalDisk()
                                : null)
                        .io(IoDto.builder()
                                .readBps(raw.getDiskReadBps())
                                .writeBps(raw.getDiskWriteBps())
                                .timePercentage(raw.getDiskIoTimePercentage())
                                .build())
                        .build())
                .network(NetworkMetricDto.builder()
                        .rxBps(raw.getNetworkRxBps())
                        .txBps(raw.getNetworkTxBps())
                        .rxPps(raw.getNetworkRxPps())
                        .txPps(raw.getNetworkTxPps())
                        .operStatus(raw.getNetworkOperStatus())
                        .build())
                .environment(EnvironmentMetricDto.builder()
                        .temperature(raw.getTemperature())
                        .humidity(null)
                        .build())
                .build();
    }
}