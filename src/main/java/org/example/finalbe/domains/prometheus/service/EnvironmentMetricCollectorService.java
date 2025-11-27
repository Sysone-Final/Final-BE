/**
 * 작성자: 황요한
 * Prometheus에서 환경 메트릭을 수집하고 Rack 229의 온도를 저장하는 서비스
 */
package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.example.finalbe.domains.prometheus.dto.PrometheusResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentMetricCollectorService {

    private final PrometheusQueryService prometheusQuery;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentMappingService equipmentMappingService;
    private final EnvironmentMetricRepository environmentMetricRepository;

    private static final Long RACK_229_ID = 229L;

    // 환경 메트릭을 수집하여 Map에 채워넣음
    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        try {
            collectGeneralEnvironmentMetrics(dataMap);
            collectRack229Temperature();
        } catch (Exception e) {
            log.error("Environment 메트릭 수집 중 오류", e);
        }
    }

    // Rack 229의 평균 온도를 수집
    private void collectRack229Temperature() {
        try {
            List<Equipment> rack229Equipments = equipmentRepository.findActiveByRackId(RACK_229_ID);

            if (rack229Equipments.isEmpty()) {
                log.warn("Rack 229에 Equipment가 없음");
                return;
            }

            List<Double> equipmentTemperatures = new ArrayList<>();

            for (Equipment equipment : rack229Equipments) {
                Optional<String> instanceOpt = equipmentMappingService.getInstance(equipment.getId());
                if (instanceOpt.isEmpty()) continue;

                Double avgTemp = queryHwmonTemperature(instanceOpt.get());
                if (avgTemp != null) {
                    equipmentTemperatures.add(avgTemp);
                }
            }

            if (equipmentTemperatures.isEmpty()) {
                log.warn("Rack 229 온도 데이터 없음");
                return;
            }

            double totalAvgTemp = equipmentTemperatures.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            saveRack229Temperature(totalAvgTemp);

        } catch (Exception e) {
            log.error("Rack 229 온도 수집 실패", e);
        }
    }

    // Prometheus에서 hwmon 온도 데이터를 조회
    private Double queryHwmonTemperature(String instance) {
        try {
            String query = "node_hwmon_temp_celsius";
            List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

            List<Double> sensorValues = results.stream()
                    .filter(result -> instance.equals(result.getInstance()))
                    .filter(result -> {
                        String chip = result.metric().get("chip");
                        return chip != null && chip.contains("thermal");
                    })
                    .map(PrometheusResponse.PrometheusResult::getValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (sensorValues.isEmpty()) return null;
            double avgTemp = sensorValues.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            return avgTemp;


        } catch (Exception e) {
            log.error("온도 쿼리 실패: instance={}", instance, e);
            return null;
        }
    }



    // Rack 229 온도를 DB에 저장
    private void saveRack229Temperature(double temperature) {
        try {
            EnvironmentMetric metric = EnvironmentMetric.builder()
                    .rackId(RACK_229_ID)
                    .temperature(temperature)
                    .generateTime(LocalDateTime.now())
                    .build();

            environmentMetricRepository.save(metric);

        } catch (Exception e) {
            log.error("Rack 229 온도 저장 실패", e);
        }
    }

    // 일반 Rack 환경 센서 메트릭 수집 (현재 미구현)
    private void collectGeneralEnvironmentMetrics(Map<Long, MetricRawData> dataMap) {
        log.debug("일반 환경 메트릭 수집 미구현");
    }
}
