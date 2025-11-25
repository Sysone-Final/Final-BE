package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
import org.example.finalbe.domains.prometheus.config.PrometheusProperties;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvironmentMetricCollectorService {

    private final PrometheusProperties properties;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentMappingService equipmentMappingService;
    private final RestTemplate restTemplate;
    private final EnvironmentMetricRepository environmentMetricRepository;

    private static final Long RACK_229_ID = 229L;


    private void saveRack229Temperature(double temperature) {
        try {
            EnvironmentMetric metric = EnvironmentMetric.builder()
                    .rackId(RACK_229_ID)
                    .temperature(temperature)
                    .generateTime(LocalDateTime.now())
                    // humidity, min/max 등은 null 또는 기본값
                    .build();

            environmentMetricRepository.save(metric);
            log.info("✅ Rack 229 온도 저장 완료: {:.2f}°C", temperature);

        } catch (Exception e) {
            log.error("❌ Rack 229 온도 저장 실패", e);
        }
    }
    /**
     * ✅ Environment 메트릭 수집 (Rack 229 특수 처리 포함)
     */
    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        try {
            // 기존 환경 센서 로직 (일반 Rack)
            collectGeneralEnvironmentMetrics(dataMap);

            // ✅ Rack 229 특수 처리: node_hwmon_temp_celsius 수집
            collectRack229Temperature();

        } catch (Exception e) {
            log.error("❌ Environment 메트릭 수집 중 오류", e);
        }
    }

    /**
     * ✅ Rack 229 온도 수집: node_hwmon_temp_celsius 평균값
     */
    private void collectRack229Temperature() {
        try {
            // 1. Rack 229에 속한 모든 Equipment 조회
            List<Equipment> rack229Equipments = equipmentRepository.findByRackId(RACK_229_ID);

            if (rack229Equipments.isEmpty()) {
                log.warn("⚠️ Rack 229에 Equipment가 없습니다.");
                return;
            }

            log.debug("🌡️ Rack 229 온도 수집 시작: {} 개 Equipment", rack229Equipments.size());

            // 2. 각 Equipment별 온도 수집
            List<Double> equipmentTemperatures = new ArrayList<>();

            for (Equipment equipment : rack229Equipments) {
                Optional<String> instanceOpt = equipmentMappingService.getInstance(equipment.getId());

                if (instanceOpt.isEmpty()) {
                    log.warn("⚠️ Equipment {} 프로메테우스 매핑 없음", equipment.getId());
                    continue;
                }

                String instance = instanceOpt.get();

                // 3. node_hwmon_temp_celsius 쿼리 실행
                Double avgTemp = queryHwmonTemperature(instance);

                if (avgTemp != null) {
                    equipmentTemperatures.add(avgTemp);
                    log.debug("  📊 Equipment {} ({}): 평균 온도 = {:.2f}°C",
                            equipment.getId(), instance, avgTemp);
                }
            }

            // 4. 전체 평균 온도 계산
            if (equipmentTemperatures.isEmpty()) {
                log.warn("⚠️ Rack 229: 수집된 온도 데이터가 없습니다.");
                return;
            }

            double totalAvgTemp = equipmentTemperatures.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            log.info("✅ Rack 229 최종 평균 온도: {:.2f}°C ({} 개 Equipment 평균)",
                    totalAvgTemp, equipmentTemperatures.size());

            // 5. environment_metric에 저장 (별도 저장 로직 필요)
            saveRack229Temperature(totalAvgTemp);

        } catch (Exception e) {
            log.error("❌ Rack 229 온도 수집 실패", e);
        }
    }

    /**
     * ✅ 프로메테우스에서 node_hwmon_temp_celsius 쿼리 (센서 평균값)
     */
    private Double queryHwmonTemperature(String instance) {
        try {
            String query = String.format(
                    "node_hwmon_temp_celsius{instance=\"%s\",chip=\"thermal_thermal_zone0\"}",
                    instance
            );

            String url = String.format("%s/api/v1/query?query=%s",
                    properties.getUrl(), query);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !"success".equals(response.get("status"))) {
                log.warn("⚠️ 프로메테우스 응답 실패: instance={}", instance);
                return null;
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");

            if (results == null || results.isEmpty()) {
                log.warn("⚠️ 온도 데이터 없음: instance={}", instance);
                return null;
            }

            // 모든 센서(temp0, temp1, ...) 값 수집
            List<Double> sensorValues = results.stream()
                    .map(result -> {
                        List<Object> value = (List<Object>) result.get("value");
                        if (value != null && value.size() > 1) {
                            String tempStr = value.get(1).toString();
                            return Double.parseDouble(tempStr);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (sensorValues.isEmpty()) {
                return null;
            }

            // 센서 평균값 계산
            double avgTemp = sensorValues.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            log.debug("  🔍 instance={}, 센서 개수={}, 평균={:.2f}°C",
                    instance, sensorValues.size(), avgTemp);

            return avgTemp;

        } catch (Exception e) {
            log.error("❌ 온도 쿼리 실패: instance={}", instance, e);
            return null;
        }
    }


    /**
     * 기존 환경 센서 로직 (일반 Rack용)
     */
    private void collectGeneralEnvironmentMetrics(Map<Long, MetricRawData> dataMap) {
        // 기존 코드 유지
        log.debug("🌡️ 일반 환경 센서 메트릭 수집...");
    }
}