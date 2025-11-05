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
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, length = 50)
    private String code;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "rows")
    private Integer rows;

    @Column(name = "columns")
    private Integer columns;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private DataCenterStatus status;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_area", precision = 10, scale = 2)
    private BigDecimal totalArea;

    @Column(name = "total_power_capacity", precision = 10, scale = 2)
    private BigDecimal totalPowerCapacity;

    @Column(name = "total_cooling_capacity", precision = 10, scale = 2)
    private BigDecimal totalCoolingCapacity;

    @Column(name = "max_rack_count")
    private Integer maxRackCount;

    @Column(name = "current_rack_count")
    @Builder.Default
    private Integer currentRackCount = 0;

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin;

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax;

    @Column(name = "humidity_min", precision = 5, scale = 2)
    private BigDecimal humidityMin;

    @Column(name = "humidity_max", precision = 5, scale = 2)
    private BigDecimal humidityMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Member manager;

    // ★ company 필드 제거 - CompanyDataCenter 매핑 테이블로 관계 관리

    /**
     * 전산실 정보 수정
     */
    public void updateInfo(
            String name,
            String code,
            String location,
            Integer floor,
            Integer rows,
            Integer columns,
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
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        if (code != null && !code.trim().isEmpty()) {
            this.code = code;
        }
        if (location != null) {
            this.location = location;
        }
        if (floor != null) {
            this.floor = floor;
        }
        if (rows != null) {
            this.rows = rows;
        }
        if (columns != null) {
            this.columns = columns;
        }
        if (status != null) {
            this.status = status;
        }
        if (description != null) {
            this.description = description;
        }
        if (totalArea != null) {
            this.totalArea = totalArea;
        }
        if (totalPowerCapacity != null) {
            this.totalPowerCapacity = totalPowerCapacity;
        }
        if (totalCoolingCapacity != null) {
            this.totalCoolingCapacity = totalCoolingCapacity;
        }
        if (maxRackCount != null) {
            this.maxRackCount = maxRackCount;
        }
        if (temperatureMin != null) {
            this.temperatureMin = temperatureMin;
        }
        if (temperatureMax != null) {
            this.temperatureMax = temperatureMax;
        }
        if (humidityMin != null) {
            this.humidityMin = humidityMin;
        }
        if (humidityMax != null) {
            this.humidityMax = humidityMax;
        }
        if (manager != null) {
            this.manager = manager;
        }
    }

    /**
     * 랙 개수 증가
     */
    public void incrementRackCount() {
        this.currentRackCount++;
    }

    /**
     * 랙 개수 감소
     */
    public void decrementRackCount() {
        if (this.currentRackCount > 0) {
            this.currentRackCount--;
        }
    }

    /**
     * 사용 가능한 랙 개수 계산
     */
    public int getAvailableRackCount() {
        return this.maxRackCount - this.currentRackCount;
    }
}