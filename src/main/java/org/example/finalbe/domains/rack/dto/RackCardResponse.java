package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.rack.domain.Rack;

@Builder
public record RackCardResponse(
        Long id,
        String rackName,
        String rackLocation,
        RackStatus status,
        Double usageRate,
        Double powerUsageRate,
        Integer usedUnits,
        Integer totalUnits,
        Double currentPowerUsage,
        Double maxPowerCapacity,
        Double temperature,
        String managerId,
        String department
) {
    public static RackCardResponse from(Rack rack) {
        return RackCardResponse.builder()
                .id(rack.getId())
                .rackName(rack.getRackName())
                .rackLocation(rack.getRackLocation())
                .status(rack.getStatus())
                .usageRate(rack.getUsageRate())
                .powerUsageRate(rack.getPowerUsageRate())
                .usedUnits(rack.getUsedUnits())
                .totalUnits(rack.getTotalUnits())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .temperature(null) // TODO: 모니터링 데이터 연동
                .managerId(rack.getManagerId())
                .department(rack.getDepartment())
                .build();
    }
}