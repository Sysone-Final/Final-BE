package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

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
        BigDecimal availablePowerCapacity,
        BigDecimal weightUsageRate,
        BigDecimal currentWeight,
        BigDecimal maxWeightCapacity,
        BigDecimal availableWeightCapacity
) {
    public static RackUtilizationResponse from(Rack rack) {
        BigDecimal availablePower = Optional.ofNullable(rack.getMaxPowerCapacity())
                .flatMap(max -> Optional.ofNullable(rack.getCurrentPowerUsage())
                        .map(max::subtract))
                .orElse(null);

        BigDecimal availableWeight = Optional.ofNullable(rack.getMaxWeightCapacity())
                .flatMap(max -> Optional.ofNullable(rack.getCurrentWeight())
                        .map(max::subtract))
                .orElse(null);

        BigDecimal weightUsageRate = Optional.ofNullable(rack.getMaxWeightCapacity())
                .filter(max -> max.compareTo(BigDecimal.ZERO) > 0)
                .flatMap(max -> Optional.ofNullable(rack.getCurrentWeight())
                        .map(current -> current
                                .divide(max, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP)))
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
                .weightUsageRate(weightUsageRate)
                .currentWeight(rack.getCurrentWeight())
                .maxWeightCapacity(rack.getMaxWeightCapacity())
                .availableWeightCapacity(availableWeight)
                .build();
    }
}