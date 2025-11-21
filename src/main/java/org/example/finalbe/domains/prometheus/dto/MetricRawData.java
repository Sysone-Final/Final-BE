package org.example.finalbe.domains.prometheus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricRawData {
    private Long equipmentId;
    private String instance;
    private Long timestamp;

    // System Metrics
    @Builder.Default
    private Map<String, Double> cpuModes = new HashMap<>();
    private Long totalMemory;
    private Long freeMemory;
    private Long availableMemory;
    private Long memoryBuffers;
    private Long memoryCached;
    private Long memoryActive;
    private Long memoryInactive;
    private Long totalSwap;
    private Long usedSwap;
    private Double loadAvg1;
    private Double loadAvg5;
    private Double loadAvg15;
    private Long contextSwitches;

    // Disk Metrics
    private Long totalDisk;            // diskTotalBytes → totalDisk
    private Long usedDisk;             // diskUsedBytes → usedDisk
    private Long freeDisk;             // diskFreeBytes → freeDisk
    private Long totalInodes;          // diskTotalInodes → totalInodes
    private Long freeInodes;           // diskFreeInodes → freeInodes
    private Double diskReadBps;
    private Double diskWriteBps;
    private Long diskReadCount;
    private Long diskWriteCount;
    private Double diskIoTimePercentage;

    // Network Metrics
    private Double networkRxBps;
    private Double networkTxBps;
    private Double networkRxPps;
    private Double networkTxPps;
    private Long networkRxErrors;
    private Long networkTxErrors;
    private Long networkRxDrops;
    private Long networkTxDrops;
    private Long networkRxBytesTotal;
    private Long networkTxBytesTotal;
    private Long networkRxPacketsTotal;
    private Long networkTxPacketsTotal;
    private Integer networkOperStatus;

    // Environment Metrics
    private Double temperature;

    public static MetricRawData createEmpty(Long equipmentId, String instance) {
        return MetricRawData.builder()
                .equipmentId(equipmentId)
                .instance(instance)
                .cpuModes(new HashMap<>())
                .build();
    }
}