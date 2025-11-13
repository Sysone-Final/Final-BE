package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.temperature.TemperatureResponse;
import org.example.finalbe.domains.prometheus.dto.temperature.TemperatureMetricsResponse;
import org.example.finalbe.domains.prometheus.repository.temperature.TemperatureMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemperatureMetricQueryService {

    private final TemperatureMetricRepository temperatureMetricRepository;

    public TemperatureMetricsResponse getTemperatureMetrics(Instant startTime, Instant endTime) {
        log.info("온도 메트릭 조회 시작 - startTime: {}, endTime: {}", startTime, endTime);

        Double currentTemperature = getCurrentTemperature();
        List<TemperatureResponse> temperatureTrend = getTemperatureTrend(startTime, endTime);

        return TemperatureMetricsResponse.builder()
                .currentTemperature(currentTemperature)
                .temperatureTrend(temperatureTrend)
                .build();
    }

    private Double getCurrentTemperature() {
        try {
            Object[] result = temperatureMetricRepository.getCurrentTemperature();
            if (result != null && result.length > 0) {
                return ((Number) result[0]).doubleValue();
            }
        } catch (Exception e) {
            log.error("현재 온도 조회 실패", e);
        }
        return 0.0;
    }

    private List<TemperatureResponse> getTemperatureTrend(Instant startTime, Instant endTime) {
        List<TemperatureResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = temperatureMetricRepository.getTemperatureTrend(startTime, endTime);
            for (Object[] row : rows) {
                result.add(TemperatureResponse.builder()
                        .time((Instant) row[0])
                        .avgTemperature(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .maxTemperature(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .minTemperature(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("온도 추이 조회 실패", e);
        }
        return result;
    }
}