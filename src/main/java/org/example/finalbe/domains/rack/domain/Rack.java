package org.example.finalbe.domains.rack.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
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

    @Column(name = "group_number", length = 50)
    private String groupNumber; // 그룹 번호

    @Column(name = "rack_location", length = 100)
    private String rackLocation; // 랙 위치

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

    @Column(name = "width")
    private BigDecimal width; // 폭 (mm)

    @Column(name = "depth")
    private BigDecimal depth; // 깊이 (mm)

    @Column(name = "height")
    private BigDecimal height; // 높이 (mm)

    @Column(name = "department", length = 100)
    private String department; // 담당 부서명

    @Column(name = "max_power_capacity")
    private BigDecimal maxPowerCapacity; // 최대 전력 용량 (W)

    @Column(name = "current_power_usage")
    private BigDecimal currentPowerUsage; // 현재 전력 사용량 (W)

    @Column(name = "measured_power")
    private BigDecimal measuredPower; // 실측 전력 사용량 (W)

    @Column(name = "max_weight_capacity")
    private BigDecimal maxWeightCapacity; // 최대 무게 용량 (kg)

    @Column(name = "current_weight")
    private BigDecimal currentWeight; // 현재 무게 (kg)

    @Column(name = "manufacturer", length = 100)
    private String manufacturer; // 제조사

    @Column(name = "serial_number", length = 100)
    private String serialNumber; // 일련번호

    @Column(name = "management_number", length = 100)
    private String managementNumber; // 관리 번호

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RackStatus status; // 랙 상태

    @Enumerated(EnumType.STRING)
    @Column(name = "rack_type", length = 20)
    private RackType rackType; // 랙 타입

    @Column(name = "color_code", length = 20)
    private String colorCode; // 색상 코드

    @Column(name = "notes", length = 1000)
    private String notes; // 비고

    @Column(name = "created_by", length = 100)
    private String createdBy; // 생성자

    @Column(name = "updated_by", length = 100)
    private String updatedBy; // 최종 수정자

    @Column(name = "manager_id", nullable = false)
    private Long managerId; // 담당자 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DataCenter datacenter; // 소속 전산실

    /**
     * 엔티티 생성 시 기본값 설정
     */
    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = RackStatus.ACTIVE;
        }
        if (this.currentPowerUsage == null) {
            this.currentPowerUsage = BigDecimal.ZERO;
        }
        if (this.currentWeight == null) {
            this.currentWeight = BigDecimal.ZERO;
        }
    }

    /**
     * 엔티티 수정 시 업데이트
     */
    @PreUpdate
    protected void onUpdate() {
        // BaseTimeEntity에서 updatedAt 자동 갱신
    }

    /**
     * 랙 정보 수정
     */
    public void updateInfo(RackUpdateRequest request, String updatedBy) {
        if (request.rackName() != null) {
            this.rackName = request.rackName();
        }
        if (request.groupNumber() != null) {
            this.groupNumber = request.groupNumber();
        }
        if (request.rackLocation() != null) {
            this.rackLocation = request.rackLocation();
        }
        if (request.totalUnits() != null) {
            this.totalUnits = request.totalUnits();
            this.availableUnits = this.totalUnits - this.usedUnits;
        }
        if (request.doorDirection() != null) {
            this.doorDirection = request.doorDirection();
        }
        if (request.zoneDirection() != null) {
            this.zoneDirection = request.zoneDirection();
        }
        if (request.width() != null) {
            this.width = request.width();
        }
        if (request.depth() != null) {
            this.depth = request.depth();
        }
        if (request.department() != null) {
            this.department = request.department();
        }
        if (request.maxPowerCapacity() != null) {
            this.maxPowerCapacity = request.maxPowerCapacity();
        }
        if (request.maxWeightCapacity() != null) {
            this.maxWeightCapacity = request.maxWeightCapacity();
        }
        if (request.manufacturer() != null) {
            this.manufacturer = request.manufacturer();
        }
        if (request.serialNumber() != null) {
            this.serialNumber = request.serialNumber();
        }
        if (request.managementNumber() != null) {
            this.managementNumber = request.managementNumber();
        }
        if (request.status() != null) {
            this.status = request.status();
        }
        if (request.rackType() != null) {
            this.rackType = request.rackType();
        }
        if (request.colorCode() != null) {
            this.colorCode = request.colorCode();
        }
        if (request.notes() != null) {
            this.notes = request.notes();
        }
        if (request.managerId() != null) {
            this.managerId = request.managerId();
        }

        this.updatedBy = updatedBy;
        this.updateTimestamp();
    }

    /**
     * 랙 상태 변경
     */
    public void changeStatus(RackStatus newStatus, String reason, String updatedBy) {
        RackStatus oldStatus = this.status;
        this.status = newStatus;

        String statusChangeLog = String.format("[%s] 상태 변경: %s → %s (사유: %s, 변경자: %s)",
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                oldStatus,
                newStatus,
                reason != null ? reason : "없음",
                updatedBy);

        if (this.notes == null || this.notes.trim().isEmpty()) {
            this.notes = statusChangeLog;
        } else {
            this.notes = this.notes + "\n" + statusChangeLog;
        }

        this.updatedBy = updatedBy;
        this.updateTimestamp();
    }

    /**
     * 랙에 장비 배치
     */
    public void placeEquipment(Equipment equipment, Integer startUnit, Integer unitSize) {
        equipment.setRack(this);
        equipment.setStartUnit(startUnit);
        equipment.setUnitSize(unitSize);

        this.usedUnits += unitSize;
        this.availableUnits = this.totalUnits - this.usedUnits;

        if (equipment.getPowerConsumption() != null) {
            if (this.currentPowerUsage == null) {
                this.currentPowerUsage = BigDecimal.ZERO;
            }
            this.currentPowerUsage = this.currentPowerUsage.add(equipment.getPowerConsumption());
        }

        if (equipment.getWeight() != null) {
            if (this.currentWeight == null) {
                this.currentWeight = BigDecimal.ZERO;
            }
            this.currentWeight = this.currentWeight.add(equipment.getWeight());
        }

        this.updateTimestamp();
    }

    /**
     * 랙에서 장비 제거
     */
    public void removeEquipment(Equipment equipment) {
        this.usedUnits -= equipment.getUnitSize();
        this.availableUnits = this.totalUnits - this.usedUnits;

        if (equipment.getPowerConsumption() != null && this.currentPowerUsage != null) {
            this.currentPowerUsage = this.currentPowerUsage.subtract(equipment.getPowerConsumption());
            if (this.currentPowerUsage.compareTo(BigDecimal.ZERO) < 0) {
                this.currentPowerUsage = BigDecimal.ZERO;
            }
        }

        if (equipment.getWeight() != null && this.currentWeight != null) {
            this.currentWeight = this.currentWeight.subtract(equipment.getWeight());
            if (this.currentWeight.compareTo(BigDecimal.ZERO) < 0) {
                this.currentWeight = BigDecimal.ZERO;
            }
        }

        this.updateTimestamp();
    }

    /**
     * 랙 내에서 장비 이동
     */
    public void moveEquipment(Equipment equipment, Integer fromUnit, Integer toUnit) {
        equipment.setStartUnit(toUnit);
        this.updateTimestamp();
    }

    /**
     * 랙 사용률 계산
     */
    public BigDecimal getUsageRate() {
        if (this.totalUnits == null || this.totalUnits == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal used = BigDecimal.valueOf(this.usedUnits);
        BigDecimal total = BigDecimal.valueOf(this.totalUnits);

        return used
                .divide(total, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 전력 사용률 계산
     */
    public BigDecimal getPowerUsageRate() {
        if (this.maxPowerCapacity == null || this.maxPowerCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (this.currentPowerUsage == null) {
            return BigDecimal.ZERO;
        }

        return this.currentPowerUsage
                .divide(this.maxPowerCapacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}