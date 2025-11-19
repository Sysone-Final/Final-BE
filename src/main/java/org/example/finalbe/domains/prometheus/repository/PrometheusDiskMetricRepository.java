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

    /**
     * ✅ 디스크 사용률 (게이지 - 최신값)
     */
    @Query(value = """
    SELECT DISTINCT ON (device, mountpoint)
        device,
        mountpoint,
        usage_percent,
        total_bytes,
        used_bytes,
        free_bytes
    FROM prometheus_disk_metrics
    WHERE instance = :instance
    ORDER BY device, mountpoint, time DESC
    """, nativeQuery = true)
    List<Object[]> getDiskUsageCurrent(@Param("instance") String instance);

    /**
     * ✅ 디스크 I/O 사용률
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        device,
        AVG(io_utilization_percent) AS avg_io_util
    FROM prometheus_disk_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
    GROUP BY bucket, device
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getDiskIoUtilization(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ 디스크 읽기/쓰기 속도
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        device,
        AVG(read_bytes_per_sec) AS avg_read_speed,
        AVG(write_bytes_per_sec) AS avg_write_speed
    FROM prometheus_disk_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
    GROUP BY bucket, device
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getDiskThroughput(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ 디스크 IOPS
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        device,
        AVG(read_iops) AS avg_read_iops,
        AVG(write_iops) AS avg_write_iops
    FROM prometheus_disk_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
    GROUP BY bucket, device
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getDiskIops(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ 디스크 공간 추이
     */
    @Query(value = """
    SELECT 
        time_bucket('1 hour', time) AS bucket,
        device,
        AVG(free_bytes) AS avg_free_bytes
    FROM prometheus_disk_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
    GROUP BY bucket, device
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getDiskSpaceTrend(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ inode 사용률 (게이지 - 최신값)
     */
    @Query(value = """
    SELECT DISTINCT ON (device, mountpoint)
        device,
        mountpoint,
        inode_usage_percent,
        total_inodes,
        used_inodes,
        free_inodes
    FROM prometheus_disk_metrics
    WHERE instance = :instance
    ORDER BY device, mountpoint, time DESC
    """, nativeQuery = true)
    List<Object[]> getInodeUsageCurrent(@Param("instance") String instance);
}