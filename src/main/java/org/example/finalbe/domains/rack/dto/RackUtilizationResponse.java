package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.rack.domain.Rack;

@Builder
public record RackUtilizationResponse(
        Long rackId,
        String rackName,
        Double usageRate,
        Integer usedUnits,
        Integer availableUnits,
        Integer totalUnits,
        Double powerUsageRate,
        Double currentPowerUsage,
        Double maxPowerCapacity,
        Double availablePowerCapacity,
        Double weightUsageRate,
        Double currentWeight,
        Double maxWeightCapacity,
        Double availableWeightCapacity
) {
    public static RackUtilizationResponse from(Rack rack) {
        Double availablePower = rack.getMaxPowerCapacity() != null
                ? rack.getMaxPowerCapacity() - rack.getCurrentPowerUsage()
                : null;

        Double availableWeight = rack.getMaxWeightCapacity() != null
                ? rack.getMaxWeightCapacity() - rack.getCurrentWeight()
                : null;

        Double weightUsageRate = null;
        if (rack.getMaxWeightCapacity() != null && rack.getMaxWeightCapacity() > 0) {
            weightUsageRate = (rack.getCurrentWeight() / rack.getMaxWeightCapacity()) * 100;
        }

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