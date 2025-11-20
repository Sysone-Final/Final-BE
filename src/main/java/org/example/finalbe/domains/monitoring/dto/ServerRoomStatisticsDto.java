package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서버실 실시간 통계 DTO
 * SSE로 전송되는 서버실 레벨 집계 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerRoomStatisticsDto {

    // 기본 정보
    private Long serverRoomId;
    private String serverRoomName;
    private LocalDateTime timestamp;

    // 장비 통계
    private Integer totalEquipments;      // 전체 장비 수
    private Integer activeEquipments;     // 활성 장비 수
    private Integer inactiveEquipments;   // 비활성 장비 수

    // CPU 통계 (평균)
    private Double avgCpuUsage;           // 평균 CPU 사용률 (%)
    private Double maxCpuUsage;           // 최대 CPU 사용률 (%)
    private Double minCpuUsage;           // 최소 CPU 사용률 (%)
    private Double avgLoadAvg1;           // 평균 Load Average (1분)

    // 메모리 통계 (평균)
    private Double avgMemoryUsage;        // 평균 메모리 사용률 (%)
    private Double maxMemoryUsage;        // 최대 메모리 사용률 (%)
    private Double minMemoryUsage;        // 최소 메모리 사용률 (%)
    private Long totalMemoryBytes;        // 전체 메모리 합계 (bytes)
    private Long usedMemoryBytes;         // 사용 중 메모리 합계 (bytes)

    // 스왑 통계
    private Double avgSwapUsage;          // 평균 스왑 사용률 (%)

    // 디스크 통계 (평균)
    private Double avgDiskUsage;          // 평균 디스크 사용률 (%)
    private Double maxDiskUsage;          // 최대 디스크 사용률 (%)
    private Double minDiskUsage;          // 최소 디스크 사용률 (%)
    private Long totalDiskBytes;          // 전체 디스크 용량 합계 (bytes)
    private Long usedDiskBytes;           // 사용 중 디스크 합계 (bytes)
    private Double avgDiskIoUsage;        // 평균 디스크 I/O 사용률 (%)

    // 네트워크 통계 (합계)
    private Double totalInBps;            // 총 수신 속도 (bytes/sec)
    private Double totalOutBps;           // 총 송신 속도 (bytes/sec)
    private Double avgRxUsage;            // 평균 수신 사용률 (%)
    private Double avgTxUsage;            // 평균 송신 사용률 (%)
    private Long totalInErrors;           // 총 수신 에러 수
    private Long totalOutErrors;          // 총 송신 에러 수

    // 랙 통계
    private Integer totalRacks;           // 전체 랙 수
    private Integer activeRacks;          // 활성 랙 수

    // 환경 통계 (평균)
    private Double avgTemperature;        // 평균 온도 (°C)
    private Double maxTemperature;        // 최대 온도 (°C)
    private Double minTemperature;        // 최소 온도 (°C)
    private Double avgHumidity;           // 평균 습도 (%)
    private Double maxHumidity;           // 최대 습도 (%)
    private Double minHumidity;           // 최소 습도 (%)
    private Integer temperatureWarnings;  // 온도 경고 수
    private Integer humidityWarnings;     // 습도 경고 수

    // 알람 통계
    private Integer totalAlerts;          // 전체 알람 수
    private Integer criticalAlerts;       // Critical 알람 수
    private Integer warningAlerts;        // Warning 알람 수

    // 전력 통계
    private Double totalPowerUsage;       // 총 전력 사용량 (kW)
    private Double avgPowerUsagePerRack;  // 랙당 평균 전력 사용량 (kW)
}