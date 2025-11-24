package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RackStatisticsDto {

    private Long rackId;
    private String rackName;
    private LocalDateTime timestamp;
    private EnvironmentStats environment;
    private RackSummary rackSummary;
    private CpuStats cpuStats;
    private MemoryStats memoryStats;
    private DiskStats diskStats;
    private SystemLoadStats systemLoadStats;
    private NetworkStats networkStats;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnvironmentStats {
        private Double temperature;
        private Double minTemperature;
        private Double maxTemperature;
        private Double humidity;
        private Double minHumidity;
        private Double maxHumidity;
        private Boolean temperatureWarning;
        private Boolean humidityWarning;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RackSummary {
        private Integer totalEquipmentCount;
        private Integer normalCount;
        private Integer warningCount;
        private Integer errorCount;
        private List<EquipmentTypeCount> activeEquipmentTypes;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquipmentTypeCount {
        private String type;
        private Integer count;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CpuStats {
        private Double avgUsage;
        private Double maxUsage;
        private List<TopEquipment> topEquipments;
        private Integer equipmentCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryStats {
        private Double avgUsage;
        private Double maxUsage;
        private List<TopEquipment> topEquipments;
        private Integer equipmentCount;
        private Long totalMemoryGB;
        private Long usedMemoryGB;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskStats {
        private Double avgUsage;
        private Double maxUsage;
        private List<TopEquipment> topEquipments;
        private Integer equipmentCount;
        private Long totalCapacityTB;
        private Long usedCapacityTB;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NetworkStats {
        private Double totalRxMbps;
        private Double totalTxMbps;
        private Double avgRxUsage;
        private Double avgTxUsage;
        private List<TopEquipment> topRxEquipments;
        private List<TopEquipment> topTxEquipments;
        private Double errorPacketRate;
        private Double dropPacketRate;
        private Integer equipmentCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopEquipment {
        private Long equipmentId;
        private String equipmentName;
        private Double value;
    }

    // RackStatisticsDto.java에 inner class 추가

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemLoadStats {
        private Double avgLoadAvg1;    // 평균 1분 부하
        private Double avgLoadAvg5;    // 평균 5분 부하
        private Double avgLoadAvg15;   // 평균 15분 부하
        private Double maxLoadAvg1;    // 최대 1분 부하
        private Double maxLoadAvg5;    // 최대 5분 부하
        private Double maxLoadAvg15;   // 최대 15분 부하
        private Integer equipmentCount; // 측정 장비 수
    }
}