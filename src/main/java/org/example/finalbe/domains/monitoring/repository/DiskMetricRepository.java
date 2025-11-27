/**
 * 작성자: 황요한
 * 디스크 메트릭 조회 및 집계용 Repository
 */
package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface DiskMetricRepository extends JpaRepository<DiskMetric, Long> {

    // 특정 장비의 시간 범위 내 메트릭 조회
    @Query("""
        SELECT dm FROM DiskMetric dm
        WHERE dm.equipmentId = :equipmentId
        AND dm.generateTime BETWEEN :startTime AND :endTime
        ORDER BY dm.generateTime ASC
    """)
    List<DiskMetric> findByEquipmentIdAndTimeRange(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 특정 장비의 최신 메트릭 조회
    @Query(value = """
        SELECT * FROM disk_metrics
        WHERE equipment_id = :equipmentId
        ORDER BY generate_time DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<DiskMetric> findLatestByEquipmentId(@Param("equipmentId") Long equipmentId);

    // 디스크 사용률 기본 통계 조회 (평균/최대/최소)
    @Query(value = """
        SELECT 
            AVG(used_percentage) AS avg_usage,
            MAX(used_percentage) AS max_usage,
            MIN(used_percentage) AS min_usage
        FROM disk_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Object[] getDiskUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1분 단위 디스크 집계 통계
    @Query(value = """
        SELECT 
            time_bucket('1 minute', generate_time) AS bucket,
            AVG(used_percentage),
            AVG(used_inode_percentage),
            AVG(io_read_bps),
            AVG(io_write_bps),
            AVG(io_time_percentage),
            COUNT(*)
        FROM disk_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY bucket
        ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getDiskAggregatedStats1Minute(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 5분 단위 디스크 집계 통계
    @Query(value = """
        SELECT 
            time_bucket('5 minutes', generate_time) AS bucket,
            AVG(used_percentage),
            AVG(used_inode_percentage),
            AVG(io_read_bps),
            AVG(io_write_bps),
            AVG(io_time_percentage),
            COUNT(*)
        FROM disk_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY bucket
        ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getDiskAggregatedStats5Minutes(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1시간 단위 디스크 집계 통계
    @Query(value = """
        SELECT 
            time_bucket('1 hour', generate_time) AS bucket,
            AVG(used_percentage),
            AVG(used_inode_percentage),
            AVG(io_read_bps),
            AVG(io_write_bps),
            AVG(io_time_percentage),
            COUNT(*)
        FROM disk_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY bucket
        ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getDiskAggregatedStats1Hour(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1일 단위 디스크 집계 통계
    @Query(value = """
        SELECT 
            time_bucket('1 day', generate_time) AS bucket,
            AVG(used_percentage),
            AVG(used_inode_percentage),
            AVG(io_read_bps),
            AVG(io_write_bps),
            AVG(io_time_percentage),
            COUNT(*)
        FROM disk_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY bucket
        ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getDiskAggregatedStats1Day(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 여러 장비의 최신 메트릭 일괄 조회
    @Query(value = """
        SELECT DISTINCT ON (equipment_id) *
        FROM disk_metrics
        WHERE equipment_id IN (:equipmentIds)
        ORDER BY equipment_id, generate_time DESC
    """, nativeQuery = true)
    List<DiskMetric> findLatestByEquipmentIds(@Param("equipmentIds") List<Long> equipmentIds);

    // 여러 장비의 디스크 사용률 통계 (평균/최대/최소)
    @Query(value = """
        SELECT 
            equipment_id,
            AVG(used_percentage),
            MAX(used_percentage),
            MIN(used_percentage)
        FROM (
            SELECT *,
                ROW_NUMBER() OVER (PARTITION BY equipment_id ORDER BY generate_time DESC) AS rn
            FROM disk_metrics
            WHERE equipment_id IN (:equipmentIds)
        ) ranked
        WHERE rn <= :limit
        GROUP BY equipment_id
    """, nativeQuery = true)
    List<Object[]> getDiskUsageStatsBatch(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("limit") int limit
    );

    // 특정 시점의 메트릭 조회
    Optional<DiskMetric> findByEquipmentIdAndGenerateTime(
            Long equipmentId,
            LocalDateTime generateTime
    );

    // 여러 장비의 평균 디스크 통계 조회
    @Query(value = """
        SELECT 
            AVG(used_percentage) AS avgDiskUsage,
            MAX(used_percentage) AS maxDiskUsage,
            MIN(used_percentage) AS minDiskUsage,
            SUM(total_bytes) AS totalDiskBytes,
            SUM(used_bytes) AS totalUsedDiskBytes,
            AVG(io_time_percentage) AS avgDiskIoUsage,
            COUNT(DISTINCT equipment_id) AS equipmentCount
        FROM disk_metrics
        WHERE equipment_id IN :equipmentIds
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Map<String, Object> getAverageDiskStatsByEquipmentIds(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
