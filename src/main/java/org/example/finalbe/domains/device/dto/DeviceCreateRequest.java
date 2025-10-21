package org.example.finalbe.domains.device.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DeviceStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.device.domain.Device;

import org.example.finalbe.domains.device.domain.DeviceType;
import org.example.finalbe.domains.rack.domain.Rack;

import java.time.LocalDate;

@Builder
public record DeviceCreateRequest(
        @NotBlank(message = "장치명을 입력해주세요.")
        @Size(max = 100, message = "장치명은 100자를 초과할 수 없습니다.")
        String deviceName,

        @Size(max = 50, message = "장치 코드는 50자를 초과할 수 없습니다.")
        String deviceCode,

        @NotNull(message = "행 위치를 입력해주세요.")
        @Min(value = 0, message = "행 위치는 0 이상이어야 합니다.")
        Integer gridY,

        @NotNull(message = "열 위치를 입력해주세요.")
        @Min(value = 0, message = "열 위치는 0 이상이어야 합니다.")
        Integer gridX,

        @Min(value = 0, message = "Z축 위치는 0 이상이어야 합니다.")
        Integer gridZ,

        @Min(value = 0, message = "회전 각도는 0 이상이어야 합니다.")
        @Max(value = 360, message = "회전 각도는 360 이하여야 합니다.")
        Integer rotation,

        String status,

        @Size(max = 100, message = "모델명은 100자를 초과할 수 없습니다.")
        String modelName,

        @Size(max = 100, message = "제조사명은 100자를 초과할 수 없습니다.")
        String manufacturer,

        @Size(max = 100, message = "시리얼 번호는 100자를 초과할 수 없습니다.")
        String serialNumber,

        @PastOrPresent(message = "구매일은 미래 날짜일 수 없습니다.")
        LocalDate purchaseDate,

        LocalDate warrantyEndDate,

        @Size(max = 1000, message = "비고는 1000자를 초과할 수 없습니다.")
        String notes,

        @NotNull(message = "장치 타입을 선택해주세요.")
        @Min(value = 1, message = "유효하지 않은 장치 타입 ID입니다.")
        Long deviceTypeId,

        @NotNull(message = "전산실을 선택해주세요.")
        @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.")
        Long datacenterId,

        Long rackId  // server 타입일 경우 필수
) {
    public Device toEntity(DeviceType deviceType, DataCenter datacenter, Rack rack, Long managerId) {
        return Device.builder()
                .deviceName(this.deviceName)
                .deviceCode(this.deviceCode)
                .gridY(this.gridY)
                .gridX(this.gridX)
                .gridZ(this.gridZ != null ? this.gridZ : 0)
                .rotation(this.rotation != null ? this.rotation : 0)
                .status(this.status != null ? DeviceStatus.valueOf(this.status) : DeviceStatus.NORMAL)
                .modelName(this.modelName)
                .manufacturer(this.manufacturer)
                .serialNumber(this.serialNumber)
                .purchaseDate(this.purchaseDate)
                .warrantyEndDate(this.warrantyEndDate)
                .notes(this.notes)
                .deviceType(deviceType)
                .datacenter(datacenter)
                .rack(rack)
                .managerId(managerId)
                .build();
    }
}