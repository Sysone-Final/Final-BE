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

    @Column(name = "floor", length = 50)
    private String floor;

    @Column(name = "rows")
    private Integer rows;

    @Column(name = "columns")
    private Integer columns;

    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

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
    private Integer currentRackCount;

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin;

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax;

    @Column(name = "humidity_min", precision = 5, scale = 2)
    private BigDecimal humidityMin;

    @Column(name = "humidity_max", precision = 5, scale = 2)
    private BigDecimal humidityMax;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // 담당자 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Member manager;

    // 정보 업데이트 메서드
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
            Member manager,
            String updatedBy
    ) {
        if (name != null) this.name = name;
        if (code != null) this.code = code;
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
        if (updatedBy != null) this.updatedBy = updatedBy;
        this.updateTimestamp();
    }

    // 랙 추가
    public void incrementRackCount() {
        if (this.currentRackCount == null) {
            this.currentRackCount = 0;
        }
        if (this.currentRackCount >= this.maxRackCount) {
            throw new IllegalStateException("최대 랙 수를 초과할 수 없습니다.");
        }
        this.currentRackCount++;
    }

    // 랙 제거
    public void decrementRackCount() {
        if (this.currentRackCount == null || this.currentRackCount <= 0) {
            throw new IllegalStateException("현재 랙 수가 0입니다.");
        }
        this.currentRackCount--;
    }

    // 사용 가능한 랙 수
    public int getAvailableRackCount() {
        return (maxRackCount != null ? maxRackCount : 0) - (currentRackCount != null ? currentRackCount : 0);
    }
}