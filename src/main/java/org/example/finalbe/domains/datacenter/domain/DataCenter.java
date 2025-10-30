package org.example.finalbe.domains.datacenter.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.member.domain.Member;

import java.math.BigDecimal;

/**
 * 전산실(데이터센터) 엔티티
 */
@Entity
@Table(name = "datacenter", indexes = {
        @Index(name = "idx_datacenter_name", columnList = "name"),
        @Index(name = "idx_datacenter_code", columnList = "code"),
        @Index(name = "idx_datacenter_status", columnList = "status")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DataCenter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "datacenter_id")
    private Long id; // 전산실 ID

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 전산실 이름

    @Column(name = "code", unique = true, length = 50)
    private String code; // 전산실 코드 (UNIQUE)

    @Column(name = "location", length = 255)
    private String location; // 전산실 위치/주소

    @Column(name = "floor", length = 50)
    private String floor; // 전산실 층수

    @Column(name = "rows")
    private Integer rows; // 랙 배치 행 수

    @Column(name = "columns")
    private Integer columns; // 랙 배치 열 수

    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl; // 평면도 이미지 URL

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private DataCenterStatus status; // 전산실 상태 (ACTIVE, INACTIVE, MAINTENANCE)

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 전산실 설명

    @Column(name = "total_area", precision = 10, scale = 2)
    private BigDecimal totalArea; // 총 면적 (m²)

    @Column(name = "total_power_capacity", precision = 10, scale = 2)
    private BigDecimal totalPowerCapacity; // 총 전력 용량 (kW)

    @Column(name = "total_cooling_capacity", precision = 10, scale = 2)
    private BigDecimal totalCoolingCapacity; // 총 냉각 용량 (kW)

    @Column(name = "max_rack_count")
    private Integer maxRackCount; // 최대 랙 개수

    @Column(name = "current_rack_count")
    @Builder.Default
    private Integer currentRackCount = 0; // 현재 랙 개수

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin; // 최저 허용 온도 (℃)

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax; // 최고 허용 온도 (℃)

    @Column(name = "humidity_min", precision = 5, scale = 2)
    private BigDecimal humidityMin; // 최저 허용 습도 (%)

    @Column(name = "humidity_max", precision = 5, scale = 2)
    private BigDecimal humidityMax; // 최고 허용 습도 (%)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Member manager; // 전산실 담당자

    /**
     * 전산실 정보 수정 (부분 수정 지원)
     */
    public void updateInfo(
            String name,
            String code,
            String location,
            String floor,
            Integer rows,
            Integer columns,
            String backgroundImageUrl,
            DataCenterStatus status,
            String description,
            BigDecimal totalArea,
            BigDecimal totalPowerCapacity,
            BigDecimal totalCoolingCapacity,
            Integer maxRackCount,
            BigDecimal temperatureMin,
            BigDecimal temperatureMax,
            BigDecimal humidityMin,
            BigDecimal humidityMax,
            Member manager
    ) {
        if (name != null && !name.trim().isEmpty()) this.name = name;
        if (code != null && !code.trim().isEmpty()) this.code = code;
        if (location != null) this.location = location;
        if (floor != null) this.floor = floor;
        if (rows != null) this.rows = rows;
        if (columns != null) this.columns = columns;
        if (backgroundImageUrl != null) this.backgroundImageUrl = backgroundImageUrl;
        if (status != null) this.status = status;
        if (description != null) this.description = description;
        if (totalArea != null) this.totalArea = totalArea;
        if (totalPowerCapacity != null) this.totalPowerCapacity = totalPowerCapacity;
        if (totalCoolingCapacity != null) this.totalCoolingCapacity = totalCoolingCapacity;
        if (maxRackCount != null) this.maxRackCount = maxRackCount;
        if (temperatureMin != null) this.temperatureMin = temperatureMin;
        if (temperatureMax != null) this.temperatureMax = temperatureMax;
        if (humidityMin != null) this.humidityMin = humidityMin;
        if (humidityMax != null) this.humidityMax = humidityMax;
        if (manager != null) this.manager = manager;

        this.updateTimestamp();
    }

    /**
     * 랙 추가 시 개수 증가
     */
    public void incrementRackCount() {
        if (this.currentRackCount == null) {
            this.currentRackCount = 0;
        }
        if (this.currentRackCount >= this.maxRackCount) {
            throw new IllegalStateException("최대 랙 수를 초과할 수 없습니다.");
        }
        this.currentRackCount++;
    }

    /**
     * 랙 제거 시 개수 감소
     */
    public void decrementRackCount() {
        if (this.currentRackCount == null || this.currentRackCount <= 0) {
            throw new IllegalStateException("현재 랙 수가 0입니다.");
        }
        this.currentRackCount--;
    }

    /**
     * 사용 가능한 랙 개수 계산
     */
    public int getAvailableRackCount() {
        return (maxRackCount != null ? maxRackCount : 0) - (currentRackCount != null ? currentRackCount : 0);
    }
}