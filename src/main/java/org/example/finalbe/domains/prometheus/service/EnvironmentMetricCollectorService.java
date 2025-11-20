package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
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
public class EnvironmentMetricCollectorService {

    private final PrometheusQueryService prometheusQuery;
    private final EnvironmentMetricRepository environmentMetricRepository;
    private final EquipmentMappingService equipmentMappingService;

    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        collectTemperature(dataMap);
    }

    private void collectTemperature(Map<Long, MetricRawData> dataMap) {
        String query = "avg by (instance) (node_hwmon_temp_celsius)";
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setTemperature(value);
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
                Equipment equipment = equipmentMappingService.getEquipment(data.getEquipmentId())
                        .orElse(null);

                if (equipment == null || equipment.getRack() == null) {
                    log.warn("⚠️ Equipment or Rack not found for equipmentId={}", data.getEquipmentId());
                    continue;
                }

                if (data.getTemperature() == null) {
                    log.debug("  ⏭️ Temperature data not available for equipmentId={}", data.getEquipmentId());
                    continue;
                }

                Long rackId = equipment.getRack().getId();
                EnvironmentMetric metric = convertToEntity(data, rackId);

                EnvironmentMetric existing = environmentMetricRepository
                        .findByRackIdAndGenerateTime(rackId, metric.getGenerateTime())
                        .orElse(null);

                if (existing != null) {
                    updateExisting(existing, metric);
                    environmentMetricRepository.save(existing);
                } else {
                    environmentMetricRepository.save(metric);
                }

                log.debug("  ✓ EnvironmentMetric 저장: rackId={}, temperature={}",
                        rackId, data.getTemperature());

            } catch (Exception e) {
                log.error("❌ EnvironmentMetric 저장 실패: equipmentId={} - {}",
                        data.getEquipmentId(), e.getMessage());
            }
        }
    }

    private EnvironmentMetric convertToEntity(MetricRawData data, Long rackId) {
        LocalDateTime generateTime = data.getTimestamp() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();

        return EnvironmentMetric.builder()
                .rackId(rackId)
                .generateTime(generateTime)
                .temperature(data.getTemperature())
                .humidity(null)
                .build();
    }

    private void updateExisting(EnvironmentMetric existing, EnvironmentMetric newMetric) {
        existing.setTemperature(newMetric.getTemperature());
    }
}