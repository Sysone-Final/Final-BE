/**
 * 작성자: 황요한
 * 서버실 실시간 통계 DTO
 */
package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerRoomStatisticsDto {

    private Long serverRoomId;
    private String serverRoomName;
    private LocalDateTime timestamp;

    private Integer totalEquipments;
    private Integer activeEquipments;
    private Integer inactiveEquipments;

    private Double avgCpuUsage;
    private Double maxCpuUsage;
    private Double minCpuUsage;
    private Double avgLoadAvg1;
    private Double avgLoadAvg5;
    private Double avgLoadAvg15;

    private Double avgMemoryUsage;
    private Double maxMemoryUsage;
    private Double minMemoryUsage;
    private Long totalMemoryBytes;
    private Long usedMemoryBytes;

    private Double avgSwapUsage;

    private Double avgDiskUsage;
    private Double maxDiskUsage;
    private Double minDiskUsage;
    private Long totalDiskBytes;
    private Long usedDiskBytes;
    private Double avgDiskIoUsage;

    private Double totalInBps;
    private Double totalOutBps;
    private Double avgRxUsage;
    private Double avgTxUsage;
    private Long totalInErrors;
    private Long totalOutErrors;

    private Integer totalRacks;
    private Integer activeRacks;

    private Double avgTemperature;
    private Double maxTemperature;
    private Double minTemperature;
    private Double avgHumidity;
    private Double maxHumidity;
    private Double minHumidity;
    private Integer temperatureWarnings;
    private Integer humidityWarnings;

    private Integer totalAlerts;
    private Integer criticalAlerts;
    private Integer warningAlerts;

    private Double totalPowerUsage;
    private Double avgPowerUsagePerRack;
}
