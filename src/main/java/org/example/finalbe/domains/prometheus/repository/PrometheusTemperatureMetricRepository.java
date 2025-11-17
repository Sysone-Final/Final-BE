package org.example.finalbe.domains.prometheus.repository;

import org.example.finalbe.domains.prometheus.domain.PrometheusTemperatureMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PrometheusTemperatureMetricRepository extends JpaRepository<PrometheusTemperatureMetric, Long> {

    /**
     * 최근 데이터 조회 (SSE용)
     */
    @Query("SELECT t FROM PrometheusTemperatureMetric t WHERE t.time >= :since ORDER BY t.time DESC")
    List<PrometheusTemperatureMetric> findRecentMetrics(@Param("since") Instant since);

    /**
     * 인스턴스별 최신 메트릭
     */
    @Query(value = """
        SELECT DISTINCT ON (instance, chip, sensor) *
        FROM prometheus_temperature_metrics
        WHERE time >= :since
        ORDER BY instance, chip, sensor, time DESC
        """, nativeQuery = true)
    List<PrometheusTemperatureMetric> findLatestByInstance(@Param("since") Instant since);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT t FROM PrometheusTemperatureMetric t WHERE t.time BETWEEN :start AND :end ORDER BY t.time DESC")
    List<PrometheusTemperatureMetric> findByTimeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 전체 인스턴스 최신 1개
     */
    @Query(value = """
        SELECT *
        FROM prometheus_temperature_metrics
        WHERE time = (SELECT MAX(time) FROM prometheus_temperature_metrics)
        ORDER BY instance, chip, sensor
        """, nativeQuery = true)
    List<PrometheusTemperatureMetric> findAllLatest();
}