package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.rack.domain.Rack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
public record RackStatisticsResponse(
        Integer totalRacks,
        Integer activeRacks,
        Integer maintenanceRacks,
        Double averageUsageRate,
        Double averagePowerUsageRate,
        Integer totalUsedUnits,
        Integer totalAvailableUnits,
        List<RackUsageData> rackUsageData,
        List<RackPowerData> rackPowerData,
        Map<String, Integer> departmentDistribution,
        Map<Long, Integer> managerDistribution
) {
    public static RackStatisticsResponse from(List<Rack> racks) {
        int totalRacks = racks.size();
        int activeRacks = (int) racks.stream()
                .filter(r -> r.getStatus() == org.example.finalbe.domains.common.enumdir.RackStatus.ACTIVE)
                .count();
        int maintenanceRacks = (int) racks.stream()
                .filter(r -> r.getStatus() == org.example.finalbe.domains.common.enumdir.RackStatus.MAINTENANCE)
                .count();

        double avgUsage = racks.stream()
                .mapToDouble(Rack::getUsageRate)
                .average()
                .orElse(0.0);

        double avgPower = racks.stream()
                .mapToDouble(Rack::getPowerUsageRate)
                .average()
                .orElse(0.0);

        int totalUsed = racks.stream()
                .mapToInt(Rack::getUsedUnits)
                .sum();

        int totalAvailable = racks.stream()
                .mapToInt(Rack::getAvailableUnits)
                .sum();

        List<RackUsageData> usageData = racks.stream()
                .map(r -> new RackUsageData(r.getRackName(), r.getUsageRate()))
                .collect(Collectors.toList());

        List<RackPowerData> powerData = racks.stream()
                .map(r -> new RackPowerData(r.getRackName(), r.getCurrentPowerUsage(), r.getMaxPowerCapacity()))
                .collect(Collectors.toList());

        Map<String, Integer> deptDist = racks.stream()
                .filter(r -> r.getDepartment() != null)
                .collect(Collectors.groupingBy(
                        Rack::getDepartment,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<Long, Integer> mgrDist = racks.stream()
                .filter(r -> r.getManagerId() != null)
                .collect(Collectors.groupingBy(
                        Rack::getManagerId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        return RackStatisticsResponse.builder()
                .totalRacks(totalRacks)
                .activeRacks(activeRacks)
                .maintenanceRacks(maintenanceRacks)
                .averageUsageRate(avgUsage)
                .averagePowerUsageRate(avgPower)
                .totalUsedUnits(totalUsed)
                .totalAvailableUnits(totalAvailable)
                .rackUsageData(usageData)
                .rackPowerData(powerData)
                .departmentDistribution(deptDist)
                .managerDistribution(mgrDist)
                .build();
    }

    public record RackUsageData(String rackName, Double usageRate) {}
    public record RackPowerData(String rackName, Double currentPower, Double maxPower) {}
}