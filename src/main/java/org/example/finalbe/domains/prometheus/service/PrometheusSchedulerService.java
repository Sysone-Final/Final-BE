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

    // ✅ Equipment 캐시 (알림 평가 최적화용)
    private final Map<Long, Equipment> equipmentCache = new HashMap<>();

    /**
     * ✅ fixedRate로 변경: 정확히 5초마다 실행
     * ✅ 통일된 수집 시간 사용
     * ✅ SSE 실시간 전송 추가
     * ✅ 알림 평가 최적화 (임계값 근처만 평가)
     */
    @Scheduled(fixedRateString = "${monitoring.scheduler.metrics-interval:10000}")
    public void collectMetrics() {
        if (!properties.getCollection().isEnabled()) {
            return;
        }

        try {
            log.info("📊 프로메테우스 메트릭 수집 시작...");
            long startTime = System.currentTimeMillis();

            // ✅ 통일된 수집 시간 생성
            LocalDateTime collectionTime = LocalDateTime.now();
            long timestamp = collectionTime.atZone(ZoneId.systemDefault()).toEpochSecond();

            Map<Long, MetricRawData> dataMap = initializeDataMap(timestamp);

            if (dataMap.isEmpty()) {
                List<String> instances = equipmentMappingService.getAllInstances();
                log.warn("⚠️ 매핑된 Equipment가 없습니다. 수집을 건너뜁니다.");
                log.warn("   getAllInstances() 결과 개수: {}", instances.size());
                if (!instances.isEmpty()) {
                    log.warn("   첫 5개 instances: {}", instances.stream().limit(5).collect(Collectors.toList()));
                }
                return;
            }

            log.debug("🎯 수집 대상: {} 개 장비, 통일 시간: {}", dataMap.size(), collectionTime);

            // ✅ Equipment 캐시 갱신 (알림 평가용)
            refreshEquipmentCache(dataMap.keySet());

            // 메트릭 수집 (병렬 실행)
            systemMetricCollector.collectAndPopulate(dataMap);
            diskMetricCollector.collectAndPopulate(dataMap);
            networkMetricCollector.collectAndPopulate(dataMap);
            environmentMetricCollector.collectAndPopulate(dataMap);

            // ✅ 유효성 검증 및 필터링
            List<MetricRawData> validDataList = dataMap.values().stream()
                    .filter(this::isValidMetric)
                    .collect(Collectors.toList());

            if (validDataList.isEmpty()) {
                log.warn("⚠️ 유효한 메트릭이 없습니다.");
                log.warn("   전체 수집된 데이터 개수: {}", dataMap.size());

                if (!dataMap.isEmpty()) {
                    MetricRawData sample = dataMap.values().iterator().next();
                    log.warn("   샘플 데이터 - equipmentId: {}, instance: {}",
                            sample.getEquipmentId(), sample.getInstance());
                    log.warn("   샘플 데이터 - CPU modes: {}", sample.getCpuModes());
                    log.warn("   샘플 데이터 - contextSwitches: {}", sample.getContextSwitches());
                    log.warn("   샘플 데이터 - totalMemory: {}", sample.getTotalMemory());
                }
                return;
            }

            int filteredCount = dataMap.size() - validDataList.size();
            if (filteredCount > 0) {
                log.warn("⚠️ {} 개의 무효한 메트릭 제외됨", filteredCount);
            }

            // ✅ 메트릭 변환 및 SSE 전송
            List<SystemMetric> systemMetrics = new ArrayList<>();
            List<DiskMetric> diskMetrics = new ArrayList<>();
            List<NetworkMetric> networkMetrics = new ArrayList<>();

            int alertEvaluationCount = 0;  // ✅ 실제 평가된 알림 수

            for (MetricRawData data : validDataList) {
                Long equipmentId = data.getEquipmentId();

                try {
                    Equipment equipment = equipmentCache.get(equipmentId);

                    // System 메트릭 변환 및 전송
                    SystemMetric systemMetric = convertToSystemMetric(data, collectionTime);
                    if (systemMetric != null) {
                        systemMetrics.add(systemMetric);

                        // 캐시 업데이트
                        monitoringMetricCache.updateSystemMetric(systemMetric);

                        // ✅ SSE 전송
                        sseService.sendToEquipment(equipmentId, "system", systemMetric);
                        log.debug("📡 System SSE 전송: equipmentId={}", equipmentId);

                        // ✅ 임계값 근처일 때만 알림 평가
                        if (equipment != null && needsSystemAlertEvaluation(systemMetric, equipment)) {
                            try {
                                alertEvaluationService.evaluateSystemMetric(systemMetric);
                                alertEvaluationCount++;
                            } catch (Exception e) {
                                log.warn("⚠️ System 알림 평가 실패: equipmentId={}, error={}", equipmentId, e.getMessage());
                            }
                        }
                    }

                    // Disk 메트릭 변환 및 전송
                    DiskMetric diskMetric = convertToDiskMetric(data, collectionTime);
                    if (diskMetric != null) {
                        diskMetrics.add(diskMetric);

                        // 캐시 업데이트
                        monitoringMetricCache.updateDiskMetric(diskMetric);

                        // ✅ SSE 전송
                        sseService.sendToEquipment(equipmentId, "disk", diskMetric);
                        log.debug("📡 Disk SSE 전송: equipmentId={}", equipmentId);

                        // ✅ 임계값 근처일 때만 알림 평가
                        if (equipment != null && needsDiskAlertEvaluation(diskMetric, equipment)) {
                            try {
                                alertEvaluationService.evaluateDiskMetric(diskMetric);
                                alertEvaluationCount++;
                            } catch (Exception e) {
                                log.warn("⚠️ Disk 알림 평가 실패: equipmentId={}, error={}", equipmentId, e.getMessage());
                            }
                        }
                    }

                    // Network 메트릭 변환 및 전송
                    NetworkMetric networkMetric = convertToNetworkMetric(data, collectionTime);
                    if (networkMetric != null) {
                        networkMetrics.add(networkMetric);

                        // 캐시 업데이트
                        monitoringMetricCache.updateNetworkMetric(networkMetric);

                        // ✅ SSE 전송
                        sseService.sendToEquipment(equipmentId, "network", networkMetric);
                        log.debug("📡 Network SSE 전송: equipmentId={}", equipmentId);

                        // ✅ 임계값 근처일 때만 알림 평가
                        if (equipment != null && needsNetworkAlertEvaluation(networkMetric, equipment)) {
                            try {
                                alertEvaluationService.evaluateNetworkMetric(networkMetric);
                                alertEvaluationCount++;
                            } catch (Exception e) {
                                log.warn("⚠️ Network 알림 평가 실패: equipmentId={}, error={}", equipmentId, e.getMessage());
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("❌ 메트릭 처리 실패: equipmentId={}", equipmentId, e);
                }
            }

            // DB 저장 (비동기 - 백그라운드)
            CompletableFuture.runAsync(() -> {
                try {
                    if (!systemMetrics.isEmpty()) {
                        systemMetricRepository.saveAll(systemMetrics);
                    }
                    if (!diskMetrics.isEmpty()) {
                        diskMetricRepository.saveAll(diskMetrics);
                    }
                    if (!networkMetrics.isEmpty()) {
                        networkMetricRepository.saveAll(networkMetrics);
                    }
                    log.debug("💾 DB 저장 완료 (백그라운드): System={}, Disk={}, Network={}",
                            systemMetrics.size(), diskMetrics.size(), networkMetrics.size());
                } catch (Exception e) {
                    log.error("❌ DB 저장 중 오류", e);
                }
            });

            // ✅ 전체 메트릭 스트림 전송 (기존 SSE - 모든 구독자에게)
            if (sseEmitterService.getActiveConnectionCount() > 0) {
                List<MetricStreamDto> streamData = validDataList.stream()
                        .map(MetricStreamDto::from)
                        .collect(Collectors.toList());

                sseEmitterService.sendToAll("metrics", streamData);
                log.debug("📤 SSE 전체 전송 완료: {} 개 장비 데이터", streamData.size());
            }

            long elapsed = System.currentTimeMillis() - startTime;

            int totalMetrics = systemMetrics.size() + diskMetrics.size() + networkMetrics.size();

            log.info("✅ 메트릭 수집 완료: {} 개 장비 (유효: {}, 제외: {}), {}ms 소요",
                    dataMap.size(), validDataList.size(), filteredCount, elapsed);
            log.info("  - System 메트릭: {}", systemMetrics.size());
            log.info("  - Disk 메트릭: {}", diskMetrics.size());
            log.info("  - Network 메트릭: {}", networkMetrics.size());

            if (totalMetrics > 0) {
                log.info("  ✅ 알림 평가 실행: {} 건 (전체 메트릭의 {}%)",
                        alertEvaluationCount,
                        String.format("%.1f", alertEvaluationCount * 100.0 / totalMetrics));
            }

        } catch (Exception e) {
            log.error("❌ 메트릭 수집 중 오류 발생", e);
        }
    }

    // ========== ✅ 새로 추가: Equipment 캐시 관리 ==========

    /**
     * Equipment 캐시 갱신
     */
    private void refreshEquipmentCache(Set<Long> equipmentIds) {
        try {
            List<Equipment> equipments = equipmentRepository.findAllById(equipmentIds);
            equipmentCache.clear();
            equipments.forEach(eq -> equipmentCache.put(eq.getId(), eq));
            log.debug("🔄 Equipment 캐시 갱신: {} 개", equipmentCache.size());
        } catch (Exception e) {
            log.warn("⚠️ Equipment 캐시 갱신 실패", e);
        }
    }


    private boolean needsSystemAlertEvaluation(SystemMetric metric, Equipment equipment) {
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }

        // ✅ 항상 평가 (최적화 로직 제거)
        return equipment.getCpuThresholdWarning() != null ||
                equipment.getMemoryThresholdWarning() != null;
    }

    private boolean needsDiskAlertEvaluation(DiskMetric metric, Equipment equipment) {
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }

        // ✅ 항상 평가
        return equipment.getDiskThresholdWarning() != null;
    }


    /**
     * ✅ MetricRawData → SystemMetric 변환
     */
    private SystemMetric convertToSystemMetric(MetricRawData data, LocalDateTime generateTime) {
        Map<String, Double> cpuModes = data.getCpuModes();
        if (cpuModes == null || cpuModes.isEmpty()) {
            return null;
        }

        Long totalMemory = data.getTotalMemory();
        Long availableMemory = data.getAvailableMemory();
        Long usedMemory = (totalMemory != null && availableMemory != null)
                ? (totalMemory - availableMemory)
                : null;

        Double memoryUsagePercent = (totalMemory != null && totalMemory > 0 && usedMemory != null)
                ? ((usedMemory * 100.0) / totalMemory)
                : null;

        Long totalSwap = data.getTotalSwap();
        Long usedSwap = data.getUsedSwap() != null ? data.getUsedSwap() : 0L;
        Double usedSwapPercentage = (totalSwap != null && totalSwap > 0)
                ? (usedSwap * 100.0 / totalSwap)
                : 0.0;

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

    /**
     * ✅ MetricRawData → DiskMetric 변환
     */
    private DiskMetric convertToDiskMetric(MetricRawData data, LocalDateTime generateTime) {
        Long totalDisk = data.getTotalDisk();
        Long usedDisk = data.getUsedDisk();
        Long freeDisk = data.getFreeDisk();

        if (totalDisk == null || totalDisk == 0) {
            return null;
        }

        // usedDisk가 없으면 계산
        if (usedDisk == null && freeDisk != null) {
            usedDisk = totalDisk - freeDisk;
        }

        // freeDisk가 없으면 계산
        if (freeDisk == null && usedDisk != null) {
            freeDisk = totalDisk - usedDisk;
        }

        // usedInodes 계산
        Long totalInodes = data.getTotalInodes();
        Long freeInodes = data.getFreeInodes();
        Long usedInodes = (totalInodes != null && freeInodes != null)
                ? (totalInodes - freeInodes)
                : null;

        Double usedPercentage = (usedDisk != null && totalDisk > 0)
                ? (usedDisk * 100.0 / totalDisk)
                : 0.0;

        Double usedInodePercentage = (usedInodes != null && totalInodes != null && totalInodes > 0)
                ? (usedInodes * 100.0 / totalInodes)
                : null;

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

    /**
     * ✅ MetricRawData → NetworkMetric 변환 (단일 NIC 데이터)
     */
    private NetworkMetric convertToNetworkMetric(MetricRawData data, LocalDateTime generateTime) {
        // Network 데이터가 없으면 null 반환
        if (data.getNetworkRxBps() == null && data.getNetworkTxBps() == null) {
            return null;
        }

        return NetworkMetric.builder()
                .equipmentId(data.getEquipmentId())
                .generateTime(generateTime)
                .nicName("eth0")  // 기본 NIC 이름 (실제로는 Collector에서 설정해야 함)
                .operStatus(data.getNetworkOperStatus())
                .inBytesTot(data.getNetworkRxBytesTotal())
                .outBytesTot(data.getNetworkTxBytesTotal())
                .inBytesPerSec(data.getNetworkRxBps())
                .outBytesPerSec(data.getNetworkTxBps())
                .inPktsTot(data.getNetworkRxPacketsTotal())
                .outPktsTot(data.getNetworkTxPacketsTotal())
                .inPktsPerSec(data.getNetworkRxPps())
                .outPktsPerSec(data.getNetworkTxPps())
                .inErrorPktsTot(data.getNetworkRxErrors())
                .outErrorPktsTot(data.getNetworkTxErrors())
                .inDiscardPktsTot(data.getNetworkRxDrops())
                .outDiscardPktsTot(data.getNetworkTxDrops())
                .rxUsage(null)  // 계산 필요 시 추가
                .txUsage(null)  // 계산 필요 시 추가
                .build();
    }

    /**
     * ✅ 통일된 타임스탬프를 사용하여 초기화
     */
    private Map<Long, MetricRawData> initializeDataMap(long timestamp) {
        Map<Long, MetricRawData> dataMap = new HashMap<>();

        List<String> instances = equipmentMappingService.getAllInstances();

        for (String instance : instances) {
            equipmentMappingService.getEquipmentId(instance).ifPresent(equipmentId -> {
                MetricRawData data = MetricRawData.builder()
                        .equipmentId(equipmentId)
                        .instance(instance)
                        .timestamp(timestamp)
                        .cpuModes(new HashMap<>())
                        .build();
                dataMap.put(equipmentId, data);
            });
        }

        return dataMap;
    }

    /**
     * ✅ 메트릭 유효성 검증
     */
    private boolean isValidMetric(MetricRawData data) {
        // 1. CPU 메트릭이 모두 0이면 무효
        Map<String, Double> cpuModes = data.getCpuModes();
        if (cpuModes == null || cpuModes.isEmpty() ||
                cpuModes.values().stream().allMatch(v -> v == null || v == 0.0)) {
            log.debug("⚠️ 무효 메트릭: equipmentId={} - CPU 값 없음", data.getEquipmentId());
            return false;
        }

        // 2. context_switches가 null이면 무효
        if (data.getContextSwitches() == null) {
            log.debug("⚠️ 무효 메트릭: equipmentId={} - contextSwitches null", data.getEquipmentId());
            return false;
        }

        // 3. 메모리 정보가 없으면 무효
        if (data.getTotalMemory() == null || data.getTotalMemory() == 0) {
            log.debug("⚠️ 무효 메트릭: equipmentId={} - 메모리 정보 없음", data.getEquipmentId());
            return false;
        }

        // 4. CPU 합계가 비정상적이면 무효 (일부 예외 허용)
        double totalCpu = cpuModes.values().stream()
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalCpu > 110.0) {
            log.warn("⚠️ 무효 메트릭: equipmentId={} - CPU 합계 {}% (비정상)",
                    data.getEquipmentId(), totalCpu);
            return false;
        }

        return true;
    }

    /**
     * Network 메트릭 알림 평가 필요 여부 체크
     */
    private boolean needsNetworkAlertEvaluation(NetworkMetric metric, Equipment equipment) {
        if (equipment == null || !Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }

        double bandwidthWarning = 80.0;

        // RX/TX 사용률 체크
        if (metric.getRxUsage() != null && metric.getRxUsage() >= bandwidthWarning * 0.8) {
            return true;
        }
        if (metric.getTxUsage() != null && metric.getTxUsage() >= bandwidthWarning * 0.8) {
            return true;
        }

        // 에러율 체크
        if (metric.getInErrorPktsTot() != null && metric.getInPktsTot() != null &&
                metric.getInPktsTot() > 0) {
            double errorRate = (metric.getInErrorPktsTot() * 100.0) / metric.getInPktsTot();
            if (errorRate >= 0.08) {
                return true;
            }
        }

        if (metric.getOutErrorPktsTot() != null && metric.getOutPktsTot() != null &&
                metric.getOutPktsTot() > 0) {
            double errorRate = (metric.getOutErrorPktsTot() * 100.0) / metric.getOutPktsTot();
            if (errorRate >= 0.08) {
                return true;
            }
        }

        // 드롭율 체크
        if (metric.getInDiscardPktsTot() != null && metric.getInPktsTot() != null &&
                metric.getInPktsTot() > 0) {
            double dropRate = (metric.getInDiscardPktsTot() * 100.0) / metric.getInPktsTot();
            if (dropRate >= 0.08) {
                return true;
            }
        }

        if (metric.getOutDiscardPktsTot() != null && metric.getOutPktsTot() != null &&
                metric.getOutPktsTot() > 0) {
            double dropRate = (metric.getOutDiscardPktsTot() * 100.0) / metric.getOutPktsTot();
            if (dropRate >= 0.08) {
                return true;
            }
        }

        return false;
    }
}