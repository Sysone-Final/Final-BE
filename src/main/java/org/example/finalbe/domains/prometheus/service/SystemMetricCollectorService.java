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
        String query = "avg by (instance, mode) (rate(node_cpu_seconds_total[5s]))";
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            String mode = result.getMode();
            Double value = result.getValue();

            // ✅ 0값 필터링
            if (instance != null && mode != null && value != null && value > 0.0) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.getCpuModes().put(mode, value * 100);
                }
            }
        }
    }

    private void collectMemoryMetrics(Map<Long, MetricRawData> dataMap) {
        collectMemoryMetric(dataMap, "node_memory_MemTotal_bytes", MetricRawData::setTotalMemory);
        collectMemoryMetric(dataMap, "node_memory_MemFree_bytes", MetricRawData::setFreeMemory);
        collectMemoryMetric(dataMap, "node_memory_MemAvailable_bytes", MetricRawData::setAvailableMemory);
        collectMemoryMetric(dataMap, "node_memory_Buffers_bytes", MetricRawData::setBuffersMemory);
        collectMemoryMetric(dataMap, "node_memory_Cached_bytes", MetricRawData::setCachedMemory);
        collectMemoryMetric(dataMap, "node_memory_Active_bytes", MetricRawData::setActiveMemory);
        collectMemoryMetric(dataMap, "node_memory_Inactive_bytes", MetricRawData::setInactiveMemory);
        collectMemoryMetric(dataMap, "node_memory_SwapTotal_bytes", MetricRawData::setTotalSwap);
        collectMemoryMetric(dataMap, "node_memory_SwapFree_bytes", MetricRawData::setFreeSwap);
    }

    private void collectMemoryMetric(Map<Long, MetricRawData> dataMap, String metric,
                                     java.util.function.BiConsumer<MetricRawData, Long> setter) {
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(metric);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            // ✅ 0값과 null 필터링
            if (instance != null && value != null && value > 0.0) {
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
        String query = "rate(node_context_switches_total[5s])";
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            // ✅ 0값 필터링
            if (instance != null && value != null && value > 0.0) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setContextSwitches((long) (value * 5));
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

    @Transactional
    public void saveMetrics(List<MetricRawData> dataList) {
        for (MetricRawData data : dataList) {
            try {
                SystemMetric metric = convertToEntity(data);

                // ✅ 중복 체크 및 업데이트
                SystemMetric existing = systemMetricRepository
                        .findByEquipmentIdAndGenerateTime(data.getEquipmentId(), metric.getGenerateTime())
                        .orElse(null);

                if (existing != null) {
                    updateExisting(existing, metric);
                    systemMetricRepository.save(existing);
                    log.debug("  ↻ SystemMetric 업데이트: equipmentId={}", data.getEquipmentId());
                } else {
                    systemMetricRepository.save(metric);
                    log.debug("  ✓ SystemMetric 저장: equipmentId={}", data.getEquipmentId());
                }

            } catch (Exception e) {
                log.error("❌ SystemMetric 저장 실패: equipmentId={} - {}",
                        data.getEquipmentId(), e.getMessage());
            }
        }
    }

    private SystemMetric convertToEntity(MetricRawData data) {
        // ✅ 통일된 타임스탬프 사용
        LocalDateTime generateTime = data.getTimestamp() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();

        Map<String, Double> cpuModes = data.getCpuModes();

        Long usedMemory = null;
        Double usedMemoryPercentage = null;
        if (data.getTotalMemory() != null && data.getFreeMemory() != null) {
            usedMemory = data.getTotalMemory() - data.getFreeMemory();
            usedMemoryPercentage = (usedMemory * 100.0) / data.getTotalMemory();
        }

        Long usedSwap = null;
        Double usedSwapPercentage = null;
        if (data.getTotalSwap() != null && data.getFreeSwap() != null && data.getTotalSwap() > 0) {
            usedSwap = data.getTotalSwap() - data.getFreeSwap();
            usedSwapPercentage = (usedSwap * 100.0) / data.getTotalSwap();
        }

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
                .memoryBuffers(data.getBuffersMemory())
                .memoryCached(data.getCachedMemory())
                .memoryActive(data.getActiveMemory())
                .memoryInactive(data.getInactiveMemory())
                .totalSwap(data.getTotalSwap())
                .usedSwap(usedSwap)
                .usedSwapPercentage(usedSwapPercentage)
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