package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EquipmentPositionType;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.util.List;

/**
 * 랙 실장도 응답 DTO
 */
@Builder
public record RackElevationResponse(
        Long rackId,
        String rackName,
        Integer totalUnits,
        Integer usedUnits,
        BigDecimal usageRate,
        BigDecimal powerUsageRate,
        String view,
        List<UnitSlot> units
) {
    public static RackElevationResponse from(Rack rack, List<Equipment> equipments, String view) {
        List<UnitSlot> units = new java.util.ArrayList<>();

        for (int i = rack.getTotalUnits(); i >= 1; i--) {
            final int unitNumber = i;

            Equipment equipment = equipments.stream()
                    .filter(eq -> unitNumber >= eq.getStartUnit()
                            && unitNumber < eq.getStartUnit() + eq.getUnitSize())
                    .findFirst()
                    .orElse(null);

            if (equipment != null && unitNumber == equipment.getStartUnit()) {
                units.add(UnitSlot.builder()
                        .unitNumber(unitNumber)
                        .isEmpty(false)
                        .equipmentId(equipment.getId())
                        .equipmentName(equipment.getName())
                        .equipmentType(equipment.getType())
                        .unitSize(equipment.getUnitSize())
                        .positionType(equipment.getPositionType())
                        .status(equipment.getStatus())
                        .powerConsumption(equipment.getPowerConsumption())
                        .build());
            } else if (equipment != null) {
                continue;
            } else {
                units.add(UnitSlot.builder()
                        .unitNumber(unitNumber)
                        .isEmpty(true)
                        .build());
            }
        }

        return RackElevationResponse.builder()
                .rackId(rack.getId())
                .rackName(rack.getRackName())
                .totalUnits(rack.getTotalUnits())
                .usedUnits(rack.getUsedUnits())
                .usageRate(rack.getUsageRate())
                .powerUsageRate(rack.getPowerUsageRate())
                .view(view)
                .units(units)
                .build();
    }

    @Builder
    public record UnitSlot(
            Integer unitNumber,
            Boolean isEmpty,
            Long equipmentId,
            String equipmentName,
            EquipmentType equipmentType,
            Integer unitSize,
            EquipmentPositionType positionType,
            EquipmentStatus status,
            BigDecimal powerConsumption
    ) {
    }
}