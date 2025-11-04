package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 장비 수정 요청 DTO
 */
@Builder
public record EquipmentUpdateRequest(
        String equipmentName,
        String equipmentCode,
        String equipmentType,
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
        LocalDate installationDate,
        String notes
) {
}