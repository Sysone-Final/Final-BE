package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.config.PrometheusProperties;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.example.finalbe.domains.prometheus.dto.MetricStreamDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    @Scheduled(fixedDelayString = "${prometheus.collection.interval:5000}")
    public void collectMetrics() {
        if (!properties.getCollection().isEnabled()) {
            return;
        }

        try {
            log.info("📊 프로메테우스 메트릭 수집 시작...");
            long startTime = System.currentTimeMillis();

            Map<Long, MetricRawData> dataMap = initializeDataMap();

            if (dataMap.isEmpty()) {
                log.warn("⚠️ 매핑된 Equipment가 없습니다. 수집을 건너뜁니다.");
                return;
            }

            // 메트릭 수집
            systemMetricCollector.collectAndPopulate(dataMap);
            diskMetricCollector.collectAndPopulate(dataMap);
            networkMetricCollector.collectAndPopulate(dataMap);
            environmentMetricCollector.collectAndPopulate(dataMap);

            List<MetricRawData> dataList = new ArrayList<>(dataMap.values());

            // DB 저장
            systemMetricCollector.saveMetrics(dataList);
            diskMetricCollector.saveMetrics(dataList);
            networkMetricCollector.saveMetrics(dataList);
            environmentMetricCollector.saveMetrics(dataList);

            // SSE로 실시간 전송
            if (sseEmitterService.getActiveConnectionCount() > 0) {
                List<MetricStreamDto> streamData = dataList.stream()
                        .map(MetricStreamDto::from)
                        .collect(Collectors.toList());

                sseEmitterService.sendToAll("metrics", streamData);
                log.debug("📤 SSE 전송 완료: {} 개 장비 데이터", streamData.size());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ 메트릭 수집 완료: {} 개 장비, {}ms 소요", dataList.size(), elapsed);

        } catch (Exception e) {
            log.error("❌ 메트릭 수집 중 오류 발생", e);
        }
    }

    private Map<Long, MetricRawData> initializeDataMap() {
        Map<Long, MetricRawData> dataMap = new HashMap<>();

        List<String> instances = equipmentMappingService.getAllInstances();

        for (String instance : instances) {
            equipmentMappingService.getEquipmentId(instance).ifPresent(equipmentId -> {
                MetricRawData data = MetricRawData.builder()
                        .equipmentId(equipmentId)
                        .instance(instance)
                        .build();
                dataMap.put(equipmentId, data);
            });
        }

        return dataMap;
    }
}