package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;

@Builder
public record RackUpdateRequest(
        String rackName,
        String groupNumber,
        String rackLocation,
        Integer totalUnits,
        DoorDirection doorDirection,
        ZoneDirection zoneDirection,
        Double width,
        Double depth,
        Double height,
        String department,
        Double maxPowerCapacity,
        Double maxWeightCapacity,
        String manufacturer,
        String serialNumber,
        String managementNumber,
        RackStatus status,
        RackType rackType,
        String colorCode,
        String notes,
        String managerId
) {
}