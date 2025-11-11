package org.example.finalbe.domains.equipment.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EquipmentPositionType;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 장비 생성 요청 DTO
 */
@Builder
public record EquipmentCreateRequest(
        @NotBlank(message = "장비명을 입력해주세요.")
        String equipmentName,

        String equipmentCode,
        String equipmentType,

        @NotNull(message = "시작 유닛을 입력해주세요.")
        @Min(value = 1, message = "시작 유닛은 1 이상이어야 합니다.")
        Integer startUnit,

        @NotNull(message = "유닛 크기를 입력해주세요.")
        @Min(value = 1, message = "유닛 크기는 1 이상이어야 합니다.")
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

        @DecimalMin(value = "0.0", message = "전력 소비량은 0 이상이어야 합니다.")
        BigDecimal powerConsumption,


        String status,
        LocalDate installationDate,
        String notes,

        @NotNull(message = "랙을 선택해주세요.")
        Long rackId,

        // 모니터링 설정
        Boolean monitoringEnabled,
        Integer cpuThresholdWarning,
        Integer cpuThresholdCritical,
        Integer memoryThresholdWarning,
        Integer memoryThresholdCritical,
        Integer diskThresholdWarning,
        Integer diskThresholdCritical
) {
    public Equipment toEntity(Rack rack) {
        return Equipment.builder()
                .name(equipmentName)
                .code(equipmentCode)
                .type(equipmentType != null ? EquipmentType.valueOf(equipmentType) : null)
                .startUnit(startUnit)
                .unitSize(unitSize)
                .positionType(positionType != null ? EquipmentPositionType.valueOf(positionType) : EquipmentPositionType.FRONT)
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
                .status(status != null ? EquipmentStatus.valueOf(status) : EquipmentStatus.NORMAL)
                .installationDate(installationDate)
                .notes(notes)
                .rack(rack)
                .monitoringEnabled(monitoringEnabled != null ? monitoringEnabled : false)
                .cpuThresholdWarning(cpuThresholdWarning)
                .cpuThresholdCritical(cpuThresholdCritical)
                .memoryThresholdWarning(memoryThresholdWarning)
                .memoryThresholdCritical(memoryThresholdCritical)
                .diskThresholdWarning(diskThresholdWarning)
                .diskThresholdCritical(diskThresholdCritical)
                .build();
    }
}