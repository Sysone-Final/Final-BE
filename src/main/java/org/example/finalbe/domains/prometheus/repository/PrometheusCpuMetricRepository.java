package org.example.finalbe.domains.prometheus.repository;

import org.example.finalbe.domains.prometheus.domain.PrometheusCpuMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PrometheusCpuMetricRepository extends JpaRepository<PrometheusCpuMetric, Long> {

    /**
     * 최근 데이터 조회 (SSE용)
     */
    @Query("SELECT c FROM PrometheusCpuMetric c WHERE c.time >= :since ORDER BY c.time DESC")
    List<PrometheusCpuMetric> findRecentMetrics(@Param("since") Instant since);

    /**
     * 인스턴스별 최신 메트릭
     */
    @Query(value = """
        SELECT DISTINCT ON (instance) *
        FROM prometheus_cpu_metrics
        WHERE time >= :since
        ORDER BY instance, time DESC
        """, nativeQuery = true)
    List<PrometheusCpuMetric> findLatestByInstance(@Param("since") Instant since);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT c FROM PrometheusCpuMetric c WHERE c.time BETWEEN :start AND :end ORDER BY c.time DESC")
    List<PrometheusCpuMetric> findByTimeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 전체 인스턴스 최신 1개
     */
    @Query(value = """
        SELECT *
        FROM prometheus_cpu_metrics
        WHERE time = (SELECT MAX(time) FROM prometheus_cpu_metrics)
        ORDER BY instance
        """, nativeQuery = true)
    List<PrometheusCpuMetric> findAllLatest();
}