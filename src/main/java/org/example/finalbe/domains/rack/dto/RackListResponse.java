package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;

@Builder
public record RackListResponse(
        Long id,
        String rackName,
        String groupNumber,
        String rackLocation,
        Integer totalUnits,
        Integer usedUnits,
        Integer availableUnits,
        RackStatus status,
        BigDecimal usageRate,
        BigDecimal powerUsageRate,
        BigDecimal currentPowerUsage,
        BigDecimal maxPowerCapacity,
        String department,
        Long managerId
) {
    public static RackListResponse from(Rack rack) {
        return RackListResponse.builder()
                .id(rack.getId())
                .rackName(rack.getRackName())
                .groupNumber(rack.getGroupNumber())
                .rackLocation(rack.getRackLocation())
                .totalUnits(rack.getTotalUnits())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .status(rack.getStatus())
                .usageRate(rack.getUsageRate())
                .powerUsageRate(rack.getPowerUsageRate())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .department(rack.getDepartment())
                .managerId(rack.getManagerId())
                .build();
    }
}