package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;
import org.example.finalbe.domains.equipment.domain.Equipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 장비 상세 조회 응답 DTO
 */
@Builder
public record EquipmentDetailResponse(
        Long equipmentId,
        String equipmentName,
        String equipmentCode,
        String equipmentType,
        Integer startUnit,
        Integer unitSize,
        String positionType,
        String modelName,
        String manufacturer,
        String serialNumber,
        String ipAddress,
        String macAddress,
        String os,
        String cpuSpec,
        String memorySpec,
        String diskSpec,
        BigDecimal powerConsumption,
        BigDecimal weight,
        String status,
        LocalDate installationDate,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long managerId,
        Long rackId,
        String rackName,
        Integer position,
        Integer height
) {
    public static EquipmentDetailResponse from(Equipment equipment) {
        return EquipmentDetailResponse.builder()
                .equipmentId(equipment.getId())
                .equipmentName(equipment.getName())
                .equipmentCode(equipment.getCode())
                .equipmentType(equipment.getType() != null ? equipment.getType().name() : null)
                .startUnit(equipment.getStartUnit())
                .unitSize(equipment.getUnitSize())
                .positionType(equipment.getPositionType() != null ? equipment.getPositionType().name() : null)
                .modelName(equipment.getModelName())
                .manufacturer(equipment.getManufacturer())
                .serialNumber(equipment.getSerialNumber())
                .ipAddress(equipment.getIpAddress())
                .macAddress(equipment.getMacAddress())
                .os(equipment.getOs())
                .cpuSpec(equipment.getCpuSpec())
                .memorySpec(equipment.getMemorySpec())
                .diskSpec(equipment.getDiskSpec())
                .powerConsumption(equipment.getPowerConsumption())
                .weight(equipment.getWeight())
                .status(equipment.getStatus() != null ? equipment.getStatus().name() : null)
                .installationDate(equipment.getInstallationDate())
                .notes(equipment.getNotes())
                .createdAt(equipment.getCreatedAt())
                .updatedAt(equipment.getUpdatedAt())
                .managerId(equipment.getManagerId())
                .rackId(equipment.getRack() != null ? equipment.getRack().getId() : null)
                .rackName(equipment.getRack() != null ? equipment.getRack().getRackName() : null)
                .position(equipment.getPosition())
                .height(equipment.getHeight())
                .build();
    }
}