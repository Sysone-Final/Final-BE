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
            FROM prom_metric.node_hwmon_temp_celsius
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
    public Object[] getCurrentTemperature() {
        String query = """
            SELECT 
                AVG(value)::double precision as current_temperature
            FROM prom_metric.node_hwmon_temp_celsius
            WHERE time = (SELECT MAX(time) FROM prom_metric.node_hwmon_temp_celsius)
            """;

        List<Object[]> results = entityManager.createNativeQuery(query).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}