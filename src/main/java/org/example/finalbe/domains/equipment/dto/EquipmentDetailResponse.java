/**
 * 작성자: 황요한
 * 장비 상세 조회 응답 DTO
 */
package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;
import org.example.finalbe.domains.equipment.domain.Equipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record EquipmentDetailResponse(
        Long id,
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
        Long rackId,
        String rackName,
        Long serverRoomId,
        Boolean monitoringEnabled,
        Integer cpuThresholdWarning,
        Integer cpuThresholdCritical,
        Integer memoryThresholdWarning,
        Integer memoryThresholdCritical,
        Integer diskThresholdWarning,
        Integer diskThresholdCritical
) {
    public static EquipmentDetailResponse from(Equipment equipment) {
        return EquipmentDetailResponse.builder()
                .id(equipment.getId())
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
                .status(equipment.getStatus() != null ? equipment.getStatus().name() : null)
                .installationDate(equipment.getInstallationDate())
                .notes(equipment.getNotes())
                .createdAt(equipment.getCreatedAt())
                .updatedAt(equipment.getUpdatedAt())
                .rackId(equipment.getRack() != null ? equipment.getRack().getId() : null)
                .rackName(equipment.getRack() != null ? equipment.getRack().getRackName() : null)
                .serverRoomId(equipment.getRack() != null && equipment.getRack().getServerRoom() != null
                        ? equipment.getRack().getServerRoom().getId() : null)
                .monitoringEnabled(equipment.getMonitoringEnabled())
                .cpuThresholdWarning(equipment.getCpuThresholdWarning())
                .cpuThresholdCritical(equipment.getCpuThresholdCritical())
                .memoryThresholdWarning(equipment.getMemoryThresholdWarning())
                .memoryThresholdCritical(equipment.getMemoryThresholdCritical())
                .diskThresholdWarning(equipment.getDiskThresholdWarning())
                .diskThresholdCritical(equipment.getDiskThresholdCritical())
                .build();
    }
}