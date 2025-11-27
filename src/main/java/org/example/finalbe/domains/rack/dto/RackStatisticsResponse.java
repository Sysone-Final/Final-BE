/**
 * 작성자: 황요한
 * 랙 통계 응답 DTO
 */
package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public record RackStatisticsResponse(
        Integer totalRacks,
        Integer activeRacks,
        Integer maintenanceRacks,
        BigDecimal averageUsageRate,
        BigDecimal averagePowerUsageRate,
        Integer totalUsedUnits,
        Integer totalAvailableUnits,
        List<RackUsageData> rackUsageData,
        List<RackPowerData> rackPowerData
) {

    public static RackStatisticsResponse from(List<Rack> racks) {

        int totalRacks = racks.size();

        int activeRacks = (int) racks.stream()
                .filter(r -> r.getStatus() == org.example.finalbe.domains.common.enumdir.RackStatus.ACTIVE)
                .count();

        int maintenanceRacks = (int) racks.stream()
                .filter(r -> r.getStatus() == org.example.finalbe.domains.common.enumdir.RackStatus.MAINTENANCE)
                .count();

        BigDecimal averageUsageRate = racks.isEmpty() ? BigDecimal.ZERO :
                racks.stream()
                        .map(Rack::getUsageRate)
                        .filter(rate -> rate != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(racks.size()), 2, RoundingMode.HALF_UP);

        BigDecimal averagePowerUsageRate = racks.isEmpty() ? BigDecimal.ZERO :
                racks.stream()
                        .map(Rack::getPowerUsageRate)
                        .filter(rate -> rate != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(racks.size()), 2, RoundingMode.HALF_UP);

        int totalUsedUnits = racks.stream()
                .mapToInt(Rack::getUsedUnits)
                .sum();

        int totalAvailableUnits = racks.stream()
                .mapToInt(Rack::getAvailableUnits)
                .sum();

        List<RackUsageData> rackUsageData = racks.stream()
                .map(r -> new RackUsageData(r.getRackName(), r.getUsageRate()))
                .collect(Collectors.toList());

        List<RackPowerData> rackPowerData = racks.stream()
                .map(r -> new RackPowerData(r.getRackName(), r.getCurrentPowerUsage(), r.getMaxPowerCapacity()))
                .collect(Collectors.toList());

        return RackStatisticsResponse.builder()
                .totalRacks(totalRacks)
                .activeRacks(activeRacks)
                .maintenanceRacks(maintenanceRacks)
                .averageUsageRate(averageUsageRate)
                .averagePowerUsageRate(averagePowerUsageRate)
                .totalUsedUnits(totalUsedUnits)
                .totalAvailableUnits(totalAvailableUnits)
                .rackUsageData(rackUsageData)
                .rackPowerData(rackPowerData)
                .build();
    }

    public record RackUsageData(String rackName, BigDecimal usageRate) {}

    public record RackPowerData(String rackName, BigDecimal currentPower, BigDecimal maxPower) {}
}
