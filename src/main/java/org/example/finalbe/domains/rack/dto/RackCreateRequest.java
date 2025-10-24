package org.example.finalbe.domains.rack.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;

/**
 * 랙 생성 요청 DTO
 *
 * 수정사항:
 * - department String 필드 제거
 * - 부서는 RackDepartment를 통해 별도로 매핑
 */
@Builder
public record RackCreateRequest(
        @NotBlank(message = "랙 이름을 입력해주세요.")
        @Size(max = 100, message = "랙 이름은 100자를 초과할 수 없습니다.")
        String rackName,

        @Size(max = 50, message = "그룹 번호는 50자를 초과할 수 없습니다.")
        String groupNumber,

        @Size(max = 100, message = "랙 위치는 100자를 초과할 수 없습니다.")
        String rackLocation,

        @Min(value = 1, message = "전체 유닛 수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "전체 유닛 수는 100을 초과할 수 없습니다.")
        Integer totalUnits,

        DoorDirection doorDirection,

        ZoneDirection zoneDirection,

        @DecimalMin(value = "0.0", inclusive = false, message = "너비는 0보다 커야 합니다.")
        BigDecimal width,

        @DecimalMin(value = "0.0", inclusive = false, message = "깊이는 0보다 커야 합니다.")
        BigDecimal depth,

        @DecimalMin(value = "0.0", message = "최대 전력 용량은 0 이상이어야 합니다.")
        BigDecimal maxPowerCapacity,

        @DecimalMin(value = "0.0", message = "최대 중량 용량은 0 이상이어야 합니다.")
        BigDecimal maxWeightCapacity,

        @Size(max = 100, message = "제조사명은 100자를 초과할 수 없습니다.")
        String manufacturer,

        @Size(max = 100, message = "시리얼 번호는 100자를 초과할 수 없습니다.")
        String serialNumber,

        @Size(max = 100, message = "관리 번호는 100자를 초과할 수 없습니다.")
        String managementNumber,

        RackStatus status,

        RackType rackType,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
                message = "색상 코드는 올바른 HEX 형식이어야 합니다. (예: #FF5733)")
        String colorCode,

        @Size(max = 1000, message = "비고는 1000자를 초과할 수 없습니다.")
        String notes,

        Long managerId,

        @NotNull(message = "전산실을 선택해주세요.")
        @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.")
        Long datacenterId
) {
    /**
     * 엔티티 변환 메서드
     * Request DTO의 일관된 패턴
     */
    public Rack toEntity(DataCenter datacenter, String createdBy) {
        return Rack.builder()
                .rackName(this.rackName)
                .groupNumber(this.groupNumber)
                .rackLocation(this.rackLocation)
                .totalUnits(this.totalUnits != null ? this.totalUnits : 42)
                .usedUnits(0)
                .availableUnits(this.totalUnits != null ? this.totalUnits : 42)
                .doorDirection(this.doorDirection != null ? this.doorDirection : DoorDirection.FRONT)
                .zoneDirection(this.zoneDirection != null ? this.zoneDirection : ZoneDirection.EAST)
                .width(this.width)
                .depth(this.depth)
                .maxPowerCapacity(this.maxPowerCapacity)
                .currentPowerUsage(BigDecimal.ZERO)
                .measuredPower(BigDecimal.ZERO)
                .maxWeightCapacity(this.maxWeightCapacity)
                .currentWeight(BigDecimal.ZERO)
                .manufacturer(this.manufacturer)
                .serialNumber(this.serialNumber)
                .managementNumber(this.managementNumber)
                .status(this.status != null ? this.status : RackStatus.ACTIVE)
                .rackType(this.rackType != null ? this.rackType : RackType.STANDARD)
                .colorCode(this.colorCode)
                .notes(this.notes)
                .managerId(this.managerId)
                .datacenter(datacenter)
                .createdBy(createdBy)
                .build();
    }
}