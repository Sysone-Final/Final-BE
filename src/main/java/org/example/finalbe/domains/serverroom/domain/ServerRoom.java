/**
 * 작성자: 황요한
 * 서버실 엔티티
 */
package org.example.finalbe.domains.serverroom.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.math.BigDecimal;

@Entity
@Table(name = "serverroom", indexes = {
        @Index(name = "idx_serverroom_name", columnList = "name"),
        @Index(name = "idx_serverroom_code", columnList = "code"),
        @Index(name = "idx_serverroom_status", columnList = "status"),
        @Index(name = "idx_serverroom_datacenter", columnList = "datacenter_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ServerRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "serverroom_id")
    private Long id;                        // 서버실 ID

    @Column(nullable = false, length = 100)
    private String name;                    // 서버실 이름

    @Column(unique = true, length = 50)
    private String code;                    // 코드

    @Column(length = 255)
    private String location;                // 위치

    private Integer floor;                  // 층수
    private Integer rows;                   // 그리드 행 수
    private Integer columns;                // 그리드 열 수

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ServerRoomStatus status;        // 서버실 상태

    @Column(columnDefinition = "TEXT")
    private String description;             // 설명

    @Column(precision = 10, scale = 2)
    private BigDecimal totalArea;           // 총 면적

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPowerCapacity;  // 전력 용량

    @Column(precision = 10, scale = 2)
    private BigDecimal totalCoolingCapacity; // 냉각 용량

    @Column(precision = 5, scale = 2)
    private BigDecimal temperatureMin;      // 온도 최소

    @Column(precision = 5, scale = 2)
    private BigDecimal temperatureMax;      // 온도 최대

    @Column(precision = 5, scale = 2)
    private BigDecimal humidityMin;         // 습도 최소

    @Column(precision = 5, scale = 2)
    private BigDecimal humidityMax;         // 습도 최대

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    @Builder.Default
    private DelYN delYn = DelYN.N;          // 삭제 여부

    @Builder.Default
    private Integer currentRackCount = 0;   // 랙 개수

    @Builder.Default
    private Boolean monitoringEnabled = true; // 모니터링 활성화 여부

    private Integer avgCpuThresholdWarning;     // CPU Warning
    private Integer avgCpuThresholdCritical;    // CPU Critical

    private Integer avgMemoryThresholdWarning;  // 메모리 Warning
    private Integer avgMemoryThresholdCritical; // 메모리 Critical

    private Integer avgDiskThresholdWarning;    // 디스크 Warning
    private Integer avgDiskThresholdCritical;   // 디스크 Critical

    private Integer avgTemperatureThresholdWarning;  // 온도 Warning
    private Integer avgTemperatureThresholdCritical; // 온도 Critical

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    private DataCenter dataCenter;          // 데이터센터

    /** 서버실 정보 수정 */
    public void updateInfo(
            String name,
            String code,
            String location,
            Integer floor,
            Integer rows,
            Integer columns,
            ServerRoomStatus status,
            String description,
            BigDecimal totalArea,
            BigDecimal totalPowerCapacity,
            BigDecimal totalCoolingCapacity,
            BigDecimal temperatureMin,
            BigDecimal temperatureMax,
            BigDecimal humidityMin,
            BigDecimal humidityMax
    ) {
        if (name != null && !name.trim().isEmpty()) this.name = name;
        if (code != null && !code.trim().isEmpty()) this.code = code;
        if (location != null) this.location = location;
        if (floor != null) this.floor = floor;
        if (rows != null) this.rows = rows;
        if (columns != null) this.columns = columns;
        if (status != null) this.status = status;
        if (description != null) this.description = description;
        if (totalArea != null) this.totalArea = totalArea;
        if (totalPowerCapacity != null) this.totalPowerCapacity = totalPowerCapacity;
        if (totalCoolingCapacity != null) this.totalCoolingCapacity = totalCoolingCapacity;
        if (temperatureMin != null) this.temperatureMin = temperatureMin;
        if (temperatureMax != null) this.temperatureMax = temperatureMax;
        if (humidityMin != null) this.humidityMin = humidityMin;
        if (humidityMax != null) this.humidityMax = humidityMax;
    }

    /** 평균 임계치 수정 */
    public void updateAverageThresholds(
            Integer cpuWarning,
            Integer cpuCritical,
            Integer memWarning,
            Integer memCritical,
            Integer diskWarning,
            Integer diskCritical,
            Integer tempWarning,
            Integer tempCritical
    ) {
        this.avgCpuThresholdWarning = cpuWarning;
        this.avgCpuThresholdCritical = cpuCritical;
        this.avgMemoryThresholdWarning = memWarning;
        this.avgMemoryThresholdCritical = memCritical;
        this.avgDiskThresholdWarning = diskWarning;
        this.avgDiskThresholdCritical = diskCritical;
        this.avgTemperatureThresholdWarning = tempWarning;
        this.avgTemperatureThresholdCritical = tempCritical;
    }

    /** 모니터링 토글 */
    public void toggleMonitoring() {
        this.monitoringEnabled = !this.monitoringEnabled;
    }

    /** 데이터센터 설정 */
    public void setDataCenter(DataCenter dataCenter) {
        this.dataCenter = dataCenter;
    }

    /** 랙 개수 증가 */
    public void incrementRackCount() {
        this.currentRackCount++;
    }

    /** 랙 개수 감소 */
    public void decrementRackCount() {
        if (this.currentRackCount > 0) this.currentRackCount--;
    }

    /** 소프트 삭제 */
    public void softDelete() {
        this.delYn = DelYN.Y;
    }
}
