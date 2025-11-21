package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.service.AlertEvaluationService;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.prometheus.config.PrometheusProperties;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.example.finalbe.domains.prometheus.dto.MetricStreamDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * ✅ fixedRate로 변경: 정확히 5초마다 실행 (이전 작업 완료 여부 무관)
     * ✅ 통일된 수집 시간 사용
     */
    @Scheduled(fixedRate = 5000, initialDelay = 1000)
    public void collectMetrics() {
        if (!properties.getCollection().isEnabled()) {
            return;
        }

        try {
            log.info("📊 프로메테우스 메트릭 수집 시작...");
            long startTime = System.currentTimeMillis();

            // ✅ 통일된 수집 시간 생성 (모든 데이터가 동일한 시간 사용)
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

                // 샘플 데이터 1개 출력 (디버깅용)
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

            // DB 저장 (유효한 데이터만)
            systemMetricCollector.saveMetrics(validDataList);
            diskMetricCollector.saveMetrics(validDataList);
            networkMetricCollector.saveMetrics(validDataList);
            environmentMetricCollector.saveMetrics(validDataList);

            // ✅ 알림 평가 추가!
            for (MetricRawData data : validDataList) {
                try {
                    evaluateMetricsForAlert(data, collectionTime);
                } catch (Exception e) {
                    log.error("❌ 알림 평가 실패: equipmentId={}", data.getEquipmentId(), e);
                }
            }

            // SSE로 실시간 전송
            if (sseEmitterService.getActiveConnectionCount() > 0) {
                List<MetricStreamDto> streamData = validDataList.stream()
                        .map(MetricStreamDto::from)
                        .collect(Collectors.toList());

                sseEmitterService.sendToAll("metrics", streamData);
                log.debug("📤 SSE 전송 완료: {} 개 장비 데이터", streamData.size());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 메트릭 수집 완료: {} 개 장비 (유효: {}, 제외: {}), {}ms 소요",
                    dataMap.size(), validDataList.size(), filteredCount, elapsed);

        } catch (Exception e) {
            log.error("❌ 메트릭 수집 중 오류 발생", e);
        }
    }

    /**
     * ✅ 메트릭 데이터를 SystemMetric, DiskMetric으로 변환하여 알림 평가
     */
    private void evaluateMetricsForAlert(MetricRawData data, LocalDateTime generateTime) {
        // System 메트릭 변환 및 평가
        if (data.getCpuModes() != null && !data.getCpuModes().isEmpty()) {
            SystemMetric systemMetric = convertToSystemMetric(data, generateTime);
            alertEvaluationService.evaluateSystemMetric(systemMetric);
        }

        // Disk 메트릭 변환 및 평가
        if (data.getTotalDisk() != null && data.getUsedDisk() != null) {
            DiskMetric diskMetric = convertToDiskMetric(data, generateTime);
            alertEvaluationService.evaluateDiskMetric(diskMetric);
        }
    }

    /**
     * ✅ MetricRawData → SystemMetric 변환
     */
    private SystemMetric convertToSystemMetric(MetricRawData data, LocalDateTime generateTime) {
        Map<String, Double> cpuModes = data.getCpuModes();

        // CPU Idle 계산
        Double cpuIdle = cpuModes.getOrDefault("idle", 0.0);

        // 메모리 계산
        Long totalMemory = data.getTotalMemory();
        Long availableMemory = data.getAvailableMemory();
        Long usedMemory = (totalMemory != null && availableMemory != null)
                ? (totalMemory - availableMemory)
                : 0L;

        Double memoryUsagePercent = (totalMemory != null && totalMemory > 0)
                ? ((usedMemory * 100.0) / totalMemory)
                : 0.0;

        return SystemMetric.builder()
                .equipmentId(data.getEquipmentId())
                .generateTime(generateTime)
                .cpuIdle(cpuIdle)
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
                .totalSwap(data.getTotalSwap())
                .usedSwap(data.getUsedSwap())
                .usedSwapPercentage(
                        (data.getTotalSwap() != null && data.getTotalSwap() > 0)
                                ? (data.getUsedSwap() * 100.0 / data.getTotalSwap())
                                : 0.0
                )
                .build();
    }

    /**
     * ✅ MetricRawData → DiskMetric 변환
     */
    private DiskMetric convertToDiskMetric(MetricRawData data, LocalDateTime generateTime) {
        Long totalDisk = data.getTotalDisk();
        Long usedDisk = data.getUsedDisk();

        return DiskMetric.builder()
                .equipmentId(data.getEquipmentId())
                .generateTime(generateTime)
                .totalBytes(totalDisk)
                .usedBytes(usedDisk)
                .freeBytes(totalDisk - usedDisk)
                .usedPercentage(
                        (totalDisk != null && totalDisk > 0)
                                ? (usedDisk * 100.0 / totalDisk)
                                : 0.0
                )
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
        if (cpuModes.isEmpty() ||
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
}