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

    /**
     * ✅ Environment 메트릭 수집 (Rack 229 특수 처리 포함)
     */
    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        try {
            // 기존 환경 센서 로직 (일반 Rack) - 현재 비어있음
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
            List<Equipment> rack229Equipments = equipmentRepository.findActiveByRackId(RACK_229_ID);

            if (rack229Equipments.isEmpty()) {
                log.warn("⚠️ Rack 229에 Equipment가 없습니다.");
                return;
            }

            log.info("🌡️ Rack 229 온도 수집 시작: {} 개 Equipment", rack229Equipments.size());

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
                    log.info("  📊 Equipment {} ({}): 평균 온도 = {:.2f}°C",
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

            // 5. environment_metric에 저장
            saveRack229Temperature(totalAvgTemp);

        } catch (Exception e) {
            log.error("❌ Rack 229 온도 수집 실패", e);
        }
    }

    /**
     * ✅ 프로메테우스에서 node_hwmon_temp_celsius 쿼리 (센서 평균값)
     * 레이블 필터 완전 제거 - 전체 데이터 가져온 후 코드에서 필터링
     */
    private Double queryHwmonTemperature(String instance) {
        try {
            // ✅ 레이블 필터 완전 제거
            String query = "node_hwmon_temp_celsius";

            log.debug("🔍 온도 쿼리: {}", query);

            List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

            if (results == null || results.isEmpty()) {
                log.warn("⚠️ node_hwmon_temp_celsius 데이터 없음");
                return null;
            }

            log.debug("  🔍 전체 온도 데이터: {} 개", results.size());

            // ✅ 코드에서 instance + chip 필터링
            List<Double> sensorValues = results.stream()
                    .filter(result -> {
                        // instance 필터
                        if (!instance.equals(result.getInstance())) {
                            return false;
                        }

                        // chip 필터 (thermal_thermal_zone0 또는 thermal 포함)
                        String chip = result.metric().get("chip");
                        if (chip == null || !chip.contains("thermal")) {
                            return false;
                        }

                        return true;
                    })
                    .map(PrometheusResponse.PrometheusResult::getValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (sensorValues.isEmpty()) {
                log.warn("⚠️ 온도 데이터 없음: instance={}", instance);
                return null;
            }

            // 센서 평균값 계산
            double avgTemp = sensorValues.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            log.info("  📊 instance={}, 센서 개수={}, 평균온도={:.2f}°C",
                    instance, sensorValues.size(), avgTemp);

            return avgTemp;

        } catch (Exception e) {
            log.error("❌ 온도 쿼리 실패: instance={}, error={}", instance, e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Rack 229 온도를 environment_metric 테이블에 저장
     */
    private void saveRack229Temperature(double temperature) {
        try {
            EnvironmentMetric metric = EnvironmentMetric.builder()
                    .rackId(RACK_229_ID)
                    .temperature(temperature)
                    .generateTime(LocalDateTime.now())
                    .build();

            environmentMetricRepository.save(metric);
            log.info("✅ Rack 229 온도 저장 완료: {:.2f}°C", temperature);

        } catch (Exception e) {
            log.error("❌ Rack 229 온도 저장 실패", e);
        }
    }

    /**
     * 기존 환경 센서 로직 (일반 Rack용)
     */
    private void collectGeneralEnvironmentMetrics(Map<Long, MetricRawData> dataMap) {
        log.debug("🌡️ 일반 환경 센서 메트릭 수집 (현재 미구현)");
    }
}