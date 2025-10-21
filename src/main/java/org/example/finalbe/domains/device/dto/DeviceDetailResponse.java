package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.device.domain.Device;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record DeviceDetailResponse(
        Long id,
        String deviceName,
        String deviceCode,
        String deviceType,
        Long deviceTypeId,
        Integer positionRow,
        Integer positionCol,
        Integer positionZ,
        Integer rotation,
        String status,
        String modelName,
        String manufacturer,
        String serialNumber,
        LocalDate purchaseDate,
        LocalDate warrantyEndDate,
        String notes,
        Long managerId,
        String datacenterName,
        Long datacenterId,
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
                .positionRow(device.getPositionRow())
                .positionCol(device.getPositionCol())
                .positionZ(device.getPositionZ())
                .rotation(device.getRotation())
                .status(device.getStatus() != null ? device.getStatus().name() : null)
                .modelName(device.getModelName())
                .manufacturer(device.getManufacturer())
                .serialNumber(device.getSerialNumber())
                .purchaseDate(device.getPurchaseDate())
                .warrantyEndDate(device.getWarrantyEndDate())
                .notes(device.getNotes())
                .managerId(device.getManagerId())
                .datacenterName(device.getDatacenter().getName())
                .datacenterId(device.getDatacenter().getId())
                .rackName(device.getRack() != null ? device.getRack().getRackName() : null)
                .rackId(device.getRack() != null ? device.getRack().getId() : null)
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}