package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;

/**
 * 랙 목록 조회 응답 DTO
 */
@Builder
public record RackListResponse(
        Long id,
        String rackName,
        BigDecimal gridX,
        BigDecimal gridY,
        Integer totalUnits,
        Integer usedUnits,
        Integer availableUnits,
        RackStatus status,
        BigDecimal usageRate,
        BigDecimal powerUsageRate,
        BigDecimal currentPowerUsage,
        BigDecimal maxPowerCapacity,
        Integer equipmentCount
) {
    /**
     * Rack 엔티티로부터 DTO 생성
     */
    public static RackListResponse from(Rack rack) {
        return RackListResponse.builder()
                .id(rack.getId())
                .rackName(rack.getRackName())
                .gridX(rack.getGridX())
                .gridY(rack.getGridY())
                .totalUnits(rack.getTotalUnits())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .status(rack.getStatus())
                .usageRate(rack.getUsageRate())
                .powerUsageRate(rack.getPowerUsageRate())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .equipmentCount(0)  // 기본값 0
                .build();
    }

    /**
     * Rack 엔티티와 장비 개수로부터 DTO 생성
     */
    public static RackListResponse from(Rack rack, Integer equipmentCount) {
        return RackListResponse.builder()
                .id(rack.getId())
                .rackName(rack.getRackName())
                .gridX(rack.getGridX())
                .gridY(rack.getGridY())
                .totalUnits(rack.getTotalUnits())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .status(rack.getStatus())
                .usageRate(rack.getUsageRate())
                .powerUsageRate(rack.getPowerUsageRate())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .equipmentCount(equipmentCount)
                .build();
    }
}