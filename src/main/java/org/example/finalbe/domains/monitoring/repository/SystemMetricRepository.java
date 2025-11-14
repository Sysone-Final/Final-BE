package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SystemMetric Repository
 * CPU, 메모리, SWAP 메트릭 조회
 */
@Repository
public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {

    // ==================== 기본 조회 ====================

    /**
     * 특정 장비의 시간 범위 내 메트릭 조회
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 메트릭 리스트 (시간순 정렬)
     */
    @Query("SELECT sm FROM SystemMetric sm " +
            "WHERE sm.equipmentId = :equipmentId " +
            "AND sm.generateTime BETWEEN :startTime AND :endTime " +
            "ORDER BY sm.generateTime ASC")
    List<SystemMetric> findByEquipmentIdAndTimeRange(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 장비의 최신 메트릭 조회
     *
     * @param equipmentId 장비 ID
     * @return 최신 메트릭
     */
    @Query(value = "SELECT * FROM system_metrics " +
            "WHERE equipment_id = :equipmentId " +
            "ORDER BY generate_time DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<SystemMetric> findLatestByEquipmentId(@Param("equipmentId") Long equipmentId);

    /**
     * 여러 장비의 시간 범위 내 메트릭 조회
     *
     * @param equipmentIds 장비 ID 리스트
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 메트릭 리스트
     */
    @Query("SELECT sm FROM SystemMetric sm " +
            "WHERE sm.equipmentId IN :equipmentIds " +
            "AND sm.generateTime BETWEEN :startTime AND :endTime " +
            "ORDER BY sm.equipmentId, sm.generateTime ASC")
    List<SystemMetric> findByEquipmentIdsAndTimeRange(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== CPU 통계 조회 ====================

    /**
     * CPU 사용률 통계 (평균, 최대, 최소)
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return [avg, max, min]
     */
    @Query(value = "SELECT " +
            "AVG(100 - cpu_idle) AS avg_cpu, " +
            "MAX(100 - cpu_idle) AS max_cpu, " +
            "MIN(100 - cpu_idle) AS min_cpu " +
            "FROM system_metrics " +
            "WHERE equipment_id = :equipmentId " +
            "AND generate_time BETWEEN :startTime AND :endTime",
            nativeQuery = true)
    Object[] getCpuUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );


    /**
     * 시간대별 CPU 평균 사용률 (1분 단위 집계)
     * TimescaleDB의 time_bucket 사용
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 1분 단위 집계 데이터
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('1 minute', generate_time) AS bucket, " +
                    "  AVG(100 - cpu_idle) AS avg_cpu_usage, " +
                    "  MAX(100 - cpu_idle) AS max_cpu_usage, " +
                    "  MIN(100 - cpu_idle) AS min_cpu_usage, " +
                    "  AVG(load_avg1) AS avg_load, " +
                    "  SUM(context_switches) AS total_context_switches, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM system_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getCpuAggregatedStats1Minute(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 시간대별 CPU 평균 사용률 (5분 단위 집계)
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 5분 단위 집계 데이터
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('5 minutes', generate_time) AS bucket, " +
                    "  AVG(100 - cpu_idle) AS avg_cpu_usage, " +
                    "  MAX(100 - cpu_idle) AS max_cpu_usage, " +
                    "  MIN(100 - cpu_idle) AS min_cpu_usage, " +
                    "  AVG(load_avg1) AS avg_load, " +
                    "  SUM(context_switches) AS total_context_switches, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM system_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getCpuAggregatedStats5Minutes(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 시간대별 CPU 평균 사용률 (1시간 단위 집계)
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 1시간 단위 집계 데이터
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('1 hour', generate_time) AS bucket, " +
                    "  AVG(100 - cpu_idle) AS avg_cpu_usage, " +
                    "  MAX(100 - cpu_idle) AS max_cpu_usage, " +
                    "  MIN(100 - cpu_idle) AS min_cpu_usage, " +
                    "  AVG(load_avg1) AS avg_load, " +
                    "  SUM(context_switches) AS total_context_switches, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM system_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getCpuAggregatedStats1Hour(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== 최신 데이터 조회 (실시간 대시보드용) ====================

    /**
     * 최근 N개의 메트릭 조회 (실시간 그래프용)
     *
     * @param equipmentId 장비 ID
     * @param limit 조회할 개수
     * @return 최근 메트릭 리스트
     */
    @Query(value =
            "SELECT * FROM system_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "ORDER BY generate_time DESC " +
                    "LIMIT :limit",
            nativeQuery = true)
    List<SystemMetric> findRecentMetrics(
            @Param("equipmentId") Long equipmentId,
            @Param("limit") int limit
    );


    /**
     * 메모리 사용률 통계
     */
    @Query("SELECT " +
            "AVG(sm.usedMemoryPercentage), " +
            "MAX(sm.usedMemoryPercentage), " +
            "MIN(sm.usedMemoryPercentage) " +
            "FROM SystemMetric sm " +
            "WHERE sm.equipmentId = :equipmentId " +
            "AND sm.generateTime BETWEEN :startTime AND :endTime")
    Object[] getMemoryUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 여러 장비의 최신 메트릭 일괄 조회
     *
     * @param equipmentIds 장비 ID 리스트
     * @return 각 장비별 최신 메트릭
     */
    @Query(value =
            "SELECT DISTINCT ON (equipment_id) * " +
                    "FROM system_metrics " +
                    "WHERE equipment_id IN (:equipmentIds) " +
                    "ORDER BY equipment_id, generate_time DESC",
            nativeQuery = true)
    List<SystemMetric> findLatestByEquipmentIds(@Param("equipmentIds") List<Long> equipmentIds);

    /**
     * 여러 장비의 최근 N개 데이터로 CPU 통계 일괄 계산
     * 각 장비별로 최근 limit개 데이터의 평균/최대/최소 반환
     */
    @Query(value =
            "SELECT " +
                    "  equipment_id, " +
                    "  AVG(100 - cpu_idle) AS avg_cpu, " +
                    "  MAX(100 - cpu_idle) AS max_cpu, " +
                    "  MIN(100 - cpu_idle) AS min_cpu " +
                    "FROM (" +
                    "  SELECT *, ROW_NUMBER() OVER (PARTITION BY equipment_id ORDER BY generate_time DESC) AS rn " +
                    "  FROM system_metrics " +
                    "  WHERE equipment_id IN (:equipmentIds) " +
                    ") AS ranked " +
                    "WHERE rn <= :limit " +
                    "GROUP BY equipment_id",
            nativeQuery = true)
    List<Object[]> getCpuUsageStatsBatch(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("limit") int limit
    );

    /**
     * 시간대별 메모리 평균 사용률 (1분 단위 집계)
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('1 minute', generate_time) AS bucket, " +
                    "  AVG(used_memory_percentage) AS avg_mem_usage, " +
                    "  MAX(used_memory_percentage) AS max_mem_usage, " +
                    "  MIN(used_memory_percentage) AS min_mem_usage, " +
                    "  AVG(used_swap_percentage) AS avg_swap_usage, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM system_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getMemoryAggregatedStats1Minute(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 시간대별 메모리 평균 사용률 (5분 단위 집계)
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('5 minutes', generate_time) AS bucket, " +
                    "  AVG(used_memory_percentage) AS avg_mem_usage, " +
                    "  MAX(used_memory_percentage) AS max_mem_usage, " +
                    "  MIN(used_memory_percentage) AS min_mem_usage, " +
                    "  AVG(used_swap_percentage) AS avg_swap_usage, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM system_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getMemoryAggregatedStats5Minutes(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 시간대별 메모리 평균 사용률 (1시간 단위 집계)
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('1 hour', generate_time) AS bucket, " +
                    "  AVG(used_memory_percentage) AS avg_mem_usage, " +
                    "  MAX(used_memory_percentage) AS max_mem_usage, " +
                    "  MIN(used_memory_percentage) AS min_mem_usage, " +
                    "  AVG(used_swap_percentage) AS avg_swap_usage, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM system_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getMemoryAggregatedStats1Hour(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 여러 장비의 최근 N개 데이터로 메모리 통계 일괄 계산
     */
    @Query(value =
            "SELECT " +
                    "  equipment_id, " +
                    "  AVG(used_memory_percentage) AS avg_mem, " +
                    "  MAX(used_memory_percentage) AS max_mem, " +
                    "  MIN(used_memory_percentage) AS min_mem " +
                    "FROM (" +
                    "  SELECT *, ROW_NUMBER() OVER (PARTITION BY equipment_id ORDER BY generate_time DESC) AS rn " +
                    "  FROM system_metrics " +
                    "  WHERE equipment_id IN (:equipmentIds) " +
                    ") AS ranked " +
                    "WHERE rn <= :limit " +
                    "GROUP BY equipment_id",
            nativeQuery = true)
    List<Object[]> getMemoryUsageStatsBatch(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("limit") int limit
    );

}