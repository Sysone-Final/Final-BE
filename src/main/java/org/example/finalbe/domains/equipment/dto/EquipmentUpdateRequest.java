package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;

import java.time.LocalDate;

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
        Double powerConsumption,
        Double weight,
        String imageUrl,
        LocalDate installationDate,
        String notes
) {
}