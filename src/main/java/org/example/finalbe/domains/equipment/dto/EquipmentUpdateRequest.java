/**
 * 작성자: 황요한
 * 장비 수정 요청 DTO
 */
package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record EquipmentUpdateRequest(
        String equipmentName,
        String equipmentCode,
        String equipmentType,
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
        String status,
        LocalDate installationDate,
        String notes,
        Integer startUnit,

        Boolean monitoringEnabled,
        Integer cpuThresholdWarning,
        Integer cpuThresholdCritical,
        Integer memoryThresholdWarning,
        Integer memoryThresholdCritical,
        Integer diskThresholdWarning,
        Integer diskThresholdCritical
) {
}