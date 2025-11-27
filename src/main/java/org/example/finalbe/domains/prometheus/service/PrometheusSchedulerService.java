/**
 * 작성자: 황요한
 * Prometheus 메트릭을 주기적으로 수집/변환/저장/SSE 전송하는 스케줄러 서비스
 */
package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.service.AlertEvaluationService;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.monitoring.service.MonitoringMetricCache;
import org.example.finalbe.domains.monitoring.service.SseService;
import org.example.finalbe.domains.prometheus.config.PrometheusProperties;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.example.finalbe.domains.prometheus.dto.MetricStreamDto;
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "prometheus.collection", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PrometheusSchedulerService {

    private final PrometheusProperties properties;
    private final EquipmentMappingService equipmentMappingService;
    private final SystemMetricCollectorService systemMetricCollector;
    private final DiskMetricCollectorService diskMetricCollector;
    private final NetworkMetricCollectorService networkMetricCollector;
    private final EnvironmentMetricCollectorService environmentMetricCollector;
    private final SseEmitterService sseEmitterService;
    private final AlertEvaluationService alertEvaluationService;
    private final MonitoringMetricCache monitoringMetricCache;
    private final SseService sseService;
    private final SystemMetricRepository systemMetricRepository;
    private final DiskMetricRepository diskMetricRepository;
    private final NetworkMetricRepository networkMetricRepository;
    private final EquipmentRepository equipmentRepository;

    private final Map<Long, Equipment> equipmentCache = new HashMap<>();

    // 메트릭 수집 스케줄 실행
    @Scheduled(fixedRateString = "${monitoring.scheduler.metrics-interval:10000}")
    public void collectMetrics() {
        if (!properties.getCollection().isEnabled()) return;

        try {
            long startTime = System.currentTimeMillis();
            LocalDateTime collectionTime = LocalDateTime.now();
            long timestamp = collectionTime.atZone(ZoneId.systemDefault()).toEpochSecond();

            Map<Long, MetricRawData> dataMap = initializeDataMap(timestamp);
            if (dataMap.isEmpty()) return;

            refreshEquipmentCache(dataMap.keySet());

            systemMetricCollector.collectAndPopulate(dataMap);
            diskMetricCollector.collectAndPopulate(dataMap);
            networkMetricCollector.collectAndPopulate(dataMap);
            environmentMetricCollector.collectAndPopulate(dataMap);

            List<MetricRawData> validDataList = dataMap.values().stream()
                    .filter(this::isValidMetric)
                    .collect(Collectors.toList());

            List<SystemMetric> systemMetrics = new ArrayList<>();
            List<DiskMetric> diskMetrics = new ArrayList<>();
            List<NetworkMetric> networkMetrics = new ArrayList<>();

            int alertEvaluationCount = 0;

            for (MetricRawData data : validDataList) {
                Long equipmentId = data.getEquipmentId();
                Equipment equipment = equipmentCache.get(equipmentId);

                SystemMetric systemMetric = convertToSystemMetric(data, collectionTime);
                if (systemMetric != null) {
                    systemMetrics.add(systemMetric);
                    monitoringMetricCache.updateSystemMetric(systemMetric);
                    sseService.sendToEquipment(equipmentId, "system", systemMetric);
                    if (equipment != null && needsSystemAlertEvaluation(systemMetric, equipment)) {
                        alertEvaluationService.evaluateSystemMetric(systemMetric);
                        alertEvaluationCount++;
                    }
                }

                DiskMetric diskMetric = convertToDiskMetric(data, collectionTime);
                if (diskMetric != null) {
                    diskMetrics.add(diskMetric);
                    monitoringMetricCache.updateDiskMetric(diskMetric);
                    sseService.sendToEquipment(equipmentId, "disk", diskMetric);
                    if (equipment != null && needsDiskAlertEvaluation(diskMetric, equipment)) {
                        alertEvaluationService.evaluateDiskMetric(diskMetric);
                        alertEvaluationCount++;
                    }
                }

                NetworkMetric networkMetric = convertToNetworkMetric(data, collectionTime);
                if (networkMetric != null) {
                    networkMetrics.add(networkMetric);
                    monitoringMetricCache.updateNetworkMetric(networkMetric);
                    sseService.sendToEquipment(equipmentId, "network", networkMetric);
                    if (equipment != null && needsNetworkAlertEvaluation(networkMetric, equipment)) {
                        alertEvaluationService.evaluateNetworkMetric(networkMetric);
                        alertEvaluationCount++;
                    }
                }
            }

            CompletableFuture.runAsync(() -> {
                if (!systemMetrics.isEmpty()) systemMetricRepository.saveAll(systemMetrics);
                if (!diskMetrics.isEmpty()) diskMetricRepository.saveAll(diskMetrics);
                if (!networkMetrics.isEmpty()) networkMetricRepository.saveAll(networkMetrics);
            });

            if (sseEmitterService.getActiveConnectionCount() > 0) {
                List<MetricStreamDto> streamData = validDataList.stream()
                        .map(MetricStreamDto::from)
                        .collect(Collectors.toList());
                sseEmitterService.sendToAll("metrics", streamData);
            }

            long elapsed = System.currentTimeMillis() - startTime;

            log.info("메트릭 수집 완료: {}ms, 평가된 알림: {}", elapsed, alertEvaluationCount);

        } catch (Exception e) {
            log.error("메트릭 수집 오류", e);
        }
    }

    // Equipment 캐시 갱신
    private void refreshEquipmentCache(Set<Long> equipmentIds) {
        try {
            List<Equipment> equipments = equipmentRepository.findAllById(equipmentIds);
            equipmentCache.clear();
            equipments.forEach(eq -> equipmentCache.put(eq.getId(), eq));
        } catch (Exception e) {
            log.warn("Equipment 캐시 갱신 실패", e);
        }
    }

    // System 알림 평가 필요 여부
    private boolean needsSystemAlertEvaluation(SystemMetric metric, Equipment equipment) {
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) return false;
        return equipment.getCpuThresholdWarning() != null ||
                equipment.getMemoryThresholdWarning() != null;
    }

    // Disk 알림 평가 필요 여부
    private boolean needsDiskAlertEvaluation(DiskMetric metric, Equipment equipment) {
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) return false;
        return equipment.getDiskThresholdWarning() != null;
    }

    // RawData → SystemMetric 변환
    private SystemMetric convertToSystemMetric(MetricRawData data, LocalDateTime generateTime) {
        Map<String, Double> cpuModes = data.getCpuModes();
        if (cpuModes == null || cpuModes.isEmpty()) return null;

        Long totalMemory = data.getTotalMemory();
        Long availableMemory = data.getAvailableMemory();
        Long usedMemory = (totalMemory != null && availableMemory != null)
                ? (totalMemory - availableMemory) : null;

        Double memoryUsagePercent = (totalMemory != null && totalMemory > 0 && usedMemory != null)
                ? ((usedMemory * 100.0) / totalMemory) : null;

        Long totalSwap = data.getTotalSwap();
        Long usedSwap = data.getUsedSwap() != null ? data.getUsedSwap() : 0L;
        Double usedSwapPercentage = (totalSwap != null && totalSwap > 0)
                ? (usedSwap * 100.0 / totalSwap) : 0.0;

        return SystemMetric.builder()
                .equipmentId(data.getEquipmentId())
                .generateTime(generateTime)
                .cpuIdle(cpuModes.getOrDefault("idle", 0.0))
                .cpuUser(cpuModes.getOrDefault("user", 0.0))
                .cpuSystem(cpuModes.getOrDefault("system", 0.0))
                .cpuWait(cpuModes.getOrDefault("iowait", 0.0))
                .cpuNice(cpuModes.getOrDefault("nice", 0.0))
                .cpuIrq(cpuModes.getOrDefault("irq", 0.0))
                .cpuSoftirq(cpuModes.getOrDefault("softirq", 0.0))
                .cpuSteal(cpuModes.getOrDefault("steal", 0.0))
                .loadAvg1(data.getLoadAvg1())
                .loadAvg5(data.getLoadAvg5())
                .loadAvg15(data.getLoadAvg15())
                .contextSwitches(data.getContextSwitches())
                .totalMemory(totalMemory)
                .usedMemory(usedMemory)
                .freeMemory(data.getFreeMemory())
                .usedMemoryPercentage(memoryUsagePercent)
                .memoryBuffers(data.getMemoryBuffers())
                .memoryCached(data.getMemoryCached())
                .memoryActive(data.getMemoryActive())
                .memoryInactive(data.getMemoryInactive())
                .totalSwap(totalSwap)
                .usedSwap(usedSwap)
                .usedSwapPercentage(usedSwapPercentage)
                .build();
    }

    // RawData → DiskMetric 변환
    private DiskMetric convertToDiskMetric(MetricRawData data, LocalDateTime generateTime) {
        Long totalDisk = data.getTotalDisk();
        Long usedDisk = data.getUsedDisk();
        Long freeDisk = data.getFreeDisk();

        if (totalDisk == null || totalDisk == 0) return null;

        if (usedDisk == null && freeDisk != null) usedDisk = totalDisk - freeDisk;
        if (freeDisk == null && usedDisk != null) freeDisk = totalDisk - usedDisk;

        Long totalInodes = data.getTotalInodes();
        Long freeInodes = data.getFreeInodes();
        Long usedInodes = (totalInodes != null && freeInodes != null)
                ? (totalInodes - freeInodes) : null;

        Double usedPercentage = (usedDisk != null && totalDisk > 0)
                ? (usedDisk * 100.0 / totalDisk) : 0.0;

        Double usedInodePercentage = (usedInodes != null && totalInodes != null && totalInodes > 0)
                ? (usedInodes * 100.0 / totalInodes) : null;

        return DiskMetric.builder()
                .equipmentId(data.getEquipmentId())
                .generateTime(generateTime)
                .totalBytes(totalDisk)
                .usedBytes(usedDisk)
                .freeBytes(freeDisk)
                .usedPercentage(usedPercentage)
                .totalInodes(totalInodes)
                .freeInodes(freeInodes)
                .usedInodes(usedInodes)
                .usedInodePercentage(usedInodePercentage)
                .ioReadBps(data.getDiskReadBps())
                .ioWriteBps(data.getDiskWriteBps())
                .ioReadCount(data.getDiskReadCount())
                .ioWriteCount(data.getDiskWriteCount())
                .ioTimePercentage(data.getDiskIoTimePercentage())
                .build();
    }

    // RawData → NetworkMetric 변환
    private NetworkMetric convertToNetworkMetric(MetricRawData data, LocalDateTime generateTime) {
        if (data.getNetworkRxBps() == null && data.getNetworkTxBps() == null) return null;
        Equipment equipment = equipmentCache.get(data.getEquipmentId());
        return networkMetricCollector.convertToNetworkMetric(data, generateTime, equipment);
    }

    // MetricRawData 초기화
    private Map<Long, MetricRawData> initializeDataMap(long timestamp) {
        Map<Long, MetricRawData> dataMap = new HashMap<>();
        List<String> instances = equipmentMappingService.getAllInstances();

        for (String instance : instances) {
            Optional<Long> equipmentId = equipmentMappingService.getEquipmentId(instance);
            equipmentId.ifPresent(id -> {
                MetricRawData data = new MetricRawData();
                data.setEquipmentId(id);
                data.setInstance(instance);
                data.setTimestamp(timestamp);
                dataMap.put(id, data);
            });
        }

        return dataMap;
    }

    // 메트릭 유효성 검증
    private boolean isValidMetric(MetricRawData data) {
        Map<String, Double> cpuModes = data.getCpuModes();
        if (cpuModes == null || cpuModes.isEmpty() ||
                cpuModes.values().stream().allMatch(v -> v == null || v == 0.0)) return false;
        if (data.getContextSwitches() == null) return false;
        if (data.getTotalMemory() == null || data.getTotalMemory() == 0) return false;

        double totalCpu = cpuModes.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        return totalCpu <= 110.0;
    }

    // Network 알림 평가 필요 여부
    private boolean needsNetworkAlertEvaluation(NetworkMetric metric, Equipment equipment) {
        if (equipment == null || !Boolean.TRUE.equals(equipment.getMonitoringEnabled())) return false;

        double bandwidthWarning = 80.0;

        if (metric.getRxUsage() != null && metric.getRxUsage() >= bandwidthWarning * 0.8) return true;
        if (metric.getTxUsage() != null && metric.getTxUsage() >= bandwidthWarning * 0.8) return true;

        if (metric.getInErrorPktsTot() != null && metric.getInPktsTot() != null && metric.getInPktsTot() > 0) {
            double rate = (metric.getInErrorPktsTot() * 100.0) / metric.getInPktsTot();
            if (rate >= 0.08) return true;
        }

        if (metric.getOutErrorPktsTot() != null && metric.getOutPktsTot() != null && metric.getOutPktsTot() > 0) {
            double rate = (metric.getOutErrorPktsTot() * 100.0) / metric.getOutPktsTot();
            if (rate >= 0.08) return true;
        }

        if (metric.getInDiscardPktsTot() != null && metric.getInPktsTot() != null && metric.getInPktsTot() > 0) {
            double rate = (metric.getInDiscardPktsTot() * 100.0) / metric.getInPktsTot();
            if (rate >= 0.08) return true;
        }

        if (metric.getOutDiscardPktsTot() != null && metric.getOutPktsTot() != null && metric.getOutPktsTot() > 0) {
            double rate = (metric.getOutDiscardPktsTot() * 100.0) / metric.getOutPktsTot();
            if (rate >= 0.08) return true;
        }

        return false;
    }
}
