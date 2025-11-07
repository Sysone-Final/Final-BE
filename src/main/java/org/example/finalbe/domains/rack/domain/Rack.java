package org.example.finalbe.domains.rack.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.rack.dto.RackUpdateRequest;

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
    private Long id; // 랙 ID

    @Column(name = "rack_name", nullable = false, length = 100)
    private String rackName; // 랙 이름

    @Column(name = "grid_x")
    private BigDecimal gridX; // X 좌표

    @Column(name = "grid_y")
    private BigDecimal gridY; // Y 좌표

    @Column(name = "total_units", nullable = false)
    private Integer totalUnits; // 전체 유닛 수

    @Column(name = "used_units", nullable = false)
    private Integer usedUnits; // 사용 중인 유닛 수

    @Column(name = "available_units", nullable = false)
    private Integer availableUnits; // 사용 가능한 유닛 수

    @Enumerated(EnumType.STRING)
    @Column(name = "door_direction", length = 10)
    private DoorDirection doorDirection; // 도어 방향

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_direction", length = 10)
    private ZoneDirection zoneDirection; // 존 방향

    @Column(name = "max_power_capacity", precision = 10, scale = 2)
    private BigDecimal maxPowerCapacity; // 최대 전력 용량

    @Column(name = "current_power_usage", precision = 10, scale = 2)
    private BigDecimal currentPowerUsage; // 현재 전력 사용량

    @Column(name = "measured_power", precision = 10, scale = 2)
    private BigDecimal measuredPower; // 실측 전력

    @Column(name = "manufacturer", length = 100)
    private String manufacturer; // 제조사

    @Column(name = "serial_number", length = 100)
    private String serialNumber; // 시리얼 번호

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RackStatus status; // 랙 상태

    @Enumerated(EnumType.STRING)
    @Column(name = "rack_type", length = 20)
    private RackType rackType; // 랙 타입

    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // 비고

    // 서버실(전산실)과의 관계 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serverroom_id", nullable = false)
    private ServerRoom serverroom; // 서버실(전산실)

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
        // 유닛 점유
        this.occupyUnits(unitSize);

        // 전력 사용량 추가
        if (equipment.getPowerConsumption() != null) {
            this.addPowerUsage(equipment.getPowerConsumption());
        }

        // 장비에 랙 정보 설정
        equipment.setRack(this);
        equipment.setStartUnit(startUnit);
        equipment.setUnitSize(unitSize);
    }

    /**
     * 장비 제거
     */
    public void removeEquipment(Equipment equipment) {
        // 유닛 해제
        this.releaseUnits(equipment.getUnitSize());

        // 전력 사용량 차감
        if (equipment.getPowerConsumption() != null) {
            this.subtractPowerUsage(equipment.getPowerConsumption());
        }

        // 장비의 랙 정보 제거
        equipment.setRack(null);
        equipment.setStartUnit(null);
    }

    /**
     * 장비 이동
     */
    public void moveEquipment(Equipment equipment, Integer fromUnit, Integer toUnit) {
        // 장비의 시작 유닛만 변경 (유닛 점유/해제는 필요 없음)
        equipment.setStartUnit(toUnit);
    }

    /**
     * 서버룸 조회
     */
    public ServerRoom getServerRoom() {
        return this.serverroom;
    }

    /**
     * 서버룸 ID 조회
     */
    public Long getServerRoomId() {
        return this.serverroom != null ? this.serverroom.getId() : null;
    }

    /**
     * 서버룸 이름 조회
     */
    public String getServerRoomName() {
        return this.serverroom != null ? this.serverroom.getName() : null;
    }

    /**
     * 서버룸 변경
     */
    public void changeServerRoom(ServerRoom newServerRoom) {
        if (newServerRoom == null) {
            throw new IllegalArgumentException("서버룸은 필수입니다.");
        }
        this.serverroom = newServerRoom;
    }
}