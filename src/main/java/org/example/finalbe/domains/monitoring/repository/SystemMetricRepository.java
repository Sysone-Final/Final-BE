/**
 * 작성자: 황요한
 * SystemMetric 저장 및 조회 기능을 제공하는 Repository
 */
package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.monitoring.dto.MetricChartData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {

    // 장비의 기간별 시스템 메트릭 조회
    @Query("""
        SELECT sm FROM SystemMetric sm
        WHERE sm.equipmentId = :equipmentId
        AND sm.generateTime BETWEEN :startTime AND :endTime
        ORDER BY sm.generateTime ASC
    """)
    List<SystemMetric> findByEquipmentIdAndTimeRange(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 장비의 최신 시스템 메트릭 조회
    @Query(value = """
        SELECT * FROM system_metrics
        WHERE equipment_id = :equipmentId
        ORDER BY generate_time DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<SystemMetric> findLatestByEquipmentId(@Param("equipmentId") Long equipmentId);

    // 여러 장비의 기간별 메트릭 조회
    @Query("""
        SELECT sm FROM SystemMetric sm
        WHERE sm.equipmentId IN :equipmentIds
        AND sm.generateTime BETWEEN :startTime AND :endTime
        ORDER BY sm.equipmentId, sm.generateTime ASC
    """)
    List<SystemMetric> findByEquipmentIdsAndTimeRange(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 여러 장비의 최신 메트릭 일괄 조회
    @Query(value = """
        SELECT DISTINCT ON (equipment_id) *
        FROM system_metrics
        WHERE equipment_id IN :equipmentIds
        ORDER BY equipment_id, generate_time DESC
    """, nativeQuery = true)
    List<SystemMetric> findLatestByEquipmentIds(@Param("equipmentIds") List<Long> equipmentIds);

    // 여러 장비의 최근 N개 메모리 사용률 통계 조회
    @Query(value = """
        SELECT equipment_id,
               AVG(used_memory_percentage),
               MAX(used_memory_percentage),
               MIN(used_memory_percentage)
        FROM (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY equipment_id ORDER BY generate_time DESC) rn
            FROM system_metrics
            WHERE equipment_id IN (:equipmentIds)
        ) ranked
        WHERE rn <= :limit
        GROUP BY equipment_id
    """, nativeQuery = true)
    List<Object[]> getMemoryUsageStatsBatch(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("limit") int limit
    );

    // 그래프용 메트릭(5초 버킷) 조회 (native)
    @Query(value = """
        SELECT 
            time_bucket('5 seconds', sm.generate_time),
            AVG(CASE WHEN sm.cpu_user > 0 THEN sm.cpu_user END),
            AVG(CASE WHEN sm.cpu_system > 0 THEN sm.cpu_system END),
            AVG(sm.used_memory_percentage),
            AVG(sm.load_avg1)
        FROM system_metrics sm
        WHERE sm.equipment_id = :equipmentId
        AND sm.generate_time BETWEEN :startTime AND :endTime
        AND sm.context_switches IS NOT NULL
        AND sm.cpu_user > 0
        GROUP BY time_bucket('5 seconds', sm.generate_time)
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> findMetricsForChartNative(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 그래프용 DTO 매핑 메서드
    default List<MetricChartData> findMetricsForChart(
            Long equipmentId, LocalDateTime startTime, LocalDateTime endTime
    ) {
        return findMetricsForChartNative(equipmentId, startTime, endTime)
                .stream()
                .map(row -> new MetricChartData(
                        (java.sql.Timestamp) row[0],
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).doubleValue(),
                        ((Number) row[3]).doubleValue(),
                        ((Number) row[4]).doubleValue()
                ))
                .toList();
    }

    // 특정 장비의 특정 시간 메트릭 조회
    Optional<SystemMetric> findByEquipmentIdAndGenerateTime(Long equipmentId, LocalDateTime generateTime);

    // CPU 사용률 통계 조회
    @Query(value = """
        SELECT 
            AVG(100 - cpu_idle),
            MAX(100 - cpu_idle),
            MIN(100 - cpu_idle)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Object[] getCpuUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 여러 장비의 최근 N개 CPU 사용률 통계 조회
    @Query(value = """
        SELECT equipment_id,
               AVG(100 - cpu_idle),
               MAX(100 - cpu_idle),
               MIN(100 - cpu_idle)
        FROM (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY equipment_id ORDER BY generate_time DESC) rn
            FROM system_metrics
            WHERE equipment_id IN (:equipmentIds)
        ) ranked
        WHERE rn <= :limit
        GROUP BY equipment_id
    """, nativeQuery = true)
    List<Object[]> getCpuUsageStatsBatch(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("limit") int limit
    );

    // CPU 집계 (1분 단위)
    @Query(value = """
        SELECT 
            time_bucket('1 minute', generate_time),
            AVG(100 - cpu_idle),
            MAX(100 - cpu_idle),
            MIN(100 - cpu_idle),
            AVG(load_avg1),
            SUM(context_switches),
            COUNT(*)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> getCpuAggregatedStats1Minute(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // CPU 집계 (5분)
    @Query(value = """
        SELECT 
            time_bucket('5 minutes', generate_time),
            AVG(100 - cpu_idle),
            MAX(100 - cpu_idle),
            MIN(100 - cpu_idle),
            AVG(load_avg1),
            SUM(context_switches),
            COUNT(*)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> getCpuAggregatedStats5Minutes(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // CPU 집계 (1시간)
    @Query(value = """
        SELECT 
            time_bucket('1 hour', generate_time),
            AVG(100 - cpu_idle),
            MAX(100 - cpu_idle),
            MIN(100 - cpu_idle),
            AVG(load_avg1),
            SUM(context_switches),
            COUNT(*)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> getCpuAggregatedStats1Hour(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // CPU 집계 (1일)
    @Query(value = """
        SELECT 
            time_bucket('1 day', generate_time),
            AVG(100 - cpu_idle),
            MAX(100 - cpu_idle),
            MIN(100 - cpu_idle),
            AVG(load_avg1),
            SUM(context_switches),
            COUNT(*)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> getCpuAggregatedStats1Day(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 메모리 사용률 통계 조회
    @Query(value = """
        SELECT 
            AVG(used_memory_percentage),
            MAX(used_memory_percentage),
            MIN(used_memory_percentage)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Object[] getMemoryUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 메모리 집계 (1분)
    @Query(value = """
        SELECT 
            time_bucket('1 minute', generate_time),
            AVG(used_memory_percentage),
            MAX(used_memory_percentage),
            MIN(used_memory_percentage),
            AVG(used_swap_percentage),
            COUNT(*)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> getMemoryAggregatedStats1Minute(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 메모리 집계 (5분)
    @Query(value = """
        SELECT 
            time_bucket('5 minutes', generate_time),
            AVG(used_memory_percentage),
            MAX(used_memory_percentage),
            MIN(used_memory_percentage),
            AVG(used_swap_percentage),
            COUNT(*)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> getMemoryAggregatedStats5Minutes(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 메모리 집계 (1시간)
    @Query(value = """
        SELECT 
            time_bucket('1 hour', generate_time),
            AVG(used_memory_percentage),
            MAX(used_memory_percentage),
            MIN(used_memory_percentage),
            AVG(used_swap_percentage),
            COUNT(*)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> getMemoryAggregatedStats1Hour(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 메모리 집계 (1일)
    @Query(value = """
        SELECT 
            time_bucket('1 day', generate_time),
            AVG(used_memory_percentage),
            MAX(used_memory_percentage),
            MIN(used_memory_percentage),
            AVG(used_swap_percentage),
            COUNT(*)
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1
    """, nativeQuery = true)
    List<Object[]> getMemoryAggregatedStats1Day(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 최근 N개 메트릭 조회
    @Query(value = """
        SELECT *
        FROM system_metrics
        WHERE equipment_id = :equipmentId
        ORDER BY generate_time DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<SystemMetric> findRecentMetrics(
            @Param("equipmentId") Long equipmentId,
            @Param("limit") int limit
    );

    // 여러 장비의 평균 CPU 통계
    @Query(value = """
        SELECT 
            AVG(100 - cpu_idle) as avgCpuUsage,
            MAX(100 - cpu_idle) as maxCpuUsage,
            MIN(100 - cpu_idle) as minCpuUsage,
            AVG(load_avg1) as avgLoadAvg1,
            AVG(load_avg5) as avgLoadAvg5,
            AVG(load_avg15) as avgLoadAvg15,
            COUNT(DISTINCT equipment_id) as equipmentCount
        FROM system_metrics
        WHERE equipment_id IN :equipmentIds
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Map<String, Object> getAverageCpuStatsByEquipmentIds(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 여러 장비의 평균 메모리 통계
    @Query(value = """
        SELECT 
            AVG(used_memory_percentage),
            MAX(used_memory_percentage),
            MIN(used_memory_percentage),
            SUM(total_memory),
            SUM(used_memory),
            AVG(used_swap_percentage),
            COUNT(DISTINCT equipment_id)
        FROM system_metrics
        WHERE equipment_id IN :equipmentIds
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Map<String, Object> getAverageMemoryStatsByEquipmentIds(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

}
