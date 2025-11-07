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
    @JoinColumn(name = "serverroom_id", nullable = false)
    private ServerRoom serverRoom; // 소속 서버실

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
        // BaseTimeEntity가 자동으로 updatedAt을 처리
    }

    /**
     * 랙 정보 업데이트
     */
    public void updateInfo(RackUpdateRequest request, String updatedByName) {
        this.rackName = request.rackName();
        this.groupNumber = request.groupNumber();
        this.rackLocation = request.rackLocation();
        this.totalUnits = request.totalUnits();
        this.doorDirection = request.doorDirection();
        this.zoneDirection = request.zoneDirection();
        this.width = request.width();
        this.depth = request.depth();
        this.height = request.height();
        this.department = request.department();
        this.maxPowerCapacity = request.maxPowerCapacity();
        this.maxWeightCapacity = request.maxWeightCapacity();
        this.manufacturer = request.manufacturer();
        this.serialNumber = request.serialNumber();
        this.managementNumber = request.managementNumber();
        this.status = request.status();
        this.rackType = request.rackType();
        this.colorCode = request.colorCode();
        this.notes = request.notes();
        this.managerId = request.managerId();
        this.updatedBy = updatedByName;

        // 사용 가능한 유닛 수 재계산
        this.availableUnits = this.totalUnits - this.usedUnits;
    }

    /**
     * 장비 추가 시 유닛 사용
     */
    public void addEquipment(Integer unitSize) {
        this.usedUnits += unitSize;
        this.availableUnits = this.totalUnits - this.usedUnits;
    }

    /**
     * 장비 제거 시 유닛 해제
     */
    public void removeEquipment(Integer unitSize) {
        this.usedUnits -= unitSize;
        this.availableUnits = this.totalUnits - this.usedUnits;
    }

    /**
     * 전력 사용량 업데이트
     */
    public void updatePowerUsage(BigDecimal powerChange) {
        this.currentPowerUsage = this.currentPowerUsage.add(powerChange);
    }

    /**
     * 무게 업데이트
     */
    public void updateWeight(BigDecimal weightChange) {
        this.currentWeight = this.currentWeight.add(weightChange);
    }

    /**
     * 전력 사용률 계산 (%)
     */
    public BigDecimal getPowerUsageRate() {
        if (maxPowerCapacity == null || maxPowerCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPowerUsage
                .divide(maxPowerCapacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 공간 사용률 계산 (%)
     */
    public BigDecimal getSpaceUsageRate() {
        if (totalUnits == null || totalUnits == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(usedUnits)
                .divide(BigDecimal.valueOf(totalUnits), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.status = RackStatus.INACTIVE;
    }
}