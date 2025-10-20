package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;

import java.math.BigDecimal;

@Builder
public record RackUpdateRequest(
        String rackName,
        String groupNumber,
        String rackLocation,
        Integer totalUnits,
        DoorDirection doorDirection,
        ZoneDirection zoneDirection,
        BigDecimal width,
        BigDecimal depth,
        BigDecimal height,
        String department,
        BigDecimal maxPowerCapacity,
        BigDecimal maxWeightCapacity,
        String manufacturer,
        String serialNumber,
        String managementNumber,
        RackStatus status,
        RackType rackType,
        String colorCode,
        String notes,
        Long managerId
) {
}