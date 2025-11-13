package org.example.finalbe.domains.prometheus.repository.disk;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PrometheusDiskMetricRepository {

    private final EntityManager entityManager;

    /**
     * 디스크 사용률 추이
     */
    public List<Object[]> getDiskUsageTrend(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                fs.time,
                fs.value as total_bytes,
                ff.value as free_bytes,
                (fs.value - ff.value) as used_bytes,
                ((fs.value - ff.value) / NULLIF(fs.value, 0) * 100) as usage_percent
            FROM prom_metric.node_filesystem_size_bytes fs
            JOIN prom_metric.node_filesystem_free_bytes ff 
                ON fs.time = ff.time 
                AND fs.device_id = ff.device_id 
                AND fs.mountpoint_id = ff.mountpoint_id
            WHERE fs.time BETWEEN :startTime AND :endTime
            ORDER BY fs.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 디스크 I/O 속도 (읽기/쓰기)
     */
    public List<Object[]> getDiskIoSpeed(Instant startTime, Instant endTime) {
        String query = """
            WITH read_data AS (
                SELECT 
                    time,
                    device_id,
                    value,
                    LAG(value) OVER (PARTITION BY device_id ORDER BY time) as prev_value
                FROM prom_metric.node_disk_read_bytes_total
                WHERE time BETWEEN :startTime AND :endTime
            ),
            write_data AS (
                SELECT 
                    time,
                    device_id,
                    value,
                    LAG(value) OVER (PARTITION BY device_id ORDER BY time) as prev_value
                FROM prom_metric.node_disk_written_bytes_total
                WHERE time BETWEEN :startTime AND :endTime
            )
            SELECT 
                r.time,
                SUM(CASE WHEN r.prev_value IS NOT NULL 
                    THEN (r.value - r.prev_value) ELSE 0 END) as read_bps,
                SUM(CASE WHEN w.prev_value IS NOT NULL 
                    THEN (w.value - w.prev_value) ELSE 0 END) as write_bps
            FROM read_data r
            JOIN write_data w ON r.time = w.time AND r.device_id = w.device_id
            GROUP BY r.time
            ORDER BY r.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 디스크 IOPS (읽기/쓰기)
     */
    public List<Object[]> getDiskIops(Instant startTime, Instant endTime) {
        String query = """
            WITH read_data AS (
                SELECT 
                    time,
                    device_id,
                    value,
                    LAG(value) OVER (PARTITION BY device_id ORDER BY time) as prev_value
                FROM prom_metric.node_disk_reads_completed_total
                WHERE time BETWEEN :startTime AND :endTime
            ),
            write_data AS (
                SELECT 
                    time,
                    device_id,
                    value,
                    LAG(value) OVER (PARTITION BY device_id ORDER BY time) as prev_value
                FROM prom_metric.node_disk_writes_completed_total
                WHERE time BETWEEN :startTime AND :endTime
            )
            SELECT 
                r.time,
                SUM(CASE WHEN r.prev_value IS NOT NULL 
                    THEN (r.value - r.prev_value) ELSE 0 END) as read_iops,
                SUM(CASE WHEN w.prev_value IS NOT NULL 
                    THEN (w.value - w.prev_value) ELSE 0 END) as write_iops
            FROM read_data r
            JOIN write_data w ON r.time = w.time AND r.device_id = w.device_id
            GROUP BY r.time
            ORDER BY r.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 디스크 I/O 사용률
     */
    public List<Object[]> getDiskIoUtilization(Instant startTime, Instant endTime) {
        String query = """
            WITH io_data AS (
                SELECT 
                    time,
                    device_id,
                    value,
                    LAG(value) OVER (PARTITION BY device_id ORDER BY time) as prev_value
                FROM prom_metric.node_disk_io_time_seconds_total
                WHERE time BETWEEN :startTime AND :endTime
            )
            SELECT 
                time,
                SUM(CASE WHEN prev_value IS NOT NULL 
                    THEN (value - prev_value) * 100 ELSE 0 END) as io_utilization_percent
            FROM io_data
            GROUP BY time
            ORDER BY time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * inode 사용률
     */
    public List<Object[]> getInodeUsage(Instant time) {
        String query = """
            SELECT 
                fi.device_id,
                fi.mountpoint_id,
                fi.value as total_inodes,
                ff.value as free_inodes,
                (fi.value - ff.value) as used_inodes,
                ((fi.value - ff.value) / NULLIF(fi.value, 0) * 100) as inode_usage_percent
            FROM prom_metric.node_filesystem_files fi
            JOIN prom_metric.node_filesystem_files_free ff 
                ON fi.device_id = ff.device_id 
                AND fi.mountpoint_id = ff.mountpoint_id
            WHERE fi.time = :time
            ORDER BY fi.device_id
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("time", time)
                .getResultList();
    }

    /**
     * 현재 디스크 사용률 (Gauge용)
     */
    public Object[] getCurrentDiskUsage() {
        String query = """
            SELECT 
                fs.value as total_bytes,
                ff.value as free_bytes,
                ((fs.value - ff.value) / NULLIF(fs.value, 0) * 100) as usage_percent
            FROM prom_metric.node_filesystem_size_bytes fs
            JOIN prom_metric.node_filesystem_free_bytes ff 
                ON fs.device_id = ff.device_id 
                AND fs.mountpoint_id = ff.mountpoint_id
            WHERE fs.time = (SELECT MAX(time) FROM prom_metric.node_filesystem_size_bytes)
            LIMIT 1
            """;

        List<Object[]> results = entityManager.createNativeQuery(query).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}