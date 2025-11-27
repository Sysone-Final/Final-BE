/**
 * 작성자: 황요한
 * 랙 사용률 응답 DTO
 */
package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.util.Optional;

@Builder
public record RackUtilizationResponse(
        Long rackId,                 // 랙 ID
        String rackName,             // 랙 이름
        BigDecimal usageRate,        // 유닛 사용률
        Integer usedUnits,           // 사용 중인 유닛
        Integer availableUnits,      // 사용 가능 유닛
        Integer totalUnits,          // 전체 유닛
        BigDecimal powerUsageRate,   // 전력 사용률
        BigDecimal currentPowerUsage,// 현재 전력 사용량
        BigDecimal maxPowerCapacity, // 최대 전력 용량
        BigDecimal availablePowerCapacity // 사용 가능 전력
) {
    public static RackUtilizationResponse from(Rack rack) {
        BigDecimal availablePower = Optional.ofNullable(rack.getMaxPowerCapacity())
                .flatMap(max ->
                        Optional.ofNullable(rack.getCurrentPowerUsage())
                                .map(max::subtract)
                )
                .orElse(null);

        return RackUtilizationResponse.builder()
                .rackId(rack.getId())
                .rackName(rack.getRackName())
                .usageRate(rack.getUsageRate())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .totalUnits(rack.getTotalUnits())
                .powerUsageRate(rack.getPowerUsageRate())
                .currentPowerUsage(rack.getCurrentPowerUsage())
                .maxPowerCapacity(rack.getMaxPowerCapacity())
                .availablePowerCapacity(availablePower)
                .build();
    }
}
