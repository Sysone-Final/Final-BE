package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;
import org.example.finalbe.domains.equipment.domain.Equipment;

import java.math.BigDecimal;

/**
 * 장비 목록 조회 응답 DTO
 */
@Builder
public record EquipmentListResponse(
        Long id,
        String equipmentName,
        String equipmentCode,
        String equipmentType,
        String status,
        Integer startUnit,
        Integer unitSize,
        String modelName,
        String manufacturer,
        String ipAddress,
        String positionType,
        BigDecimal powerConsumption
) {
    public static EquipmentListResponse from(Equipment equipment) {
        return EquipmentListResponse.builder()
                .id(equipment.getId())
                .equipmentName(equipment.getName())
                .equipmentCode(equipment.getCode())
                .equipmentType(equipment.getType() != null ? equipment.getType().name() : null)
                .status(equipment.getStatus() != null ? equipment.getStatus().name() : null)
                .startUnit(equipment.getStartUnit())
                .unitSize(equipment.getUnitSize())
                .modelName(equipment.getModelName())
                .manufacturer(equipment.getManufacturer())
                .ipAddress(equipment.getIpAddress())
                .positionType(equipment.getPositionType() != null ? equipment.getPositionType().name() : null)
                .powerConsumption(equipment.getPowerConsumption())
                .build();
    }
}