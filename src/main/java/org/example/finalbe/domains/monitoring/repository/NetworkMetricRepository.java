/**
 * 작성자: 황요한
 * 네트워크 메트릭 조회 및 집계 기능 제공 Repository
 */
package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface NetworkMetricRepository extends JpaRepository<NetworkMetric, Long> {

    // 장비의 기간별 네트워크 메트릭 조회
    @Query("""
        SELECT nm FROM NetworkMetric nm
        WHERE nm.equipmentId = :equipmentId
        AND nm.generateTime BETWEEN :startTime AND :endTime
        ORDER BY nm.generateTime ASC, nm.nicName ASC
    """)
    List<NetworkMetric> findByEquipmentIdAndTimeRange(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 장비의 최신 네트워크 메트릭 조회 (모든 NIC)
    @Query(value = """
        SELECT * FROM network_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time = (
            SELECT MAX(generate_time)
            FROM network_metrics WHERE equipment_id = :equipmentId
        )
    """, nativeQuery = true)
    List<NetworkMetric> findLatestByEquipmentId(@Param("equipmentId") Long equipmentId);

    // 네트워크 사용률 통계 조회 (평균/최대/최소)
    @Query(value = """
        SELECT 
            AVG(rx_usage),
            MAX(rx_usage),
            MIN(rx_usage)
        FROM network_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Object[] getNetworkUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1분 단위 네트워크 집계 조회
    @Query(value = """
        SELECT 
            time_bucket('1 minute', generate_time),
            SUM(in_bytes_per_sec),
            SUM(out_bytes_per_sec),
            AVG(rx_usage),
            AVG(tx_usage),
            COUNT(*)
        FROM network_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1 ASC
    """, nativeQuery = true)
    List<Object[]> getNetworkAggregatedStats1Minute(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 5분 단위 네트워크 집계 조회
    @Query(value = """
        SELECT 
            time_bucket('5 minutes', generate_time),
            SUM(in_bytes_per_sec),
            SUM(out_bytes_per_sec),
            AVG(rx_usage),
            AVG(tx_usage),
            COUNT(*)
        FROM network_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1 ASC
    """, nativeQuery = true)
    List<Object[]> getNetworkAggregatedStats5Minutes(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1시간 단위 네트워크 집계 조회
    @Query(value = """
        SELECT 
            time_bucket('1 hour', generate_time),
            SUM(in_bytes_per_sec),
            SUM(out_bytes_per_sec),
            AVG(rx_usage),
            AVG(tx_usage),
            COUNT(*)
        FROM network_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1 ASC
    """, nativeQuery = true)
    List<Object[]> getNetworkAggregatedStats1Hour(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1일 단위 네트워크 집계 조회
    @Query(value = """
        SELECT 
            time_bucket('1 day', generate_time),
            SUM(in_bytes_per_sec),
            SUM(out_bytes_per_sec),
            AVG(rx_usage),
            AVG(tx_usage),
            COUNT(*)
        FROM network_metrics
        WHERE equipment_id = :equipmentId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY 1
        ORDER BY 1 ASC
    """, nativeQuery = true)
    List<Object[]> getNetworkAggregatedStats1Day(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 여러 장비의 최신 네트워크 메트릭 일괄 조회
    @Query(value = """
        SELECT *
        FROM network_metrics
        WHERE (equipment_id, generate_time) IN (
            SELECT equipment_id, MAX(generate_time)
            FROM network_metrics
            WHERE equipment_id IN (:equipmentIds)
            GROUP BY equipment_id
        )
    """, nativeQuery = true)
    List<NetworkMetric> findLatestByEquipmentIds(@Param("equipmentIds") List<Long> equipmentIds);

    // 여러 장비의 최근 N개 데이터 기반 사용률 통계 조회
    @Query(value = """
        SELECT 
            equipment_id,
            AVG(rx_usage),
            MAX(rx_usage),
            MIN(rx_usage)
        FROM (
            SELECT *, ROW_NUMBER() OVER (PARTITION BY equipment_id ORDER BY generate_time DESC) AS rn
            FROM network_metrics
            WHERE equipment_id IN (:equipmentIds)
        ) ranked
        WHERE rn <= :limit
        GROUP BY equipment_id
    """, nativeQuery = true)
    List<Object[]> getNetworkUsageStatsBatch(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("limit") int limit
    );

    // 특정 장비의 특정 NIC 특정 시간 조회 (UPSERT 활용)
    Optional<NetworkMetric> findByEquipmentIdAndNicNameAndGenerateTime(
            Long equipmentId,
            String nicName,
            LocalDateTime generateTime
    );

    // 여러 장비의 네트워크 평균 통계 조회
    @Query(value = """
        SELECT 
            SUM(in_bytes_per_sec) AS totalInBps,
            SUM(out_bytes_per_sec) AS totalOutBps,
            AVG(rx_usage) AS avgRxUsage,
            AVG(tx_usage) AS avgTxUsage,
            SUM(in_error_pkts_tot) AS totalInErrors,
            SUM(out_error_pkts_tot) AS totalOutErrors,
            COUNT(DISTINCT equipment_id) AS equipmentCount
        FROM network_metrics
        WHERE equipment_id IN :equipmentIds
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Map<String, Object> getAverageNetworkStatsByEquipmentIds(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

}
