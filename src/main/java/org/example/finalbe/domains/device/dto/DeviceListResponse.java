/**
 * 작성자: 황요한
 * 장치 목록 조회 응답 DTO
 */
package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.device.domain.Device;

@Builder
public record DeviceListResponse(
        Long id,
        String deviceName,
        String deviceCode,
        String deviceType,
        Integer gridY,
        Integer gridX,
        Integer gridZ,
        Integer rotation,
        String status,
        String serverRoomName,
        Long serverRoomId,
        Integer serverRoomRows,
        Integer serverRoomColumns,
        DelYN delYn,
        String rackName,
        Long rackId
) {
    public static DeviceListResponse from(Device device) {
        return DeviceListResponse.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType().getTypeName())
                .gridY(device.getGridY())
                .gridX(device.getGridX())
                .gridZ(device.getGridZ())
                .rotation(device.getRotation())
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .serverRoomName(device.getServerRoom().getName())
                .serverRoomId(device.getServerRoom().getId())
                .serverRoomRows(device.getServerRoom().getRows())
                .serverRoomColumns(device.getServerRoom().getColumns())
                .rackName(device.getRack() != null ? device.getRack().getRackName() : null)
                .rackId(device.getRack() != null ? device.getRack().getId() : null)
                .build();
    }
}