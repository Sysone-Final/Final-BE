package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.device.domain.Device;

@Builder
public record DeviceListResponse(
        Long id,
        String deviceName,
        String deviceCode,
        String deviceType,
        Integer positionRow,
        Integer positionCol,
        Integer positionZ,
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
                .positionRow(device.getPositionRow())
                .positionCol(device.getPositionCol())
                .positionZ(device.getPositionZ())
                .rotation(device.getRotation())
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .datacenterName(device.getDatacenter().getName())
                .datacenterId(device.getDatacenter().getId())
                .rackName(device.getRack() != null ? device.getRack().getRackName() : null)
                .rackId(device.getRack() != null ? device.getRack().getId() : null)
                .build();
    }
}