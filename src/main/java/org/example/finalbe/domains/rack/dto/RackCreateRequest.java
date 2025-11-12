package org.example.finalbe.domains.rack.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;

/**
 * 랙 생성 요청 DTO
 */
@Builder
public record RackCreateRequest(
        @NotBlank(message = "랙 이름을 입력해주세요.")
        @Size(max = 100, message = "랙 이름은 100자를 초과할 수 없습니다.")
        String rackName,

        @DecimalMin(value = "0.0", message = "X 좌표는 0 이상이어야 합니다.")
        BigDecimal gridX,

        @DecimalMin(value = "0.0", message = "Y 좌표는 0 이상이어야 합니다.")
        BigDecimal gridY,

        @Min(value = 1, message = "전체 유닛 수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "전체 유닛 수는 100을 초과할 수 없습니다.")
        Integer totalUnits,

        DoorDirection doorDirection,

        ZoneDirection zoneDirection,

        @DecimalMin(value = "0.0", message = "최대 전력 용량은 0 이상이어야 합니다.")
        BigDecimal maxPowerCapacity,

        @Size(max = 100, message = "제조사명은 100자를 초과할 수 없습니다.")
        String manufacturer,

        @Size(max = 100, message = "시리얼 번호는 100자를 초과할 수 없습니다.")
        String serialNumber,

        RackStatus status,

        RackType rackType,

        @Size(max = 1000, message = "비고는 1000자를 초과할 수 없습니다.")
        String notes,

        @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.")
        Long serverRoomId
) {
    public Rack toEntity(ServerRoom serverRoom) {
        return Rack.builder()
                .rackName(this.rackName)
                .gridX(this.gridX)
                .gridY(this.gridY)
                .totalUnits(this.totalUnits != null ? this.totalUnits : 42)
                .usedUnits(0)
                .availableUnits(this.totalUnits != null ? this.totalUnits : 42)
                .doorDirection(this.doorDirection != null ? this.doorDirection : DoorDirection.FRONT)
                .zoneDirection(this.zoneDirection != null ? this.zoneDirection : ZoneDirection.EAST)
                .maxPowerCapacity(this.maxPowerCapacity)
                .currentPowerUsage(BigDecimal.ZERO)
                .measuredPower(BigDecimal.ZERO)
                .manufacturer(this.manufacturer)
                .serialNumber(this.serialNumber)
                .status(this.status != null ? this.status : RackStatus.ACTIVE)
                .rackType(this.rackType != null ? this.rackType : RackType.STANDARD)
                .notes(this.notes)
                .serverRoom(serverRoom)
                .build();
    }
}