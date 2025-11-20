package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiskMetricRepository extends JpaRepository<DiskMetric, Long> {

    // ==================== 기본 조회 ====================

    @Query("SELECT dm FROM DiskMetric dm " +
            "WHERE dm.equipmentId = :equipmentId " +
            "AND dm.generateTime BETWEEN :startTime AND :endTime " +
            "ORDER BY dm.generateTime ASC")
    List<DiskMetric> findByEquipmentIdAndTimeRange(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value = "SELECT * FROM disk_metrics " +
            "WHERE equipment_id = :equipmentId " +
            "ORDER BY generate_time DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<DiskMetric> findLatestByEquipmentId(@Param("equipmentId") Long equipmentId);

    // ==================== 디스크 통계 조회 ====================

    @Query(value = "SELECT " +
            "AVG(used_percentage) AS avg_usage, " +
            "MAX(used_percentage) AS max_usage, " +
            "MIN(used_percentage) AS min_usage " +
            "FROM disk_metrics " +
            "WHERE equipment_id = :equipmentId " +
            "AND generate_time BETWEEN :startTime AND :endTime",
            nativeQuery = true)
    Object[] getDiskUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== 집계 쿼리 (time_bucket) ====================

    @Query(value =
            "SELECT " +
                    "  time_bucket('1 minute', generate_time) AS bucket, " +
                    "  AVG(used_percentage) AS avg_usage, " +
                    "  AVG(used_inode_percentage) AS avg_inode_usage, " +
                    "  AVG(io_read_bps) AS avg_read_bps, " +
                    "  AVG(io_write_bps) AS avg_write_bps, " +
                    "  AVG(io_time_percentage) AS avg_io_time, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM disk_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getDiskAggregatedStats1Minute(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value =
            "SELECT " +
                    "  time_bucket('5 minutes', generate_time) AS bucket, " +
                    "  AVG(used_percentage) AS avg_usage, " +
                    "  AVG(used_inode_percentage) AS avg_inode_usage, " +
                    "  AVG(io_read_bps) AS avg_read_bps, " +
                    "  AVG(io_write_bps) AS avg_write_bps, " +
                    "  AVG(io_time_percentage) AS avg_io_time, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM disk_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getDiskAggregatedStats5Minutes(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value =
            "SELECT " +
                    "  time_bucket('1 hour', generate_time) AS bucket, " +
                    "  AVG(used_percentage) AS avg_usage, " +
                    "  AVG(used_inode_percentage) AS avg_inode_usage, " +
                    "  AVG(io_read_bps) AS avg_read_bps, " +
                    "  AVG(io_write_bps) AS avg_write_bps, " +
                    "  AVG(io_time_percentage) AS avg_io_time, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM disk_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getDiskAggregatedStats1Hour(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value =
            "SELECT " +
                    "  time_bucket('1 day', generate_time) AS bucket, " +
                    "  AVG(used_percentage) AS avg_usage, " +
                    "  AVG(used_inode_percentage) AS avg_inode_usage, " +
                    "  AVG(io_read_bps) AS avg_read_bps, " +
                    "  AVG(io_write_bps) AS avg_write_bps, " +
                    "  AVG(io_time_percentage) AS avg_io_time, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM disk_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getDiskAggregatedStats1Day(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== 일괄 조회 (Batch) ====================

    @Query(value =
            "SELECT DISTINCT ON (equipment_id) * " +
                    "FROM disk_metrics " +
                    "WHERE equipment_id IN (:equipmentIds) " +
                    "ORDER BY equipment_id, generate_time DESC",
            nativeQuery = true)
    List<DiskMetric> findLatestByEquipmentIds(@Param("equipmentIds") List<Long> equipmentIds);

    @Query(value =
            "SELECT " +
                    "  equipment_id, " +
                    "  AVG(used_percentage) AS avg_usage, " +
                    "  MAX(used_percentage) AS max_usage, " +
                    "  MIN(used_percentage) AS min_usage " +
                    "FROM (" +
                    "  SELECT *, ROW_NUMBER() OVER (PARTITION BY equipment_id ORDER BY generate_time DESC) AS rn " +
                    "  FROM disk_metrics " +
                    "  WHERE equipment_id IN (:equipmentIds) " +
                    ") AS ranked " +
                    "WHERE rn <= :limit " +
                    "GROUP BY equipment_id",
            nativeQuery = true)
    List<Object[]> getDiskUsageStatsBatch(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("limit") int limit
    );

    Optional<DiskMetric> findByEquipmentIdAndGenerateTime(Long equipmentId, LocalDateTime generateTime);
}
