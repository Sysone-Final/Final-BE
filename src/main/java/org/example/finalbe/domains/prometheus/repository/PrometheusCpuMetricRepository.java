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

    /**
     * ✅ CPU 사용률 시계열 (time_bucket 집계)
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        instance,
        AVG(cpu_usage_percent) AS avg_cpu
    FROM prometheus_cpu_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
    GROUP BY bucket, instance
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getCpuUsageTimeSeries(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ CPU 모드별 분포 (적층 차트용)
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        instance,
        AVG(user_percent) AS user,
        AVG(system_percent) AS system,
        AVG(iowait_percent) AS iowait,
        AVG(irq_percent) AS irq,
        AVG(softirq_percent) AS softirq,
        AVG(idle_percent) AS idle
    FROM prometheus_cpu_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
    GROUP BY bucket, instance
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getCpuModeDistribution(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * 시스템 부하
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        AVG(load_avg1) AS load1,
        AVG(load_avg5) AS load5,
        AVG(load_avg15) AS load15
    FROM prometheus_cpu_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
    GROUP BY bucket
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getSystemLoad(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}