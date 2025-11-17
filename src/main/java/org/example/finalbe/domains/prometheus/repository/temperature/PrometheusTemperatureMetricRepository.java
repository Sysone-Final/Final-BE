package org.example.finalbe.domains.prometheus.repository.temperature;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PrometheusTemperatureMetricRepository {

    private final EntityManager entityManager;

    /**
     * 온도 추이
     */
    public List<Object[]> getTemperatureTrend(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                time,
                AVG(value)::double precision as avg_temperature,
                MAX(value)::double precision as max_temperature,
                MIN(value)::double precision as min_temperature
            FROM prom_metric."node_hwmon_temp_celsius"
            WHERE time BETWEEN :startTime AND :endTime
            GROUP BY time
            ORDER BY time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 현재 온도 (Gauge용)
     */
    public Double getCurrentTemperature() {
        String query = """
        SELECT 
            AVG(value)::double precision as current_temperature
        FROM prom_metric."node_hwmon_temp_celsius"
        WHERE time = (SELECT MAX(time) FROM prom_metric."node_hwmon_temp_celsius")
        """;

        List<Double> results = entityManager.createNativeQuery(query).getResultList();
        return results.isEmpty() ? 0.0 : results.get(0);
    }
    /**
     * 모든 센서의 최신 온도 조회
     */
    public List<Object[]> getLatestTemperatureAllSensors() {
        String query = """
        WITH latest_time AS (
            SELECT MAX(time) as max_time 
            FROM prom_metric.node_hwmon_temp_celsius
        )
        SELECT 
            instance_id,
            chip_id,
            sensor_id,
            value as celsius
        FROM prom_metric.node_hwmon_temp_celsius
        CROSS JOIN latest_time lt
        WHERE time = lt.max_time
        """;

        return entityManager.createNativeQuery(query).getResultList();
    }
}