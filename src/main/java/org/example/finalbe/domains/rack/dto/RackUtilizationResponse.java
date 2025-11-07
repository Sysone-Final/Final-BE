package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 랙 사용률 응답 DTO
 */
@Builder
public record RackUtilizationResponse(
        Long rackId,
        String rackName,
        BigDecimal usageRate,
        Integer usedUnits,
        Integer availableUnits,
        Integer totalUnits,
        BigDecimal powerUsageRate,
        BigDecimal currentPowerUsage,
        BigDecimal maxPowerCapacity,
        BigDecimal availablePowerCapacity
) {
    public static RackUtilizationResponse from(Rack rack) {
        BigDecimal availablePower = Optional.ofNullable(rack.getMaxPowerCapacity())
                .flatMap(max -> Optional.ofNullable(rack.getCurrentPowerUsage())
                        .map(max::subtract))
                .orElse(null);

        return RackUtilizationResponse.builder()
                .rackId(rack.getId())
                .rackName(rack.getRackName())
                .usageRate(rack.getUsageRate())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .totalUnits(rack.getTotalUnits())
                .powerUsageRate(rack.getPowerUsageRate())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .availablePowerCapacity(availablePower)
                .build();
    }
}