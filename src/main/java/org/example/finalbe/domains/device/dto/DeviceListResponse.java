package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.device.domain.Device;

/**
 * 장치 목록 조회 응답 DTO
 */
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
        String datacenterName,
        Long datacenterId,
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
                .datacenterName(device.getDatacenter().getName())
                .datacenterId(device.getDatacenter().getId())
                .rackName(device.getRack() != null ? device.getRack().getRackName() : null)
                .rackId(device.getRack() != null ? device.getRack().getId() : null)
                .build();
    }
}