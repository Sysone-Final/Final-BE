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

    @Column(name = "group_number", length = 50)
    private String groupNumber;

    @Column(name = "rack_location", length = 100)
    private String rackLocation;

    @Column(name = "total_units", nullable = false)
    private Integer totalUnits;

    @Column(name = "used_units", nullable = false)
    private Integer usedUnits;

    @Column(name = "available_units", nullable = false)
    private Integer availableUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "door_direction", length = 10)
    private DoorDirection doorDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_direction", length = 10)
    private ZoneDirection zoneDirection;

    @Column(name = "width")
    private BigDecimal width;

    @Column(name = "depth")
    private BigDecimal depth;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "max_power_capacity")
    private BigDecimal maxPowerCapacity;

    @Column(name = "current_power_usage")
    private BigDecimal currentPowerUsage;

    @Column(name = "measured_power")
    private BigDecimal measuredPower;

    @Column(name = "max_weight_capacity")
    private BigDecimal maxWeightCapacity;

    @Column(name = "current_weight")
    private BigDecimal currentWeight;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "management_number", length = 100)
    private String managementNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RackStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "rack_type", length = 20)
    private RackType rackType;

    @Column(name = "color_code", length = 20)
    private String colorCode;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "manager_id", nullable = false, length = 50)
    private Long managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DataCenter datacenter;

    /**
     * 랙 정보 업데이트
     */
    public void updateInfo(RackUpdateRequest request, String updatedBy) {
        if (request.rackName() != null && !request.rackName().trim().isEmpty()) {
            this.rackName = request.rackName();
        }
        if (request.groupNumber() != null) {
            this.groupNumber = request.groupNumber();
        }
        if (request.rackLocation() != null) {
            this.rackLocation = request.rackLocation();
        }
        if (request.totalUnits() != null) {
            // 총 유닛 수 변경 시 사용 가능 유닛 재계산
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
        this.status = newStatus;

        // 상태 변경 이력을 notes에 추가
        String statusChangeLog = String.format("[%s] 상태 변경: %s → %s (사유: %s, 변경자: %s)",
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                this.status,
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

    public void placeEquipment(Equipment equipment, Integer startUnit, Integer unitSize) {
        // 장비에 랙 정보 설정
        equipment.setRack(this);
        equipment.setStartUnit(startUnit);
        equipment.setUnitSize(unitSize);

        // 사용중 유닛 증가
        this.usedUnits += unitSize;
        this.availableUnits = this.totalUnits - this.usedUnits;

        // 전력 사용량 증가 (BigDecimal 연산)
        if (equipment.getPowerConsumption() != null) {
            if (this.currentPowerUsage == null) {
                this.currentPowerUsage = BigDecimal.ZERO;
            }
            this.currentPowerUsage = this.currentPowerUsage.add(equipment.getPowerConsumption());
        }

        // 무게 증가 (BigDecimal 연산)
        if (equipment.getWeight() != null) {
            if (this.currentWeight == null) {
                this.currentWeight = BigDecimal.ZERO;
            }
            this.currentWeight = this.currentWeight.add(equipment.getWeight());
        }

        this.updateTimestamp();
    }

    /**
     * 장비 제거
     */
    public void removeEquipment(Equipment equipment) {
        // 사용중 유닛 감소
        this.usedUnits -= equipment.getUnitSize();
        this.availableUnits = this.totalUnits - this.usedUnits;

        // 전력 사용량 감소 (BigDecimal 연산)
        if (equipment.getPowerConsumption() != null && this.currentPowerUsage != null) {
            this.currentPowerUsage = this.currentPowerUsage.subtract(equipment.getPowerConsumption());
            if (this.currentPowerUsage.compareTo(BigDecimal.ZERO) < 0) {
                this.currentPowerUsage = BigDecimal.ZERO; // 음수 방지
            }
        }

        // 무게 감소 (BigDecimal 연산)
        if (equipment.getWeight() != null && this.currentWeight != null) {
            this.currentWeight = this.currentWeight.subtract(equipment.getWeight());
            if (this.currentWeight.compareTo(BigDecimal.ZERO) < 0) {
                this.currentWeight = BigDecimal.ZERO; // 음수 방지
            }
        }

        this.updateTimestamp();
    }


    /**
     * 장비 이동
     */
    public void moveEquipment(Equipment equipment, Integer fromUnit, Integer toUnit) {
        // 장비의 시작 유닛만 변경
        equipment.setStartUnit(toUnit);
        this.updateTimestamp();
    }

    public BigDecimal getUsageRate() {
        if (this.totalUnits == null || this.totalUnits == 0) {
            return BigDecimal.ZERO;
        }

        // Integer → BigDecimal 변환
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

        // 둘 다 BigDecimal이므로 그대로 사용 가능
        return this.currentPowerUsage
                .divide(this.maxPowerCapacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
