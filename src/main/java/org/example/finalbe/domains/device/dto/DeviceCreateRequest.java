package org.example.finalbe.domains.device.dto;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DeviceStatus;
import org.example.finalbe.domains.device.domain.Device;
import org.example.finalbe.domains.device.domain.DeviceType;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.time.LocalDate;

/**
 * 장치 생성 요청 DTO
 */
@Builder
public record DeviceCreateRequest(
        String deviceName,

        String deviceCode,

        @Min(value = 0, message = "행 위치는 0 이상이어야 합니다.")
        Integer gridY,

        @Min(value = 0, message = "열 위치는 0 이상이어야 합니다.")
        Integer gridX,

        @Min(value = 0, message = "Z축 위치는 0 이상이어야 합니다.")
        Integer gridZ,

        @Min(value = 0, message = "회전 각도는 0 이상이어야 합니다.")
        Integer rotation,

        String status,

        String modelName,

        String manufacturer,

        String serialNumber,

        LocalDate purchaseDate,

        LocalDate warrantyEndDate,

        String notes,

        Long deviceTypeId,

        Long serverRoomId,

        Long rackId
) {
    /**
     * DTO를 Entity로 변환
     */
    public Device toEntity(DeviceType deviceType, ServerRoom serverRoom, Rack rack) {
        return Device.builder()
                .deviceName(this.deviceName)
                .deviceCode(this.deviceCode)
                .gridY(this.gridY != null ? this.gridY : 0)
                .gridX(this.gridX != null ? this.gridX : 0)
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
                .serverRoom(serverRoom)
                .rack(rack)
                .build();
    }
}