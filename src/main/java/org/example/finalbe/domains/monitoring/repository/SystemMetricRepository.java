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

    /**
     * 여러 장비의 최신 메트릭 조회
     */
    @Query(value = "SELECT DISTINCT ON (equipment_id) * FROM system_metrics " +
            "WHERE equipment_id IN :equipmentIds " +
            "ORDER BY equipment_id, generate_time DESC",
            nativeQuery = true)
    List<SystemMetric> findLatestByEquipmentIds(@Param("equipmentIds") List<Long> equipmentIds);

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

    /**
     * ✅ 그래프용 조회: 5초 단위 집계, 0값 제외
     *
     * TimescaleDB의 time_bucket 함수 사용으로 불규칙한 데이터 간격에도 정확한 집계 가능
     * 실제 데이터는 약 2~3초 간격으로 들어오지만, 5초 버킷으로 자동 그룹화
     */
    @Query(value = """
        SELECT 
            time_bucket('5 seconds', sm.generate_time) as timestamp,
            AVG(CASE WHEN sm.cpu_user > 0 THEN sm.cpu_user ELSE NULL END) as cpu_user,
            AVG(CASE WHEN sm.cpu_system > 0 THEN sm.cpu_system ELSE NULL END) as cpu_system,
            AVG(sm.used_memory_percentage) as memory_usage,
            AVG(sm.load_avg1) as load_avg
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

    /**
     * ✅ 그래프용 조회 (DTO 매핑 버전)
     * Native Query 결과를 서비스 레이어에서 DTO로 변환하여 사용
     */
    default List<MetricChartData> findMetricsForChart(
            Long equipmentId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        List<Object[]> results = findMetricsForChartNative(equipmentId, startTime, endTime);

        return results.stream()
                .map(row -> new MetricChartData(
                        (java.sql.Timestamp) row[0],  // timestamp
                        ((Number) row[1]).doubleValue(),  // cpuUser
                        ((Number) row[2]).doubleValue(),  // cpuSystem
                        ((Number) row[3]).doubleValue(),  // memoryUsage
                        ((Number) row[4]).doubleValue()   // loadAvg
                ))
                .toList();
    }

    /**
     * equipmentId와 generateTime으로 조회 (중복 체크용)
     */
    Optional<SystemMetric> findByEquipmentIdAndGenerateTime(Long equipmentId, LocalDateTime generateTime);

    // ==================== CPU 통계 조회 ====================

    /**
     * CPU 사용률 통계 (평균, 최대, 최소)
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
     * 여러 장비의 최근 N개 데이터로 CPU 통계 일괄 계산
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
     * 시간대별 CPU 평균 사용률 (1분 단위 집계)
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

    /**
     * 시간대별 CPU 평균 사용률 (1일 단위 집계)
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('1 day', generate_time) AS bucket, " +
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
    List<Object[]> getCpuAggregatedStats1Day(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== 메모리 통계 조회 ====================

    /**
     * 메모리 사용률 통계 (평균, 최대, 최소)
     */
    @Query(value = "SELECT " +
            "AVG(used_memory_percentage) as avg_mem, " +
            "MAX(used_memory_percentage) as max_mem, " +
            "MIN(used_memory_percentage) as min_mem " +
            "FROM system_metrics " +
            "WHERE equipment_id = :equipmentId " +
            "AND generate_time BETWEEN :startTime AND :endTime",
            nativeQuery = true)
    Object[] getMemoryUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
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
     * 시간대별 메모리 평균 사용률 (1일 단위 집계)
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('1 day', generate_time) AS bucket, " +
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
    List<Object[]> getMemoryAggregatedStats1Day(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== 추가 유틸리티 메서드 ====================

    /**
     * 최근 N개의 메트릭 조회 (실시간 그래프용)
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


    // SystemMetricRepository.java

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

    /**
     * 여러 장비의 평균 메모리 통계 조회
     */
    @Query(value = """
        SELECT 
            AVG(used_memory_percentage) as avgMemoryUsage,
            MAX(used_memory_percentage) as maxMemoryUsage,
            MIN(used_memory_percentage) as minMemoryUsage,
            SUM(total_memory) as totalMemory,
            SUM(used_memory) as totalUsedMemory,
            AVG(used_swap_percentage) as avgSwapUsage,
            COUNT(DISTINCT equipment_id) as equipmentCount
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