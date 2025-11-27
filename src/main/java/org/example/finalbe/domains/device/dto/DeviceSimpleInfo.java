/**
 * 작성자: 황요한
 * 장치 간단 정보 DTO
 */
package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.device.domain.Device;

@Builder
public record DeviceSimpleInfo(
        Long id,
        String deviceName,
        String deviceCode,
        String deviceType,
        Long deviceTypeId,
        Integer gridY,
        Integer gridX,
        Integer gridZ,
        Integer rotation,
        String status,
        String rackName,
        Long rackId,
        Integer equipmentCount
) {
    public static DeviceSimpleInfo from(Device device, Integer equipmentCount) {
        return DeviceSimpleInfo.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceCode(device.getDeviceCode())
                .deviceType(device.getDeviceType().getTypeName())
                .deviceTypeId(device.getDeviceType().getId())
                .gridY(device.getGridY())
                .gridX(device.getGridX())
                .gridZ(device.getGridZ())
                .rotation(device.getRotation())
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .rackName(device.getRack() != null ? device.getRack().getRackName() : null)
                .rackId(device.getRack() != null ? device.getRack().getId() : null)
                .equipmentCount(equipmentCount)
                .build();
    }
}