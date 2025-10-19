package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.rack.domain.Rack;

@Builder
public record RackCreateRequest(
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
        String managerId,
        Long datacenterId
) {
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
                .department(this.department)
                .maxPowerCapacity(this.maxPowerCapacity)
                .currentPowerUsage(0.0)
                .measuredPower(0.0)
                .maxWeightCapacity(this.maxWeightCapacity)
                .currentWeight(0.0)
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