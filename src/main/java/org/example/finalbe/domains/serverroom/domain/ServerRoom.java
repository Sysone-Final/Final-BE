// src/main/java/org/example/finalbe/domains/serverroom/domain/ServerRoom.java

package org.example.finalbe.domains.serverroom.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.math.BigDecimal;

/**
 * 서버실 엔티티
 * (기존 전산실/데이터센터)
 */
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
    private ServerRoomStatus status;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_area", precision = 10, scale = 2)
    private BigDecimal totalArea;

    @Column(name = "total_power_capacity", precision = 10, scale = 2)
    private BigDecimal totalPowerCapacity;

    @Column(name = "total_cooling_capacity", precision = 10, scale = 2)
    private BigDecimal totalCoolingCapacity;

    @Column(name = "temperature_min", precision = 5, scale = 2)
    private BigDecimal temperatureMin;

    @Column(name = "temperature_max", precision = 5, scale = 2)
    private BigDecimal temperatureMax;

    @Column(name = "humidity_min", precision = 5, scale = 2)
    private BigDecimal humidityMin;

    @Column(name = "humidity_max", precision = 5, scale = 2)
    private BigDecimal humidityMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false, length = 1)
    @Builder.Default
    private DelYN delYn = DelYN.N;

    /**
     * 현재 랙 개수
     */
    @Column(name = "current_rack_count")
    @Builder.Default
    private Integer currentRackCount = 0;

    /**
     * 소속 데이터센터 (nullable)
     * 서버실을 그룹화하기 위한 용도
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    private DataCenter dataCenter;

    /**
     * 서버실 정보 수정
     */
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
    }

    /**
     * 데이터센터 설정
     */
    public void setDataCenter(DataCenter dataCenter) {
        this.dataCenter = dataCenter;
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
     * 소프트 삭제
     */
    public void softDelete() {
        this.delYn = DelYN.Y;
    }
}