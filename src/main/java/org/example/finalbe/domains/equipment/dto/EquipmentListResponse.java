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
        Long companyId,
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
        BigDecimal powerConsumption,
        Long rackId,
        String rackName,

        // ===== 임계치 필드 =====
        Boolean monitoringEnabled,
        Integer cpuThresholdWarning,
        Integer cpuThresholdCritical,
        Integer memoryThresholdWarning,
        Integer memoryThresholdCritical,
        Integer diskThresholdWarning,
        Integer diskThresholdCritical
) {
    public static EquipmentListResponse from(Equipment equipment) {
        return EquipmentListResponse.builder()
                .id(equipment.getId())
                .companyId(equipment.getCompanyId())
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
                .rackId(equipment.getRack() != null ? equipment.getRack().getId() : null)
                .rackName(equipment.getRack() != null ? equipment.getRack().getRackName() : null)

                // ===== 임계치 값 매핑 =====
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