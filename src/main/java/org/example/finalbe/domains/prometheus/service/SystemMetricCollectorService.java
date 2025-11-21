package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.example.finalbe.domains.prometheus.dto.PrometheusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemMetricCollectorService {

    private final PrometheusQueryService prometheusQuery;
    private final SystemMetricRepository systemMetricRepository;

    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        try {
            collectCpuMetrics(dataMap);
            collectMemoryMetrics(dataMap);
            collectLoadAverage(dataMap);
            collectContextSwitches(dataMap);
        } catch (Exception e) {
            log.error("❌ SystemMetric 수집 중 오류", e);
        }
    }

    private void collectCpuMetrics(Map<Long, MetricRawData> dataMap) {
        String query = "avg by (instance, mode) (rate(node_cpu_seconds_total[15s]))";

        log.info("🔍 CPU 메트릭 쿼리 실행: {}", query);  // ← 추가

        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        log.info("📊 CPU 메트릭 쿼리 결과: {} 개", results.size());  // ← 추가

        if (results.isEmpty()) {
            log.warn("⚠️ CPU 메트릭 쿼리 결과가 비어있습니다!");  // ← 추가
            return;
        }

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            String mode = result.getMode();
            Double value = result.getValue();

            log.debug("  결과: instance={}, mode={}, value={}", instance, mode, value);  // ← 추가

            if (instance != null && mode != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.getCpuModes().put(mode, value * 100);
                    log.debug("    ✓ 데이터 추가됨");  // ← 추가
                } else {
                    log.warn("    ✗ dataMap에서 instance를 찾을 수 없음: {}", instance);  // ← 추가
                }
            }
        }
    }

    private void collectMemoryMetrics(Map<Long, MetricRawData> dataMap) {
        collectMemoryMetric(dataMap, "node_memory_MemTotal_bytes", MetricRawData::setTotalMemory);
        collectMemoryMetric(dataMap, "node_memory_MemFree_bytes", MetricRawData::setFreeMemory);
        collectMemoryMetric(dataMap, "node_memory_MemAvailable_bytes", MetricRawData::setAvailableMemory);
        collectMemoryMetric(dataMap, "node_memory_Buffers_bytes", MetricRawData::setMemoryBuffers);
        collectMemoryMetric(dataMap, "node_memory_Cached_bytes", MetricRawData::setMemoryCached);
        collectMemoryMetric(dataMap, "node_memory_Active_bytes", MetricRawData::setMemoryActive);
        collectMemoryMetric(dataMap, "node_memory_Inactive_bytes", MetricRawData::setMemoryInactive);
        collectMemoryMetric(dataMap, "node_memory_SwapTotal_bytes", MetricRawData::setTotalSwap);
        collectMemoryMetric(dataMap, "node_memory_SwapUsed_bytes", MetricRawData::setUsedSwap);
    }
    private void collectMemoryMetric(Map<Long, MetricRawData> dataMap, String metric,
                                     java.util.function.BiConsumer<MetricRawData, Long> setter) {
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(metric);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            // ✅ null 체크만 수행 (0도 유효한 값)
            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    setter.accept(data, value.longValue());
                }
            }
        }
    }

    private void collectLoadAverage(Map<Long, MetricRawData> dataMap) {
        collectLoadMetric(dataMap, "node_load1", MetricRawData::setLoadAvg1);
        collectLoadMetric(dataMap, "node_load5", MetricRawData::setLoadAvg5);
        collectLoadMetric(dataMap, "node_load15", MetricRawData::setLoadAvg15);
    }

    private void collectLoadMetric(Map<Long, MetricRawData> dataMap, String metric,
                                   java.util.function.BiConsumer<MetricRawData, Double> setter) {
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(metric);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    setter.accept(data, value);
                }
            }
        }
    }

    private void collectContextSwitches(Map<Long, MetricRawData> dataMap) {
        String query = "rate(node_context_switches_total[15m])";
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();


            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setContextSwitches(value.longValue());
                }
            }
        }
    }

    private MetricRawData findDataByInstance(Map<Long, MetricRawData> dataMap, String instance) {
        return dataMap.values().stream()
                .filter(d -> instance.equals(d.getInstance()))
                .findFirst()
                .orElse(null);
    }

    public void saveMetrics(List<MetricRawData> dataList) {
        int successCount = 0;
        int failureCount = 0;

        for (MetricRawData data : dataList) {
            try {
                saveMetricWithNewTransaction(data);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("❌ SystemMetric 저장 실패: equipmentId={} - {}",
                        data.getEquipmentId(), e.getMessage());
            }
        }

        if (failureCount > 0) {
            log.warn("⚠️ SystemMetric 저장 완료: 성공={}, 실패={}", successCount, failureCount);
        } else {
            log.debug("✅ SystemMetric 저장 완료: {} 건", successCount);
        }
    }

    @Transactional
    public void saveMetricWithNewTransaction(MetricRawData data) {
        LocalDateTime generateTime = data.getTimestamp() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();

        SystemMetric metric = convertToEntity(data, generateTime);

        SystemMetric existing = systemMetricRepository
                .findByEquipmentIdAndGenerateTime(data.getEquipmentId(), generateTime)
                .orElse(null);

        if (existing != null) {
            updateExisting(existing, metric);
            systemMetricRepository.save(existing);
        } else {
            systemMetricRepository.save(metric);
        }

        log.debug("  ✓ SystemMetric 저장: equipmentId={}, time={}",
                data.getEquipmentId(), generateTime);
    }

    private SystemMetric convertToEntity(MetricRawData data, LocalDateTime generateTime) {
        Map<String, Double> cpuModes = data.getCpuModes();

        Long totalMemory = data.getTotalMemory();
        Long freeMemory = data.getFreeMemory();
        Long availableMemory = data.getAvailableMemory();

        Long usedMemory = (totalMemory != null && availableMemory != null)
                ? totalMemory - availableMemory : null;

        Double usedMemoryPercentage = (totalMemory != null && totalMemory > 0 && usedMemory != null)
                ? (usedMemory * 100.0 / totalMemory) : null;

        Long swapTotal = data.getTotalSwap();
        Long swapUsed = data.getUsedSwap();

        // swapUsed가 null일 경우를 대비한 안전한 처리
        Double swapUsedPercentage = (swapTotal != null && swapTotal > 0 && swapUsed != null)
                ? (swapUsed * 100.0 / swapTotal) : null;

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
                .totalMemory(data.getTotalMemory())
                .usedMemory(usedMemory)
                .freeMemory(data.getFreeMemory())
                .usedMemoryPercentage(usedMemoryPercentage)
                .memoryBuffers(data.getMemoryBuffers())
                .memoryCached(data.getMemoryCached())
                .memoryActive(data.getMemoryActive())
                .memoryInactive(data.getMemoryInactive())
                .totalSwap(swapTotal)
                .usedSwap(swapUsed)
                .usedSwapPercentage(swapUsedPercentage)
                .build();
    }

    private void updateExisting(SystemMetric existing, SystemMetric newMetric) {
        existing.setCpuIdle(newMetric.getCpuIdle());
        existing.setCpuUser(newMetric.getCpuUser());
        existing.setCpuSystem(newMetric.getCpuSystem());
        existing.setCpuWait(newMetric.getCpuWait());
        existing.setCpuNice(newMetric.getCpuNice());
        existing.setCpuIrq(newMetric.getCpuIrq());
        existing.setCpuSoftirq(newMetric.getCpuSoftirq());
        existing.setCpuSteal(newMetric.getCpuSteal());
        existing.setLoadAvg1(newMetric.getLoadAvg1());
        existing.setLoadAvg5(newMetric.getLoadAvg5());
        existing.setLoadAvg15(newMetric.getLoadAvg15());
        existing.setContextSwitches(newMetric.getContextSwitches());
        existing.setTotalMemory(newMetric.getTotalMemory());
        existing.setUsedMemory(newMetric.getUsedMemory());
        existing.setFreeMemory(newMetric.getFreeMemory());
        existing.setUsedMemoryPercentage(newMetric.getUsedMemoryPercentage());
        existing.setMemoryBuffers(newMetric.getMemoryBuffers());
        existing.setMemoryCached(newMetric.getMemoryCached());
        existing.setMemoryActive(newMetric.getMemoryActive());
        existing.setMemoryInactive(newMetric.getMemoryInactive());
        existing.setTotalSwap(newMetric.getTotalSwap());
        existing.setUsedSwap(newMetric.getUsedSwap());
        existing.setUsedSwapPercentage(newMetric.getUsedSwapPercentage());
    }
}