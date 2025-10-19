package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.rack.domain.Rack;

import java.time.LocalDateTime;

@Builder
public record RackDetailResponse(
        Long id,
        String rackName,
        String groupNumber,
        String rackLocation,
        Integer totalUnits,
        Integer usedUnits,
        Integer availableUnits,
        DoorDirection doorDirection,
        ZoneDirection zoneDirection,
        Double width,
        Double depth,
        Double height,
        String department,
        Double maxPowerCapacity,
        Double currentPowerUsage,
        Double measuredPower,
        Double maxWeightCapacity,
        Double currentWeight,
        String manufacturer,
        String serialNumber,
        String managementNumber,
        RackStatus status,
        RackType rackType,
        String colorCode,
        String notes,
        String managerId,
        Long datacenterId,
        String datacenterName,
        Double usageRate,
        Double powerUsageRate,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public static RackDetailResponse from(Rack rack) {
        return RackDetailResponse.builder()
                .id(rack.getId())
                .rackName(rack.getRackName())
                .groupNumber(rack.getGroupNumber())
                .rackLocation(rack.getRackLocation())
                .totalUnits(rack.getTotalUnits())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .doorDirection(rack.getDoorDirection())
                .zoneDirection(rack.getZoneDirection())
                .width(rack.getWidth())
                .depth(rack.getDepth())
                .department(rack.getDepartment())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .measuredPower(rack.getMeasuredPower())
                .maxWeightCapacity(rack.getMaxWeightCapacity())
                .currentWeight(rack.getCurrentWeight())
                .manufacturer(rack.getManufacturer())
                .serialNumber(rack.getSerialNumber())
                .managementNumber(rack.getManagementNumber())
                .status(rack.getStatus())
                .rackType(rack.getRackType())
                .colorCode(rack.getColorCode())
                .notes(rack.getNotes())
                .managerId(rack.getManagerId())
                .datacenterId(rack.getDatacenter().getId())
                .datacenterName(rack.getDatacenter().getName())
                .usageRate(rack.getUsageRate())
                .powerUsageRate(rack.getPowerUsageRate())
                .createdBy(rack.getCreatedBy())
                .createdAt(rack.getCreatedAt())
                .updatedBy(rack.getUpdatedBy())
                .updatedAt(rack.getUpdatedAt())
                .build();
    }
}