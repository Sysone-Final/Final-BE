package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 랙 상세 조회 응답 DTO
 */
@Builder
public record RackDetailResponse(
        Long id,
        String rackName,
        BigDecimal gridX,
        BigDecimal gridY,
        Integer totalUnits,
        Integer usedUnits,
        Integer availableUnits,
        DoorDirection doorDirection,
        ZoneDirection zoneDirection,
        BigDecimal maxPowerCapacity,
        BigDecimal currentPowerUsage,
        BigDecimal measuredPower,
        String manufacturer,
        String serialNumber,
        RackStatus status,
        RackType rackType,
        String notes,
        Long serverRoomId,
        String serverRoomName,
        BigDecimal usageRate,
        BigDecimal powerUsageRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RackDetailResponse from(Rack rack) {
        return RackDetailResponse.builder()
                .id(rack.getId())
                .rackName(rack.getRackName())
                .gridX(rack.getGridX())
                .gridY(rack.getGridY())
                .totalUnits(rack.getTotalUnits())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .doorDirection(rack.getDoorDirection())
                .zoneDirection(rack.getZoneDirection())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .measuredPower(rack.getMeasuredPower())
                .manufacturer(rack.getManufacturer())
                .serialNumber(rack.getSerialNumber())
                .status(rack.getStatus())
                .rackType(rack.getRackType())
                .notes(rack.getNotes())
                .serverRoomId(rack.getServerRoom().getId())
                .serverRoomName(rack.getServerRoom().getName())
                .usageRate(rack.getUsageRate())
                .powerUsageRate(rack.getPowerUsageRate())
                .createdAt(rack.getCreatedAt())
                .updatedAt(rack.getUpdatedAt())
                .build();
    }
}