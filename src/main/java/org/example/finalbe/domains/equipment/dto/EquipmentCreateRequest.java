package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EquipmentPositionType;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.rack.domain.Rack;

import java.time.LocalDate;

@Builder
public record EquipmentCreateRequest(
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
        Double powerConsumption,
        Double weight,
        String status,
        String imageUrl,
        LocalDate installationDate,
        String notes,
        Long rackId,
        Integer position,
        Integer height
) {
    public Equipment toEntity(Rack rack, Long managerId) {
        return Equipment.builder()
                .name(equipmentName)
                .code(equipmentCode)
                .type(equipmentType != null ? EquipmentType.valueOf(equipmentType) : null)
                .startUnit(startUnit)
                .unitSize(unitSize)
                .positionType(positionType != null ? EquipmentPositionType.valueOf(positionType) : EquipmentPositionType.NORMAL)
                .modelName(modelName)
                .manufacturer(manufacturer)
                .serialNumber(serialNumber)
                .ipAddress(ipAddress)
                .macAddress(macAddress)
                .os(os)
                .cpuSpec(cpuSpec)
                .memorySpec(memorySpec)
                .diskSpec(diskSpec)
                .powerConsumption(powerConsumption)
                .weight(weight)
                .status(status != null ? EquipmentStatus.valueOf(status) : EquipmentStatus.NORMAL)
                .imageUrl(imageUrl)
                .installationDate(installationDate)
                .notes(notes)
                .managerId(managerId)
                .rack(rack)
                .position(position)
                .height(height)
                .build();
    }
}