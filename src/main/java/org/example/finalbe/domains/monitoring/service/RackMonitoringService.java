package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.monitoring.dto.RackStatisticsDto;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackMonitoringService {

    private final RackRepository rackRepository;
    private final EquipmentRepository equipmentRepository;
    private final MonitoringMetricCache metricCache;

    private static final double CPU_WARNING_THRESHOLD = 70.0;
    private static final double CPU_CRITICAL_THRESHOLD = 90.0;
    private static final double MEMORY_WARNING_THRESHOLD = 70.0;
    private static final double MEMORY_CRITICAL_THRESHOLD = 90.0;
    private static final double DISK_WARNING_THRESHOLD = 70.0;
    private static final double DISK_CRITICAL_THRESHOLD = 90.0;


    private RackStatisticsDto.EnvironmentStats getEnvironmentStats(Long rackId) {
        EnvironmentMetric metric = metricCache.getEnvironmentMetric(rackId).orElse(null);

        if (metric == null) {
            return RackStatisticsDto.EnvironmentStats.builder().build();
        }

        return RackStatisticsDto.EnvironmentStats.builder()
                .temperature(metric.getTemperature())
                .minTemperature(metric.getMinTemperature())
                .maxTemperature(metric.getMaxTemperature())
                .humidity(metric.getHumidity())
                .minHumidity(metric.getMinHumidity())
                .maxHumidity(metric.getMaxHumidity())
                .temperatureWarning(metric.getTemperatureWarning())
                .humidityWarning(metric.getHumidityWarning())
                .build();
    }

    private RackStatisticsDto.RackSummary calculateRackSummary(
            List<Equipment> equipments, List<Long> equipmentIds) {

        int normalCount = 0;
        int warningCount = 0;
        int errorCount = 0;

        for (Long equipmentId : equipmentIds) {
            String status = determineEquipmentStatus(equipmentId);
            switch (status) {
                case "NORMAL" -> normalCount++;
                case "WARNING" -> warningCount++;
                case "ERROR" -> errorCount++;
            }
        }

        Map<EquipmentType, Long> typeCounts = equipments.stream()
                .collect(Collectors.groupingBy(Equipment::getType, Collectors.counting()));

        List<RackStatisticsDto.EquipmentTypeCount> activeEquipmentTypes = typeCounts.entrySet().stream()
                .map(entry -> RackStatisticsDto.EquipmentTypeCount.builder()
                        .type(entry.getKey().name())
                        .count(entry.getValue().intValue())
                        .build())
                .collect(Collectors.toList());

        return RackStatisticsDto.RackSummary.builder()
                .totalEquipmentCount(equipments.size())
                .normalCount(normalCount)
                .warningCount(warningCount)
                .errorCount(errorCount)
                .activeEquipmentTypes(activeEquipmentTypes)
                .build();
    }

    private String determineEquipmentStatus(Long equipmentId) {
        SystemMetric systemMetric = metricCache.getSystemMetric(equipmentId).orElse(null);
        DiskMetric diskMetric = metricCache.getDiskMetric(equipmentId).orElse(null);

        boolean hasError = false;
        boolean hasWarning = false;

        if (systemMetric != null && systemMetric.getCpuIdle() != null) {
            double cpuUsage = 100.0 - systemMetric.getCpuIdle();
            if (cpuUsage >= CPU_CRITICAL_THRESHOLD) {
                hasError = true;
            } else if (cpuUsage >= CPU_WARNING_THRESHOLD) {
                hasWarning = true;
            }
        }

        if (systemMetric != null && systemMetric.getUsedMemoryPercentage() != null) {
            double memoryUsage = systemMetric.getUsedMemoryPercentage();
            if (memoryUsage >= MEMORY_CRITICAL_THRESHOLD) {
                hasError = true;
            } else if (memoryUsage >= MEMORY_WARNING_THRESHOLD) {
                hasWarning = true;
            }
        }

        if (diskMetric != null && diskMetric.getUsedPercentage() != null) {
            double diskUsage = diskMetric.getUsedPercentage();
            if (diskUsage >= DISK_CRITICAL_THRESHOLD) {
                hasError = true;
            } else if (diskUsage >= DISK_WARNING_THRESHOLD) {
                hasWarning = true;
            }
        }

        if (hasError) return "ERROR";
        if (hasWarning) return "WARNING";
        return "NORMAL";
    }

    private RackStatisticsDto.CpuStats calculateCpuStats(
            List<Equipment> equipments, List<Long> equipmentIds) {

        List<SystemMetric> metrics = new ArrayList<>();
        for (Long equipmentId : equipmentIds) {
            metricCache.getSystemMetric(equipmentId).ifPresent(metrics::add);
        }

        if (metrics.isEmpty()) {
            return RackStatisticsDto.CpuStats.builder().equipmentCount(0).build();
        }

        double avgUsage = metrics.stream()
                .filter(m -> m.getCpuIdle() != null)
                .mapToDouble(m -> 100.0 - m.getCpuIdle())
                .average()
                .orElse(0.0);

        double maxUsage = metrics.stream()
                .filter(m -> m.getCpuIdle() != null)
                .mapToDouble(m -> 100.0 - m.getCpuIdle())
                .max()
                .orElse(0.0);

        Map<Long, String> equipmentNameMap = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        List<RackStatisticsDto.TopEquipment> topEquipments = metrics.stream()
                .filter(m -> m.getCpuIdle() != null)
                .sorted(Comparator.comparingDouble((SystemMetric m) -> 100.0 - m.getCpuIdle()).reversed())
                .limit(5)
                .map(m -> RackStatisticsDto.TopEquipment.builder()
                        .equipmentId(m.getEquipmentId())
                        .equipmentName(equipmentNameMap.get(m.getEquipmentId()))
                        .value(100.0 - m.getCpuIdle())
                        .build())
                .collect(Collectors.toList());

        return RackStatisticsDto.CpuStats.builder()
                .avgUsage(avgUsage)
                .maxUsage(maxUsage)
                .topEquipments(topEquipments)
                .equipmentCount(metrics.size())
                .build();
    }

    private RackStatisticsDto.MemoryStats calculateMemoryStats(
            List<Equipment> equipments, List<Long> equipmentIds) {

        List<SystemMetric> metrics = new ArrayList<>();
        for (Long equipmentId : equipmentIds) {
            metricCache.getSystemMetric(equipmentId).ifPresent(metrics::add);
        }

        if (metrics.isEmpty()) {
            return RackStatisticsDto.MemoryStats.builder().equipmentCount(0).build();
        }

        double avgUsage = metrics.stream()
                .filter(m -> m.getUsedMemoryPercentage() != null)
                .mapToDouble(SystemMetric::getUsedMemoryPercentage)
                .average()
                .orElse(0.0);

        double maxUsage = metrics.stream()
                .filter(m -> m.getUsedMemoryPercentage() != null)
                .mapToDouble(SystemMetric::getUsedMemoryPercentage)
                .max()
                .orElse(0.0);

        long totalMemoryBytes = metrics.stream()
                .filter(m -> m.getTotalMemory() != null)
                .mapToLong(SystemMetric::getTotalMemory)
                .sum();

        long usedMemoryBytes = metrics.stream()
                .filter(m -> m.getUsedMemory() != null)
                .mapToLong(SystemMetric::getUsedMemory)
                .sum();

        Map<Long, String> equipmentNameMap = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        List<RackStatisticsDto.TopEquipment> topEquipments = metrics.stream()
                .filter(m -> m.getUsedMemoryPercentage() != null)
                .sorted(Comparator.comparingDouble(SystemMetric::getUsedMemoryPercentage).reversed())
                .limit(5)
                .map(m -> RackStatisticsDto.TopEquipment.builder()
                        .equipmentId(m.getEquipmentId())
                        .equipmentName(equipmentNameMap.get(m.getEquipmentId()))
                        .value(m.getUsedMemoryPercentage())
                        .build())
                .collect(Collectors.toList());

        return RackStatisticsDto.MemoryStats.builder()
                .avgUsage(avgUsage)
                .maxUsage(maxUsage)
                .topEquipments(topEquipments)
                .equipmentCount(metrics.size())
                .totalMemoryGB(totalMemoryBytes / (1024 * 1024 * 1024))
                .usedMemoryGB(usedMemoryBytes / (1024 * 1024 * 1024))
                .build();
    }

    private RackStatisticsDto.DiskStats calculateDiskStats(
            List<Equipment> equipments, List<Long> equipmentIds) {

        List<Long> diskEquipmentIds = equipments.stream()
                .filter(e -> e.getType() == EquipmentType.SERVER || e.getType() == EquipmentType.STORAGE)
                .map(Equipment::getId)
                .collect(Collectors.toList());

        List<DiskMetric> metrics = new ArrayList<>();
        for (Long equipmentId : diskEquipmentIds) {
            metricCache.getDiskMetric(equipmentId).ifPresent(metrics::add);
        }

        if (metrics.isEmpty()) {
            return RackStatisticsDto.DiskStats.builder().equipmentCount(0).build();
        }

        double avgUsage = metrics.stream()
                .filter(m -> m.getUsedPercentage() != null)
                .mapToDouble(DiskMetric::getUsedPercentage)
                .average()
                .orElse(0.0);

        double maxUsage = metrics.stream()
                .filter(m -> m.getUsedPercentage() != null)
                .mapToDouble(DiskMetric::getUsedPercentage)
                .max()
                .orElse(0.0);

        long totalBytes = metrics.stream()
                .filter(m -> m.getTotalBytes() != null)
                .mapToLong(DiskMetric::getTotalBytes)
                .sum();

        long usedBytes = metrics.stream()
                .filter(m -> m.getUsedBytes() != null)
                .mapToLong(DiskMetric::getUsedBytes)
                .sum();

        Map<Long, String> equipmentNameMap = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        List<RackStatisticsDto.TopEquipment> topEquipments = metrics.stream()
                .filter(m -> m.getUsedPercentage() != null)
                .sorted(Comparator.comparingDouble(DiskMetric::getUsedPercentage).reversed())
                .limit(5)
                .map(m -> RackStatisticsDto.TopEquipment.builder()
                        .equipmentId(m.getEquipmentId())
                        .equipmentName(equipmentNameMap.get(m.getEquipmentId()))
                        .value(m.getUsedPercentage())
                        .build())
                .collect(Collectors.toList());

        return RackStatisticsDto.DiskStats.builder()
                .avgUsage(avgUsage)
                .maxUsage(maxUsage)
                .topEquipments(topEquipments)
                .equipmentCount(metrics.size())
                .totalCapacityTB(totalBytes / (1024L * 1024L * 1024L * 1024L))
                .usedCapacityTB(usedBytes / (1024L * 1024L * 1024L * 1024L))
                .build();
    }

    private RackStatisticsDto.NetworkStats calculateNetworkStats(
            List<Equipment> equipments, List<Long> equipmentIds) {

        Map<Long, AggregatedNetworkMetric> aggregatedMetrics = new HashMap<>();

        for (Long equipmentId : equipmentIds) {
            List<NetworkMetric> nicMetrics = metricCache.getNetworkMetrics(equipmentId);
            if (!nicMetrics.isEmpty()) {
                AggregatedNetworkMetric aggregated = aggregateNicMetrics(nicMetrics);
                aggregatedMetrics.put(equipmentId, aggregated);
            }
        }

        if (aggregatedMetrics.isEmpty()) {
            return RackStatisticsDto.NetworkStats.builder().equipmentCount(0).build();
        }

        double totalRxMbps = aggregatedMetrics.values().stream()
                .mapToDouble(m -> m.inBytesPerSec * 8 / 1_000_000)
                .sum();

        double totalTxMbps = aggregatedMetrics.values().stream()
                .mapToDouble(m -> m.outBytesPerSec * 8 / 1_000_000)
                .sum();

        double avgRxUsage = aggregatedMetrics.values().stream()
                .mapToDouble(m -> m.rxUsage)
                .average()
                .orElse(0.0);

        double avgTxUsage = aggregatedMetrics.values().stream()
                .mapToDouble(m -> m.txUsage)
                .average()
                .orElse(0.0);

        long totalInPackets = aggregatedMetrics.values().stream()
                .mapToLong(m -> m.inPktsTot)
                .sum();

        long totalInErrors = aggregatedMetrics.values().stream()
                .mapToLong(m -> m.inErrorPktsTot + m.inDiscardPktsTot)
                .sum();

        long totalOutPackets = aggregatedMetrics.values().stream()
                .mapToLong(m -> m.outPktsTot)
                .sum();

        long totalOutErrors = aggregatedMetrics.values().stream()
                .mapToLong(m -> m.outErrorPktsTot + m.outDiscardPktsTot)
                .sum();

        double errorPacketRate = totalInPackets > 0
                ? (totalInErrors * 100.0 / totalInPackets)
                : 0.0;

        double dropPacketRate = totalOutPackets > 0
                ? (totalOutErrors * 100.0 / totalOutPackets)
                : 0.0;

        Map<Long, String> equipmentNameMap = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        List<RackStatisticsDto.TopEquipment> topRxEquipments = aggregatedMetrics.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<Long, AggregatedNetworkMetric> e) ->
                        e.getValue().inBytesPerSec).reversed())
                .limit(5)
                .map(e -> RackStatisticsDto.TopEquipment.builder()
                        .equipmentId(e.getKey())
                        .equipmentName(equipmentNameMap.get(e.getKey()))
                        .value(e.getValue().inBytesPerSec * 8 / 1_000_000)
                        .build())
                .collect(Collectors.toList());

        List<RackStatisticsDto.TopEquipment> topTxEquipments = aggregatedMetrics.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<Long, AggregatedNetworkMetric> e) ->
                        e.getValue().outBytesPerSec).reversed())
                .limit(5)
                .map(e -> RackStatisticsDto.TopEquipment.builder()
                        .equipmentId(e.getKey())
                        .equipmentName(equipmentNameMap.get(e.getKey()))
                        .value(e.getValue().outBytesPerSec * 8 / 1_000_000)
                        .build())
                .collect(Collectors.toList());

        return RackStatisticsDto.NetworkStats.builder()
                .totalRxMbps(totalRxMbps)
                .totalTxMbps(totalTxMbps)
                .avgRxUsage(avgRxUsage)
                .avgTxUsage(avgTxUsage)
                .topRxEquipments(topRxEquipments)
                .topTxEquipments(topTxEquipments)
                .errorPacketRate(errorPacketRate)
                .dropPacketRate(dropPacketRate)
                .equipmentCount(aggregatedMetrics.size())
                .build();
    }

    private AggregatedNetworkMetric aggregateNicMetrics(List<NetworkMetric> nicMetrics) {
        AggregatedNetworkMetric result = new AggregatedNetworkMetric();

        for (NetworkMetric metric : nicMetrics) {
            if (metric.getRxUsage() != null) result.rxUsage += metric.getRxUsage();
            if (metric.getTxUsage() != null) result.txUsage += metric.getTxUsage();
            if (metric.getInBytesPerSec() != null) result.inBytesPerSec += metric.getInBytesPerSec();
            if (metric.getOutBytesPerSec() != null) result.outBytesPerSec += metric.getOutBytesPerSec();
            if (metric.getInPktsTot() != null) result.inPktsTot += metric.getInPktsTot();
            if (metric.getOutPktsTot() != null) result.outPktsTot += metric.getOutPktsTot();
            if (metric.getInErrorPktsTot() != null) result.inErrorPktsTot += metric.getInErrorPktsTot();
            if (metric.getOutErrorPktsTot() != null) result.outErrorPktsTot += metric.getOutErrorPktsTot();
            if (metric.getInDiscardPktsTot() != null) result.inDiscardPktsTot += metric.getInDiscardPktsTot();
            if (metric.getOutDiscardPktsTot() != null) result.outDiscardPktsTot += metric.getOutDiscardPktsTot();
        }

        return result;
    }



    private static class AggregatedNetworkMetric {
        double rxUsage = 0.0;
        double txUsage = 0.0;
        double inBytesPerSec = 0.0;
        double outBytesPerSec = 0.0;
        long inPktsTot = 0L;
        long outPktsTot = 0L;
        long inErrorPktsTot = 0L;
        long outErrorPktsTot = 0L;
        long inDiscardPktsTot = 0L;
        long outDiscardPktsTot = 0L;
    }

    // RackMonitoringService.java

    public RackStatisticsDto calculateRackStatistics(Long rackId) {
        log.debug("📊 랙 통계 계산 시작: rackId={}", rackId);

        Rack rack = rackRepository.findById(rackId)
                .orElseThrow(() -> new IllegalArgumentException("랙을 찾을 수 없습니다: " + rackId));

        LocalDateTime now = LocalDateTime.now();
        List<Equipment> equipments = equipmentRepository.findByRackIdAndDelYn(rackId, DelYN.N);

        if (equipments.isEmpty()) {
            log.debug("⚠️ 랙에 활성 장비가 없습니다: rackId={}", rackId);
            return createEmptyStatistics(rack, now);
        }

        List<Long> equipmentIds = equipments.stream()
                .map(Equipment::getId)
                .collect(Collectors.toList());

        RackStatisticsDto.EnvironmentStats environmentStats = getEnvironmentStats(rackId);
        RackStatisticsDto.RackSummary rackSummary = calculateRackSummary(equipments, equipmentIds);
        RackStatisticsDto.CpuStats cpuStats = calculateCpuStats(equipments, equipmentIds);
        RackStatisticsDto.SystemLoadStats systemLoadStats = calculateSystemLoadStats(equipments, equipmentIds);  // ✅ 추가
        RackStatisticsDto.MemoryStats memoryStats = calculateMemoryStats(equipments, equipmentIds);
        RackStatisticsDto.DiskStats diskStats = calculateDiskStats(equipments, equipmentIds);
        RackStatisticsDto.NetworkStats networkStats = calculateNetworkStats(equipments, equipmentIds);

        return RackStatisticsDto.builder()
                .rackId(rackId)
                .rackName(rack.getRackName())
                .timestamp(now)
                .environment(environmentStats)
                .rackSummary(rackSummary)
                .cpuStats(cpuStats)
                .systemLoadStats(systemLoadStats)  // ✅ 추가
                .memoryStats(memoryStats)
                .diskStats(diskStats)
                .networkStats(networkStats)
                .build();
    }

    // ✅ 새로운 메서드 추가
    private RackStatisticsDto.SystemLoadStats calculateSystemLoadStats(
            List<Equipment> equipments, List<Long> equipmentIds) {

        List<SystemMetric> metrics = new ArrayList<>();
        for (Long equipmentId : equipmentIds) {
            metricCache.getSystemMetric(equipmentId).ifPresent(metrics::add);
        }

        if (metrics.isEmpty()) {
            return RackStatisticsDto.SystemLoadStats.builder().equipmentCount(0).build();
        }

        // 1분 평균 부하
        double avgLoadAvg1 = metrics.stream()
                .filter(m -> m.getLoadAvg1() != null)
                .mapToDouble(SystemMetric::getLoadAvg1)
                .average()
                .orElse(0.0);

        double maxLoadAvg1 = metrics.stream()
                .filter(m -> m.getLoadAvg1() != null)
                .mapToDouble(SystemMetric::getLoadAvg1)
                .max()
                .orElse(0.0);

        // 5분 평균 부하
        double avgLoadAvg5 = metrics.stream()
                .filter(m -> m.getLoadAvg5() != null)
                .mapToDouble(SystemMetric::getLoadAvg5)
                .average()
                .orElse(0.0);

        double maxLoadAvg5 = metrics.stream()
                .filter(m -> m.getLoadAvg5() != null)
                .mapToDouble(SystemMetric::getLoadAvg5)
                .max()
                .orElse(0.0);

        // 15분 평균 부하
        double avgLoadAvg15 = metrics.stream()
                .filter(m -> m.getLoadAvg15() != null)
                .mapToDouble(SystemMetric::getLoadAvg15)
                .average()
                .orElse(0.0);

        double maxLoadAvg15 = metrics.stream()
                .filter(m -> m.getLoadAvg15() != null)
                .mapToDouble(SystemMetric::getLoadAvg15)
                .max()
                .orElse(0.0);

        return RackStatisticsDto.SystemLoadStats.builder()
                .avgLoadAvg1(avgLoadAvg1)
                .avgLoadAvg5(avgLoadAvg5)
                .avgLoadAvg15(avgLoadAvg15)
                .maxLoadAvg1(maxLoadAvg1)
                .maxLoadAvg5(maxLoadAvg5)
                .maxLoadAvg15(maxLoadAvg15)
                .equipmentCount(metrics.size())
                .build();
    }

    private RackStatisticsDto createEmptyStatistics(Rack rack, LocalDateTime now) {
        return RackStatisticsDto.builder()
                .rackId(rack.getId())
                .rackName(rack.getRackName())
                .timestamp(now)
                .environment(RackStatisticsDto.EnvironmentStats.builder().build())
                .rackSummary(RackStatisticsDto.RackSummary.builder()
                        .totalEquipmentCount(0)
                        .normalCount(0)
                        .warningCount(0)
                        .errorCount(0)
                        .activeEquipmentTypes(Collections.emptyList())
                        .build())
                .cpuStats(RackStatisticsDto.CpuStats.builder().equipmentCount(0).build())
                .systemLoadStats(RackStatisticsDto.SystemLoadStats.builder().equipmentCount(0).build())  // ✅ 추가
                .memoryStats(RackStatisticsDto.MemoryStats.builder().equipmentCount(0).build())
                .diskStats(RackStatisticsDto.DiskStats.builder().equipmentCount(0).build())
                .networkStats(RackStatisticsDto.NetworkStats.builder().equipmentCount(0).build())
                .build();
    }
}