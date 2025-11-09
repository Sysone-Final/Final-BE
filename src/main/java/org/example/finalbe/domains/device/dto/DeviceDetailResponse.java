package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.device.domain.Device;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 장치 상세 조회 응답 DTO
 */
@Builder
public record DeviceDetailResponse(
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
        String modelName,
        String manufacturer,
        String serialNumber,
        LocalDate purchaseDate,
        LocalDate warrantyEndDate,
        String notes,
        String serverRoomName,
        Long serverRoomId,
        String rackName,
        Long rackId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DeviceDetailResponse from(Device device) {
        return DeviceDetailResponse.builder()
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
                .modelName(device.getModelName())
                .manufacturer(device.getManufacturer())
                .serialNumber(device.getSerialNumber())
                .purchaseDate(device.getPurchaseDate())
                .warrantyEndDate(device.getWarrantyEndDate())
                .notes(device.getNotes())
                .serverRoomName(device.getServerRoom().getName())
                .serverRoomId(device.getServerRoom().getId())
                .rackName(device.getRack() != null ? device.getRack().getRackName() : null)
                .rackId(device.getRack() != null ? device.getRack().getId() : null)
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}