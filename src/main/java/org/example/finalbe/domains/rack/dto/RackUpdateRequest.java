package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;

import java.math.BigDecimal;

/**
 * 랙 수정 요청 DTO
 */
@Builder
public record RackUpdateRequest(
        String rackName,
        BigDecimal gridX,
        BigDecimal gridY,
        Integer totalUnits,
        DoorDirection doorDirection,
        ZoneDirection zoneDirection,
        BigDecimal maxPowerCapacity,
        String manufacturer,
        String serialNumber,
        RackStatus status,
        RackType rackType,
        String notes
) {
}