/**
 * 작성자: 황요한
 * 랙 카드 뷰 응답 DTO
 */
package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;

@Builder
public record RackCardResponse(
        Long id,
        String rackName,
        BigDecimal gridX,
        BigDecimal gridY,
        RackStatus status,
        BigDecimal usageRate,
        BigDecimal powerUsageRate,
        Integer usedUnits,
        Integer totalUnits,
        BigDecimal currentPowerUsage,
        BigDecimal maxPowerCapacity,
        BigDecimal temperature
) {
    public static RackCardResponse from(Rack rack) {
        return RackCardResponse.builder()
                .id(rack.getId())
                .rackName(rack.getRackName())
                .gridX(rack.getGridX())
                .gridY(rack.getGridY())
                .status(rack.getStatus())
                .usageRate(rack.getUsageRate())
                .powerUsageRate(rack.getPowerUsageRate())
                .usedUnits(rack.getUsedUnits())
                .totalUnits(rack.getTotalUnits())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .temperature(null)
                .build();
    }
}
