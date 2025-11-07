package org.example.finalbe.domains.device.dto;

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

        Integer gridY,

        Integer gridX,

        Integer gridZ,

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
    public Device toEntity(DeviceType deviceType, ServerRoom serverRoom, Rack rack, Long managerId) {
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
                .serverRoom(serverRoom)
                .rack(rack)
                .managerId(managerId)
                .build();
    }
}