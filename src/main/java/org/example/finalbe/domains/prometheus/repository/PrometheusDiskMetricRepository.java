package org.example.finalbe.domains.prometheus.repository;

import org.example.finalbe.domains.prometheus.domain.PrometheusDiskMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PrometheusDiskMetricRepository extends JpaRepository<PrometheusDiskMetric, Long> {

    /**
     * 최근 데이터 조회 (SSE용)
     */
    @Query("SELECT d FROM PrometheusDiskMetric d WHERE d.time >= :since ORDER BY d.time DESC")
    List<PrometheusDiskMetric> findRecentMetrics(@Param("since") Instant since);

    /**
     * 인스턴스+디바이스별 최신 메트릭
     */
    @Query(value = """
        SELECT DISTINCT ON (instance, device, mountpoint) *
        FROM prometheus_disk_metrics
        WHERE time >= :since
        ORDER BY instance, device, mountpoint, time DESC
        """, nativeQuery = true)
    List<PrometheusDiskMetric> findLatestByInstanceAndDevice(@Param("since") Instant since);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT d FROM PrometheusDiskMetric d WHERE d.time BETWEEN :start AND :end ORDER BY d.time DESC")
    List<PrometheusDiskMetric> findByTimeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 전체 인스턴스+디바이스 최신 1개
     */
    @Query(value = """
        SELECT *
        FROM prometheus_disk_metrics
        WHERE time = (SELECT MAX(time) FROM prometheus_disk_metrics)
        ORDER BY instance, device, mountpoint
        """, nativeQuery = true)
    List<PrometheusDiskMetric> findAllLatest();
}