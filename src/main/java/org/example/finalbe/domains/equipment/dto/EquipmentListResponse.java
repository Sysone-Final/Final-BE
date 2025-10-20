package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;
import org.example.finalbe.domains.equipment.domain.Equipment;

@Builder
public record EquipmentListResponse(
        Long equipmentId,
        String equipmentName,
        String equipmentCode,
        String equipmentType,
        String status,
        Integer startUnit,
        Integer unitSize,
        String rackName,
        String modelName,
        String manufacturer,
        String ipAddress,
        Double powerConsumption,
        String imageUrl
) {
    public static EquipmentListResponse from(Equipment equipment) {
        return EquipmentListResponse.builder()
                .equipmentId(equipment.getId())
                .equipmentName(equipment.getName())
                .equipmentCode(equipment.getCode())
                .equipmentType(equipment.getType() != null ? equipment.getType().name() : null)
                .status(equipment.getStatus() != null ? equipment.getStatus().name() : null)
                .startUnit(equipment.getStartUnit())
                .unitSize(equipment.getUnitSize())
                .rackName(equipment.getRack() != null ? equipment.getRack().getRackName() : null)
                .modelName(equipment.getModelName())
                .manufacturer(equipment.getManufacturer())
                .ipAddress(equipment.getIpAddress())
                .powerConsumption(equipment.getPowerConsumption())
                .imageUrl(equipment.getImageUrl())
                .build();
    }
}