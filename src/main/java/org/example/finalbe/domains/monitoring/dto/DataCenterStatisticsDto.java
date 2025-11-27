// 작성자: 황요한
// 데이터센터 실시간 집계 통계 DTO

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCenterStatisticsDto {

    // 데이터센터 기본 정보
    private Long dataCenterId;
    private String dataCenterName;
    private LocalDateTime timestamp;

    // 서버실 통계
    private Integer totalServerRooms;
    private Integer activeServerRooms;

    // 랙 통계
    private Integer totalRacks;
    private Integer activeRacks;

    // 장비 통계
    private Integer totalEquipments;
    private Integer activeEquipments;
    private Integer inactiveEquipments;

    // CPU 통계
    private Double avgCpuUsage;
    private Double maxCpuUsage;
    private Double minCpuUsage;
    private Double avgLoadAvg1;
    private Double avgLoadAvg5;
    private Double avgLoadAvg15;

    // 메모리 통계
    private Double avgMemoryUsage;
    private Double maxMemoryUsage;
    private Double minMemoryUsage;
    private Long totalMemoryBytes;
    private Long usedMemoryBytes;

    // 스왑 통계
    private Double avgSwapUsage;

    // 디스크 통계
    private Double avgDiskUsage;
    private Double maxDiskUsage;
    private Double minDiskUsage;
    private Long totalDiskBytes;
    private Long usedDiskBytes;
    private Double avgDiskIoUsage;

    // 네트워크 통계
    private Double totalInBps;
    private Double totalOutBps;
    private Double avgRxUsage;
    private Double avgTxUsage;
    private Long totalInErrors;
    private Long totalOutErrors;

    // 환경 통계
    private Double avgTemperature;
    private Double maxTemperature;
    private Double minTemperature;
    private Double avgHumidity;
    private Double maxHumidity;
    private Double minHumidity;
    private Integer temperatureWarnings;
    private Integer humidityWarnings;

    // 알람 통계
    private Integer totalAlerts;
    private Integer criticalAlerts;
    private Integer warningAlerts;

    // 서버실 요약 리스트
    private List<ServerRoomSummaryDto> serverRoomSummaries;

    // 서버실 요약 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerRoomSummaryDto {
        private Long serverRoomId;
        private String serverRoomName;
        private Integer equipmentCount;
        private Double avgCpuUsage;
        private Double avgMemoryUsage;
        private Double avgDiskUsage;
        private Double avgTemperature;
        private Integer alertCount;
    }
}
