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
        @Size(max = 200, message = "장비명은 200자를 초과할 수 없습니다.")
        String equipmentName,

        @Size(max = 100, message = "장비 코드는 100자를 초과할 수 없습니다.")
        String equipmentCode,

        String equipmentType,

        @NotNull(message = "시작 유닛을 입력해주세요.")
        @Min(value = 1, message = "시작 유닛은 1 이상이어야 합니다.")
        @Max(value = 100, message = "시작 유닛은 100을 초과할 수 없습니다.")
        Integer startUnit,

        @NotNull(message = "유닛 크기를 입력해주세요.")
        @Min(value = 1, message = "유닛 크기는 1 이상이어야 합니다.")
        @Max(value = 50, message = "유닛 크기는 50을 초과할 수 없습니다.")
        Integer unitSize,

        String positionType,

        @Size(max = 200, message = "모델명은 200자를 초과할 수 없습니다.")
        String modelName,

        @Size(max = 100, message = "제조사명은 100자를 초과할 수 없습니다.")
        String manufacturer,

        @Size(max = 100, message = "시리얼 번호는 100자를 초과할 수 없습니다.")
        String serialNumber,

        @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
                message = "올바른 IP 주소 형식이 아닙니다.")
        String ipAddress,

        @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$",
                message = "올바른 MAC 주소 형식이 아닙니다.")
        String macAddress,

        @Size(max = 100, message = "운영체제는 100자를 초과할 수 없습니다.")
        String os,

        @Size(max = 200, message = "CPU 사양은 200자를 초과할 수 없습니다.")
        String cpuSpec,

        @Size(max = 200, message = "메모리 사양은 200자를 초과할 수 없습니다.")
        String memorySpec,

        @Size(max = 200, message = "디스크 사양은 200자를 초과할 수 없습니다.")
        String diskSpec,

        @DecimalMin(value = "0.0", message = "전력 소비량은 0 이상이어야 합니다.")
        BigDecimal powerConsumption,

        @DecimalMin(value = "0.0", message = "무게는 0 이상이어야 합니다.")
        BigDecimal weight,

        String status,

        String imageUrl,

        @PastOrPresent(message = "설치일은 미래 날짜일 수 없습니다.")
        LocalDate installationDate,

        @Size(max = 1000, message = "비고는 1000자를 초과할 수 없습니다.")
        String notes,

        @NotNull(message = "랙을 선택해주세요.")
        @Min(value = 1, message = "유효하지 않은 랙 ID입니다.")
        Long rackId,

        Integer position,

        Integer height
) {
    /**
     * DTO를 Entity로 변환
     */
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