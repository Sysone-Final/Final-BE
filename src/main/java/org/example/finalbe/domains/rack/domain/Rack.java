package org.example.finalbe.domains.rack.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.rack.dto.RackUpdateRequest;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 랙 엔티티
 */
@Entity
@Table(name = "rack")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rack extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rack_id")
    private Long id;

    @Column(name = "rack_name", nullable = false, length = 100)
    private String rackName;

    @Column(name = "grid_x")
    private BigDecimal gridX;

    @Column(name = "grid_y")
    private BigDecimal gridY;

    @Column(name = "total_units")
    private Integer totalUnits;

    @Column(name = "used_units")
    private Integer usedUnits;

    @Column(name = "available_units")
    private Integer availableUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "door_direction", length = 10)
    private DoorDirection doorDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_direction", length = 10)
    private ZoneDirection zoneDirection;

    @Column(name = "max_power_capacity", precision = 10, scale = 2)
    private BigDecimal maxPowerCapacity;

    @Column(name = "current_power_usage", precision = 10, scale = 2)
    private BigDecimal currentPowerUsage;

    @Column(name = "measured_power", precision = 10, scale = 2)
    private BigDecimal measuredPower;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RackStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "rack_type", length = 20)
    private RackType rackType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ✅ 알림 모니터링 활성화 여부
    @Column(name = "monitoring_enabled")
    @Builder.Default
    private Boolean monitoringEnabled = true;

    // ✅ 온도 임계치 (WARNING)
    @Column(name = "temperature_threshold_warning")
    private Integer temperatureThresholdWarning;

    // ✅ 온도 임계치 (CRITICAL)
    @Column(name = "temperature_threshold_critical")
    private Integer temperatureThresholdCritical;

    // ✅ 습도 최소 임계치 (WARNING)
    @Column(name = "humidity_threshold_min_warning")
    private Integer humidityThresholdMinWarning;

    // ✅ 습도 최소 임계치 (CRITICAL)
    @Column(name = "humidity_threshold_min_critical")
    private Integer humidityThresholdMinCritical;

    // ✅ 습도 최대 임계치 (WARNING)
    @Column(name = "humidity_threshold_max_warning")
    private Integer humidityThresholdMaxWarning;

    // ✅ 습도 최대 임계치 (CRITICAL)
    @Column(name = "humidity_threshold_max_critical")
    private Integer humidityThresholdMaxCritical;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serverRoom_id")
    private ServerRoom serverRoom;

    /**
     * 랙 정보 수정
     */
    public void updateInfo(RackUpdateRequest request) {
        this.rackName = request.rackName();
        this.gridX = request.gridX();
        this.gridY = request.gridY();
        this.totalUnits = request.totalUnits();
        this.doorDirection = request.doorDirection();
        this.zoneDirection = request.zoneDirection();
        this.maxPowerCapacity = request.maxPowerCapacity();
        this.manufacturer = request.manufacturer();
        this.serialNumber = request.serialNumber();
        this.status = request.status();
        this.rackType = request.rackType();
        this.notes = request.notes();
    }

    /**
     * ✅ 환경 임계치 설정
     */
    public void updateEnvironmentThresholds(
            Integer tempWarning,
            Integer tempCritical,
            Integer humidMinWarning,
            Integer humidMinCritical,
            Integer humidMaxWarning,
            Integer humidMaxCritical) {
        this.temperatureThresholdWarning = tempWarning;
        this.temperatureThresholdCritical = tempCritical;
        this.humidityThresholdMinWarning = humidMinWarning;
        this.humidityThresholdMinCritical = humidMinCritical;
        this.humidityThresholdMaxWarning = humidMaxWarning;
        this.humidityThresholdMaxCritical = humidMaxCritical;
    }

    /**
     * ✅ 모니터링 활성화/비활성화 토글
     */
    public void toggleMonitoring() {
        this.monitoringEnabled = !this.monitoringEnabled;
    }

    /**
     * 유닛 사용률 계산
     */
    public BigDecimal getUsageRate() {
        if (totalUnits == null || totalUnits == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(usedUnits)
                .divide(BigDecimal.valueOf(totalUnits), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 전력 사용률 계산
     */
    public BigDecimal getPowerUsageRate() {
        if (maxPowerCapacity == null || maxPowerCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (currentPowerUsage == null) {
            return BigDecimal.ZERO;
        }
        return currentPowerUsage
                .divide(maxPowerCapacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 유닛 사용
     */
    public void occupyUnits(int units) {
        if (this.availableUnits < units) {
            throw new IllegalStateException("사용 가능한 유닛이 부족합니다.");
        }
        this.usedUnits += units;
        this.availableUnits -= units;
    }

    /**
     * 유닛 해제
     */
    public void releaseUnits(int units) {
        this.usedUnits -= units;
        this.availableUnits += units;
    }

    /**
     * 전력 사용량 추가
     */
    public void addPowerUsage(BigDecimal power) {
        if (power == null) {
            return;
        }
        if (this.currentPowerUsage == null) {
            this.currentPowerUsage = BigDecimal.ZERO;
        }
        this.currentPowerUsage = this.currentPowerUsage.add(power);
    }

    /**
     * 전력 사용량 차감
     */
    public void subtractPowerUsage(BigDecimal power) {
        if (power == null) {
            return;
        }
        if (this.currentPowerUsage == null) {
            this.currentPowerUsage = BigDecimal.ZERO;
            return;
        }
        this.currentPowerUsage = this.currentPowerUsage.subtract(power);
        if (this.currentPowerUsage.compareTo(BigDecimal.ZERO) < 0) {
            this.currentPowerUsage = BigDecimal.ZERO;
        }
    }

    /**
     * 장비 배치
     */
    public void placeEquipment(Equipment equipment, Integer startUnit, Integer unitSize) {
        this.occupyUnits(unitSize);
        if (equipment.getPowerConsumption() != null) {
            this.addPowerUsage(equipment.getPowerConsumption());
        }
        equipment.setRack(this);
        equipment.setStartUnit(startUnit);
        equipment.setUnitSize(unitSize);
    }

    /**
     * 장비 제거
     */
    public void removeEquipment(Equipment equipment) {
        this.releaseUnits(equipment.getUnitSize());
        if (equipment.getPowerConsumption() != null) {
            this.subtractPowerUsage(equipment.getPowerConsumption());
        }
        equipment.setRack(null);
        equipment.setStartUnit(null);
    }

    /**
     * 장비 이동
     */
    public void moveEquipment(Equipment equipment, Integer fromUnit, Integer toUnit) {
        equipment.setStartUnit(toUnit);
    }

    /**
     * 서버룸 조회
     */
    public ServerRoom getServerRoom() {
        return this.serverRoom;
    }

    /**
     * 서버룸 ID 조회
     */
    public Long getServerRoomId() {
        return this.serverRoom != null ? this.serverRoom.getId() : null;
    }

    /**
     * 서버룸 이름 조회
     */
    public String getServerRoomName() {
        return this.serverRoom != null ? this.serverRoom.getName() : null;
    }

    /**
     * 서버룸 변경
     */
    public void changeServerRoom(ServerRoom newServerRoom) {
        if (newServerRoom == null) {
            throw new IllegalArgumentException("서버룸은 필수입니다.");
        }
        this.serverRoom = newServerRoom;
    }
}