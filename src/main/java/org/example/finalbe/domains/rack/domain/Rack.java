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

    @Column(name = "width", precision = 10, scale = 2)
    private BigDecimal width; // 폭

    @Column(name = "depth", precision = 10, scale = 2)
    private BigDecimal depth; // 깊이

    @Column(name = "department", length = 100)
    private String department; // 부서

    @Column(name = "max_power_capacity", precision = 10, scale = 2)
    private BigDecimal maxPowerCapacity; // 최대 전력 용량

    @Column(name = "current_power_usage", precision = 10, scale = 2)
    private BigDecimal currentPowerUsage; // 현재 전력 사용량

    @Column(name = "measured_power", precision = 10, scale = 2)
    private BigDecimal measuredPower; // 실측 전력

    @Column(name = "max_weight_capacity", precision = 10, scale = 2)
    private BigDecimal maxWeightCapacity; // 최대 무게 용량

    @Column(name = "current_weight", precision = 10, scale = 2)
    private BigDecimal currentWeight; // 현재 무게

    @Column(name = "manufacturer", length = 100)
    private String manufacturer; // 제조사

    @Column(name = "serial_number", length = 100)
    private String serialNumber; // 시리얼 번호

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

    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // 비고

    @Column(name = "manager_id")
    private Long managerId; // 담당자 ID

    // 서버실(전산실)과의 관계 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serverroom_id", nullable = false)
    private ServerRoom serverroom; // 서버실(전산실)

    /**
     * 랙 정보 수정
     */
    public void updateInfo(RackUpdateRequest request) {
        this.rackName = request.rackName();
        this.groupNumber = request.groupNumber();
        this.rackLocation = request.rackLocation();
        this.totalUnits = request.totalUnits();
        this.usedUnits = request.usedUnits();
        this.availableUnits = request.availableUnits();
        this.doorDirection = request.doorDirection();
        this.zoneDirection = request.zoneDirection();
        this.width = request.width();
        this.depth = request.depth();
        this.department = request.department();
        this.maxPowerCapacity = request.maxPowerCapacity();
        this.currentPowerUsage = request.currentPowerUsage();
        this.measuredPower = request.measuredPower();
        this.maxWeightCapacity = request.maxWeightCapacity();
        this.currentWeight = request.currentWeight();
        this.manufacturer = request.manufacturer();
        this.serialNumber = request.serialNumber();
        this.managementNumber = request.managementNumber();
        this.status = request.status();
        this.rackType = request.rackType();
        this.colorCode = request.colorCode();
        this.notes = request.notes();
        this.managerId = request.managerId();
    }

    /**
     * 유닛 사용률 계산
     */
    public double getUsageRate() {
        if (totalUnits == null || totalUnits == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(usedUnits)
                .divide(BigDecimal.valueOf(totalUnits), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * 전력 사용률 계산
     */
    public double getPowerUsageRate() {
        if (maxPowerCapacity == null || maxPowerCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return currentPowerUsage
                .divide(maxPowerCapacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * 무게 사용률 계산
     */
    public double getWeightUsageRate() {
        if (maxWeightCapacity == null || maxWeightCapacity.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return currentWeight
                .divide(maxWeightCapacity, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
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
        this.currentPowerUsage = this.currentPowerUsage.add(power);
    }

    /**
     * 전력 사용량 차감
     */
    public void subtractPowerUsage(BigDecimal power) {
        this.currentPowerUsage = this.currentPowerUsage.subtract(power);
        if (this.currentPowerUsage.compareTo(BigDecimal.ZERO) < 0) {
            this.currentPowerUsage = BigDecimal.ZERO;
        }
    }

    /**
     * 무게 추가
     */
    public void addWeight(BigDecimal weight) {
        this.currentWeight = this.currentWeight.add(weight);
    }

    /**
     * 무게 차감
     */
    public void subtractWeight(BigDecimal weight) {
        this.currentWeight = this.currentWeight.subtract(weight);
        if (this.currentWeight.compareTo(BigDecimal.ZERO) < 0) {
            this.currentWeight = BigDecimal.ZERO;
        }
    }
}