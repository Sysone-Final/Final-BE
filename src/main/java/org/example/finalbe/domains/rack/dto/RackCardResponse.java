package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;

/**
 * 랙 카드 뷰 응답 DTO
 */
@Builder
public record RackCardResponse(
        Long id,
        String rackName,
        String rackLocation,
        RackStatus status,
        BigDecimal usageRate,
        BigDecimal powerUsageRate,
        Integer usedUnits,
        Integer totalUnits,
        BigDecimal currentPowerUsage,
        BigDecimal maxPowerCapacity,
        BigDecimal temperature,
        Long managerId,
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
                .temperature(null)
                .managerId(rack.getManagerId())
                .department(rack.getDepartment())
                .build();
    }
}