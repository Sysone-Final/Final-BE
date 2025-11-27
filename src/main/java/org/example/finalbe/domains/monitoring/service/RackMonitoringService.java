/**
 * 작성자: 황요한
 * 랙 단위 모니터링 통계 계산 서비스
 */
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


    private static final double TEMP_MAX_THRESHOLD = 28.0;
    private static final double TEMP_MIN_THRESHOLD = 18.0;
    private static final double HUMIDITY_MAX_THRESHOLD = 70.0;
    private static final double HUMIDITY_MIN_THRESHOLD = 30.0;
    private static final double CPU_THRESHOLD = 80.0;
    private static final double MEMORY_THRESHOLD = 85.0;
    private static final double DISK_THRESHOLD = 85.0;


    private static final double NETWORK_ERROR_RATE_THRESHOLD = 1.0;  // 에러 패킷률 1%
    private static final double NETWORK_DROP_RATE_THRESHOLD = 1.0;   // 드롭 패킷률 1%

    private static final double CPU_WARNING_THRESHOLD = 70.0;
    private static final double CPU_CRITICAL_THRESHOLD = 90.0;
    private static final double MEMORY_WARNING_THRESHOLD = 70.0;
    private static final double MEMORY_CRITICAL_THRESHOLD = 90.0;
    private static final double DISK_WARNING_THRESHOLD = 70.0;
    private static final double DISK_CRITICAL_THRESHOLD = 90.0;

    public RackStatisticsDto calculateRackStatistics(Long rackId) {
        log.debug("랙 통계 계산 시작: rackId={}", rackId);

        Rack rack = rackRepository.findById(rackId)
                .orElseThrow(() -> new IllegalArgumentException("랙을 찾을 수 없습니다: " + rackId));

        LocalDateTime now = LocalDateTime.now();
        List<Equipment> equipments = equipmentRepository.findByRackIdAndDelYn(rackId, DelYN.N);

        if (equipments.isEmpty()) {
            log.debug("랙에 활성 장비가 없습니다: rackId={}", rackId);
            return createEmptyStatistics(rack, now);
        }

        List<Long> equipmentIds = equipments.stream()
                .map(Equipment::getId)
                .collect(Collectors.toList());

        RackStatisticsDto.EnvironmentStats environmentStats = getEnvironmentStats(rackId);
        RackStatisticsDto.RackSummary rackSummary = calculateRackSummary(equipments, equipmentIds);
        RackStatisticsDto.CpuStats cpuStats = calculateCpuStats(equipments, equipmentIds);
        RackStatisticsDto.SystemLoadStats systemLoadStats = calculateSystemLoadStatsOptimized(equipments, equipmentIds);
        RackStatisticsDto.MemoryStats memoryStats = calculateMemoryStats(equipments, equipmentIds);
        RackStatisticsDto.DiskStats diskStats = calculateDiskStats(equipments, equipmentIds);
        RackStatisticsDto.NetworkStats networkStats = calculateNetworkStats(equipments, equipmentIds);


        RackStatisticsDto.WarningDetails warningDetails = checkWarnings(
                environmentStats, cpuStats, memoryStats, diskStats, networkStats);

        return RackStatisticsDto.builder()
                .rackId(rackId)
                .rackName(rack.getRackName())
                .timestamp(now)
                .environment(environmentStats)
                .rackSummary(rackSummary)
                .cpuStats(cpuStats)
                .systemLoadStats(systemLoadStats)
                .memoryStats(memoryStats)
                .diskStats(diskStats)
                .networkStats(networkStats)
                .isWarning(warningDetails.hasAnyWarning())
                .warningDetails(warningDetails)
                .build();
    }

    // 경고 체크 메서드 (네트워크 추가)
    private RackStatisticsDto.WarningDetails checkWarnings(
            RackStatisticsDto.EnvironmentStats environmentStats,
            RackStatisticsDto.CpuStats cpuStats,
            RackStatisticsDto.MemoryStats memoryStats,
            RackStatisticsDto.DiskStats diskStats,
            RackStatisticsDto.NetworkStats networkStats) {

        // 온도 체크
        boolean temperatureWarning = false;
        if (environmentStats.getTemperature() != null) {
            temperatureWarning = environmentStats.getTemperature() > TEMP_MAX_THRESHOLD ||
                    environmentStats.getTemperature() < TEMP_MIN_THRESHOLD;
        }

        // 습도 체크
        boolean humidityWarning = false;
        if (environmentStats.getHumidity() != null) {
            humidityWarning = environmentStats.getHumidity() > HUMIDITY_MAX_THRESHOLD ||
                    environmentStats.getHumidity() < HUMIDITY_MIN_THRESHOLD;
        }

        // CPU 사용률 체크
        boolean cpuWarning = false;
        if (cpuStats.getMaxUsage() != null) {
            cpuWarning = cpuStats.getMaxUsage() > CPU_THRESHOLD;
        }

        // 메모리 사용률 체크
        boolean memoryWarning = false;
        if (memoryStats.getMaxUsage() != null) {
            memoryWarning = memoryStats.getMaxUsage() > MEMORY_THRESHOLD;
        }

        // 디스크 사용률 체크
        boolean diskWarning = false;
        if (diskStats.getMaxUsage() != null) {
            diskWarning = diskStats.getMaxUsage() > DISK_THRESHOLD;
        }

        // 네트워크 에러/드롭 체크 추가
        boolean networkWarning = false;
        if (networkStats.getErrorPacketRate() != null && networkStats.getErrorPacketRate() > NETWORK_ERROR_RATE_THRESHOLD) {
            networkWarning = true;
            log.warn("네트워크 에러 패킷률 임계치 초과: {}%", networkStats.getErrorPacketRate());
        }
        if (networkStats.getDropPacketRate() != null && networkStats.getDropPacketRate() > NETWORK_DROP_RATE_THRESHOLD) {
            networkWarning = true;
            log.warn("네트워크 드롭 패킷률 임계치 초과: {}%", networkStats.getDropPacketRate());
        }

        return RackStatisticsDto.WarningDetails.builder()
                .temperature(temperatureWarning)
                .humidity(humidityWarning)
                .cpu(cpuWarning)
                .memory(memoryWarning)
                .disk(diskWarning)
                .network(networkWarning)
                .build();
    }

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
                .isWarning(metric.getIsWarning())
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

    private RackStatisticsDto.SystemLoadStats calculateSystemLoadStatsOptimized(
            List<Equipment> equipments, List<Long> equipmentIds) {

        List<SystemMetric> metrics = new ArrayList<>();
        for (Long equipmentId : equipmentIds) {
            metricCache.getSystemMetric(equipmentId).ifPresent(metrics::add);
        }

        if (metrics.isEmpty()) {
            return RackStatisticsDto.SystemLoadStats.builder().equipmentCount(0).build();
        }

        // 한 번의 루프로 모든 통계 계산
        double sumLoadAvg1 = 0.0, maxLoadAvg1 = Double.MIN_VALUE;
        double sumLoadAvg5 = 0.0, maxLoadAvg5 = Double.MIN_VALUE;
        double sumLoadAvg15 = 0.0, maxLoadAvg15 = Double.MIN_VALUE;
        int countLoadAvg1 = 0, countLoadAvg5 = 0, countLoadAvg15 = 0;

        for (SystemMetric metric : metrics) {
            if (metric.getLoadAvg1() != null) {
                double val = metric.getLoadAvg1();
                sumLoadAvg1 += val;
                maxLoadAvg1 = Math.max(maxLoadAvg1, val);
                countLoadAvg1++;
            }
            if (metric.getLoadAvg5() != null) {
                double val = metric.getLoadAvg5();
                sumLoadAvg5 += val;
                maxLoadAvg5 = Math.max(maxLoadAvg5, val);
                countLoadAvg5++;
            }
            if (metric.getLoadAvg15() != null) {
                double val = metric.getLoadAvg15();
                sumLoadAvg15 += val;
                maxLoadAvg15 = Math.max(maxLoadAvg15, val);
                countLoadAvg15++;
            }
        }

        return RackStatisticsDto.SystemLoadStats.builder()
                .avgLoadAvg1(countLoadAvg1 > 0 ? sumLoadAvg1 / countLoadAvg1 : 0.0)
                .avgLoadAvg5(countLoadAvg5 > 0 ? sumLoadAvg5 / countLoadAvg5 : 0.0)
                .avgLoadAvg15(countLoadAvg15 > 0 ? sumLoadAvg15 / countLoadAvg15 : 0.0)
                .maxLoadAvg1(maxLoadAvg1 == Double.MIN_VALUE ? 0.0 : maxLoadAvg1)
                .maxLoadAvg5(maxLoadAvg5 == Double.MIN_VALUE ? 0.0 : maxLoadAvg5)
                .maxLoadAvg15(maxLoadAvg15 == Double.MIN_VALUE ? 0.0 : maxLoadAvg15)
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
                .filter(e -> e.getType() != EquipmentType.ENVIRONMENTAL_SENSOR)
                .map(Equipment::getId)
                .collect(Collectors.toList());

        List<DiskMetric> metrics = new ArrayList<>();
        for (Long equipmentId : diskEquipmentIds) {
            metricCache.getDiskMetric(equipmentId).ifPresent(metrics::add);
        }

        if (metrics.isEmpty()) {
            return RackStatisticsDto.DiskStats.builder().equipmentCount(0).build();
        }

        Map<Long, String> equipmentNameMap = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        List<DiskMetric> validMetrics = metrics.stream()
                .filter(m -> m.getUsedPercentage() != null)
                .collect(Collectors.toList());

        if (validMetrics.isEmpty()) {
            return RackStatisticsDto.DiskStats.builder().equipmentCount(0).build();
        }

        DoubleSummaryStatistics diskStats = validMetrics.stream()
                .mapToDouble(DiskMetric::getUsedPercentage)
                .summaryStatistics();

        long totalBytes = metrics.stream()
                .filter(m -> m.getTotalBytes() != null)
                .mapToLong(DiskMetric::getTotalBytes)
                .sum();

        long usedBytes = metrics.stream()
                .filter(m -> m.getUsedBytes() != null)
                .mapToLong(DiskMetric::getUsedBytes)
                .sum();

        List<RackStatisticsDto.TopEquipment> topEquipments = validMetrics.stream()
                .sorted(Comparator.comparingDouble(DiskMetric::getUsedPercentage).reversed())
                .limit(5)
                .map(m -> RackStatisticsDto.TopEquipment.builder()
                        .equipmentId(m.getEquipmentId())
                        .equipmentName(equipmentNameMap.get(m.getEquipmentId()))
                        .value(m.getUsedPercentage())
                        .build())
                .collect(Collectors.toList());


        double totalCapacityTB = totalBytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
        double usedCapacityTB = usedBytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);

        return RackStatisticsDto.DiskStats.builder()
                .avgUsage(diskStats.getAverage())
                .maxUsage(diskStats.getMax())
                .topEquipments(topEquipments)
                .equipmentCount(validMetrics.size())
                .totalCapacityTB(totalCapacityTB)
                .usedCapacityTB(usedCapacityTB)
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
                .mapToDouble(m -> m.inBytesPerSec * 8.0 / 1_000_000.0)
                .sum();

        double totalTxMbps = aggregatedMetrics.values().stream()
                .mapToDouble(m -> m.outBytesPerSec * 8.0 / 1_000_000.0)
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


        double errorPacketRate = 0.0;
        if (totalInPackets > 0) {
            errorPacketRate = (totalInErrors * 100.0 / totalInPackets);
            if (errorPacketRate > 5.0) {
                log.warn("높은 에러 패킷률 감지: {0}% (에러: {1}, 전체: {2})",
                        String.format("%.2f", errorPacketRate), totalInErrors, totalInPackets);
            } else {
                log.debug("정상 에러 패킷률: {0}% (에러: {1}, 전체: {2})",
                        String.format("%.2f", errorPacketRate), totalInErrors, totalInPackets);
            }
        }

        double dropPacketRate = 0.0;
        if (totalOutPackets > 0) {
            dropPacketRate = (totalOutErrors * 100.0 / totalOutPackets);
            if (dropPacketRate > 5.0) {
                log.warn("높은 드롭 패킷률 감지: {0}% (드롭: {1}, 전체: {2})",
                        String.format("%.2f", dropPacketRate), totalOutErrors, totalOutPackets);
            } else {
                log.debug("정상 드롭 패킷률: {0}% (드롭: {1}, 전체: {2})",
                        String.format("%.2f", dropPacketRate), totalOutErrors, totalOutPackets);
            }
        }

        Map<Long, String> equipmentNameMap = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        List<RackStatisticsDto.TopEquipment> topRxEquipments = aggregatedMetrics.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<Long, AggregatedNetworkMetric> e) ->
                        e.getValue().inBytesPerSec).reversed())
                .limit(5)
                .map(e -> RackStatisticsDto.TopEquipment.builder()
                        .equipmentId(e.getKey())
                        .equipmentName(equipmentNameMap.get(e.getKey()))
                        .value(e.getValue().inBytesPerSec * 8.0 / 1_000_000.0)
                        .build())
                .collect(Collectors.toList());

        List<RackStatisticsDto.TopEquipment> topTxEquipments = aggregatedMetrics.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<Long, AggregatedNetworkMetric> e) ->
                        e.getValue().outBytesPerSec).reversed())
                .limit(5)
                .map(e -> RackStatisticsDto.TopEquipment.builder()
                        .equipmentId(e.getKey())
                        .equipmentName(equipmentNameMap.get(e.getKey()))
                        .value(e.getValue().outBytesPerSec * 8.0 / 1_000_000.0)
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

        long inBytesPerSec = nicMetrics.stream()
                .mapToLong(m -> m.getInBytesPerSec() != null ? m.getInBytesPerSec().longValue() : 0L)
                .sum();

        long outBytesPerSec = nicMetrics.stream()
                .mapToLong(m -> m.getOutBytesPerSec() != null ? m.getOutBytesPerSec().longValue() : 0L)
                .sum();


        long inPktsTot = nicMetrics.stream()
                .mapToLong(m -> m.getInPktsTot() != null ? m.getInPktsTot() : 0L)
                .sum();

        long outPktsTot = nicMetrics.stream()
                .mapToLong(m -> m.getOutPktsTot() != null ? m.getOutPktsTot() : 0L)
                .sum();


        long inErrorPktsTot = nicMetrics.stream()
                .mapToLong(m -> m.getInErrorPktsTot() != null ? m.getInErrorPktsTot() : 0L)
                .sum();

        long inDiscardPktsTot = nicMetrics.stream()
                .mapToLong(m -> m.getInDiscardPktsTot() != null ? m.getInDiscardPktsTot() : 0L)
                .sum();

        long outErrorPktsTot = nicMetrics.stream()
                .mapToLong(m -> m.getOutErrorPktsTot() != null ? m.getOutErrorPktsTot() : 0L)
                .sum();

        long outDiscardPktsTot = nicMetrics.stream()
                .mapToLong(m -> m.getOutDiscardPktsTot() != null ? m.getOutDiscardPktsTot() : 0L)
                .sum();

        double avgRxUsage = nicMetrics.stream()
                .filter(m -> m.getRxUsage() != null)
                .mapToDouble(NetworkMetric::getRxUsage)
                .average()
                .orElse(0.0);

        double avgTxUsage = nicMetrics.stream()
                .filter(m -> m.getTxUsage() != null)
                .mapToDouble(NetworkMetric::getTxUsage)
                .average()
                .orElse(0.0);



        AggregatedNetworkMetric result = new AggregatedNetworkMetric();
        result.inBytesPerSec = inBytesPerSec;
        result.outBytesPerSec = outBytesPerSec;
        result.rxUsage = avgRxUsage;
        result.txUsage = avgTxUsage;
        result.inPktsTot = inPktsTot;
        result.outPktsTot = outPktsTot;
        result.inErrorPktsTot = inErrorPktsTot;
        result.inDiscardPktsTot = inDiscardPktsTot;
        result.outErrorPktsTot = outErrorPktsTot;
        result.outDiscardPktsTot = outDiscardPktsTot;

        return result;
    }

    private static class AggregatedNetworkMetric {
        long inBytesPerSec = 0L;
        long outBytesPerSec = 0L;
        double rxUsage = 0.0;
        double txUsage = 0.0;
        long inPktsTot = 0L;
        long outPktsTot = 0L;
        long inErrorPktsTot = 0L;
        long inDiscardPktsTot = 0L;
        long outErrorPktsTot = 0L;
        long outDiscardPktsTot = 0L;
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
                .systemLoadStats(RackStatisticsDto.SystemLoadStats.builder().equipmentCount(0).build())
                .memoryStats(RackStatisticsDto.MemoryStats.builder().equipmentCount(0).build())
                .diskStats(RackStatisticsDto.DiskStats.builder().equipmentCount(0).build())
                .networkStats(RackStatisticsDto.NetworkStats.builder().equipmentCount(0).build())
                .isWarning(false)
                .warningDetails(RackStatisticsDto.WarningDetails.builder()
                        .temperature(false)
                        .humidity(false)
                        .cpu(false)
                        .memory(false)
                        .disk(false)
                        .network(false)
                        .build())
                .build();
    }
}