package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.rack.domain.Rack;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record RackElevationResponse(
        Long rackId,
        String rackName,
        Integer totalUnits,
        Integer usedUnits,
        Double usageRate,
        Double powerUsageRate,
        String view,
        List<UnitSlot> units
) {
    public static RackElevationResponse from(Rack rack, List<Equipment> equipments, String view) {
        // 각 유닛별 슬롯 생성
        List<UnitSlot> units = new java.util.ArrayList<>();

        for (int i = rack.getTotalUnits(); i >= 1; i--) {
            final int unitNumber = i;

            // 해당 유닛에 장비가 있는지 확인
            Equipment equipment = equipments.stream()
                    .filter(eq -> unitNumber >= eq.getStartUnit()
                            && unitNumber < eq.getStartUnit() + eq.getUnitSize())
                    .findFirst()
                    .orElse(null);

            if (equipment != null && unitNumber == equipment.getStartUnit()) {
                // 장비의 시작 유닛
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
                // 장비가 차지하는 중간/끝 유닛 (표시하지 않음)
                continue;
            } else {
                // 빈 유닛
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
            org.example.finalbe.domains.common.enumdir.EquipmentType equipmentType,
            Integer unitSize,
            org.example.finalbe.domains.common.enumdir.PositionType positionType,
            org.example.finalbe.domains.common.enumdir.EquipmentStatus status,
            Double powerConsumption
    ) {
    }
}