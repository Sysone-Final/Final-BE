package org.example.finalbe.domains.prometheus.repository;

import org.example.finalbe.domains.prometheus.domain.PrometheusMemoryMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PrometheusMemoryMetricRepository extends JpaRepository<PrometheusMemoryMetric, Long> {

    /**
     * 최근 데이터 조회 (SSE용)
     */
    @Query("SELECT m FROM PrometheusMemoryMetric m WHERE m.time >= :since ORDER BY m.time DESC")
    List<PrometheusMemoryMetric> findRecentMetrics(@Param("since") Instant since);

    /**
     * 인스턴스별 최신 메트릭
     */
    @Query(value = """
        SELECT DISTINCT ON (instance) *
        FROM prometheus_memory_metrics
        WHERE time >= :since
        ORDER BY instance, time DESC
        """, nativeQuery = true)
    List<PrometheusMemoryMetric> findLatestByInstance(@Param("since") Instant since);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT m FROM PrometheusMemoryMetric m WHERE m.time BETWEEN :start AND :end ORDER BY m.time DESC")
    List<PrometheusMemoryMetric> findByTimeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 전체 인스턴스 최신 1개
     */
    @Query(value = """
        SELECT *
        FROM prometheus_memory_metrics
        WHERE time = (SELECT MAX(time) FROM prometheus_memory_metrics)
        ORDER BY instance
        """, nativeQuery = true)
    List<PrometheusMemoryMetric> findAllLatest();
}