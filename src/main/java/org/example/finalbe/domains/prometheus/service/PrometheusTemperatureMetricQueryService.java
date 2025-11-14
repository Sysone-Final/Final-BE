package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.temperature.TemperatureResponse;
import org.example.finalbe.domains.prometheus.dto.temperature.TemperatureMetricsResponse;
import org.example.finalbe.domains.prometheus.repository.temperature.PrometheusTemperatureMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusTemperatureMetricQueryService {

    private final PrometheusTemperatureMetricRepository prometheusTemperatureMetricRepository;

    public TemperatureMetricsResponse getTemperatureMetrics(Instant startTime, Instant endTime) {
        log.info("온도 메트릭 조회 시작 - startTime: {}, endTime: {}", startTime, endTime);

        return new TemperatureMetricsResponse(
                getCurrentTemperature(),
                getTemperatureTrend(startTime, endTime)
        );
    }

    private Double getCurrentTemperature() {
        try {
            return prometheusTemperatureMetricRepository.getCurrentTemperature();
        } catch (Exception e) {
            log.error("현재 온도 조회 실패", e);
            return 0.0;
        }
    }

    private List<TemperatureResponse> getTemperatureTrend(Instant startTime, Instant endTime) {
        try {
            return prometheusTemperatureMetricRepository.getTemperatureTrend(startTime, endTime)
                    .stream()
                    .map(TemperatureResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("온도 추이 조회 실패", e);
            return List.of();
        }
    }
}