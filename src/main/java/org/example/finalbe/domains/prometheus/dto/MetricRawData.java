// 작성자: 황요한
// Prometheus 원시 메트릭 DTO
package org.example.finalbe.domains.prometheus.dto;

import lombok.*;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricRawData {

    private Long equipmentId;     // 장비 ID
    private String instance;      // Prometheus 인스턴스 주소
    private Long timestamp;       // 수집 시각(Unix Time)

    // CPU
    @Builder.Default
    private Map<String, Double> cpuModes = new HashMap<>(); // user/system/idle 등

    // 메모리
    private Long totalMemory;      // 전체 메모리
    private Long freeMemory;       // free
    private Long availableMemory;  // available
    private Long memoryBuffers;
    private Long memoryCached;
    private Long memoryActive;
    private Long memoryInactive;
    private Long totalSwap;
    private Long usedSwap;

    // Load Average
    private Double loadAvg1;
    private Double loadAvg5;
    private Double loadAvg15;
    private Long contextSwitches;

    // 디스크
    private Long totalDisk;
    private Long usedDisk;
    private Long freeDisk;
    private Long totalInodes;
    private Long freeInodes;
    private Double diskReadBps;
    private Double diskWriteBps;
    private Long diskReadCount;
    private Long diskWriteCount;
    private Double diskIoTimePercentage;

    // 네트워크
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

    // 환경 센서
    private Double temperature;  // 온도

    // 빈 데이터 생성 (기본값 세팅)
    public static MetricRawData createEmpty(Long equipmentId, String instance) {
        return MetricRawData.builder()
                .equipmentId(equipmentId)
                .instance(instance)
                .cpuModes(new HashMap<>())
                .build();
    }
}
