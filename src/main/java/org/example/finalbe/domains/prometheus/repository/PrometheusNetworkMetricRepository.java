package org.example.finalbe.domains.prometheus.repository;

import org.example.finalbe.domains.prometheus.domain.PrometheusNetworkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PrometheusNetworkMetricRepository extends JpaRepository<PrometheusNetworkMetric, Long> {

    /**
     * 최근 데이터 조회 (SSE용)
     */
    @Query("SELECT n FROM PrometheusNetworkMetric n WHERE n.time >= :since ORDER BY n.time DESC")
    List<PrometheusNetworkMetric> findRecentMetrics(@Param("since") Instant since);

    /**
     * 인스턴스+디바이스별 최신 메트릭
     */
    @Query(value = """
        SELECT DISTINCT ON (instance, device) *
        FROM prometheus_network_metrics
        WHERE time >= :since
        ORDER BY instance, device, time DESC
        """, nativeQuery = true)
    List<PrometheusNetworkMetric> findLatestByInstanceAndDevice(@Param("since") Instant since);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT n FROM PrometheusNetworkMetric n WHERE n.time BETWEEN :start AND :end ORDER BY n.time DESC")
    List<PrometheusNetworkMetric> findByTimeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 전체 인스턴스+디바이스 최신 1개
     */
    @Query(value = """
        SELECT *
        FROM prometheus_network_metrics
        WHERE time = (SELECT MAX(time) FROM prometheus_network_metrics)
        ORDER BY instance, device
        """, nativeQuery = true)
    List<PrometheusNetworkMetric> findAllLatest();
}