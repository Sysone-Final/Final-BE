package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NetworkMetricRepository extends JpaRepository<NetworkMetric, Long> {

    // ==================== 기본 조회 ====================

    @Query("SELECT nm FROM NetworkMetric nm " +
            "WHERE nm.equipmentId = :equipmentId " +
            "AND nm.generateTime BETWEEN :startTime AND :endTime " +
            "ORDER BY nm.generateTime ASC, nm.nicName ASC")
    List<NetworkMetric> findByEquipmentIdAndTimeRange(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 장비의 최신 메트릭 조회 (모든 NIC 포함)
     */
    @Query(value = "SELECT * FROM network_metrics " +
            "WHERE equipment_id = :equipmentId " +
            "AND generate_time = (" +
            "  SELECT MAX(generate_time) FROM network_metrics WHERE equipment_id = :equipmentId" +
            ")",
            nativeQuery = true)
    List<NetworkMetric> findLatestByEquipmentId(@Param("equipmentId") Long equipmentId);


    // ==================== 네트워크 통계 조회 ====================

    /**
     * 네트워크 사용률 통계 (평균, 최대, 최소) - NIC 기준
     */
    @Query(value = "SELECT " +
            "AVG(rx_usage) AS avg_rx, " +
            "MAX(rx_usage) AS max_rx, " +
            "MIN(rx_usage) AS min_rx " +
            "FROM network_metrics " +
            "WHERE equipment_id = :equipmentId " +
            "AND generate_time BETWEEN :startTime AND :endTime",
            nativeQuery = true)
    Object[] getNetworkUsageStats(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== 집계 쿼리 (time_bucket) ====================

    /**
     * 1분 단위 집계 (Bps는 SUM, Usage는 AVG)
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('1 minute', generate_time) AS bucket, " +
                    "  SUM(in_bytes_per_sec) AS total_in_bps, " +
                    "  SUM(out_bytes_per_sec) AS total_out_bps, " +
                    "  AVG(rx_usage) AS avg_rx_usage, " +
                    "  AVG(tx_usage) AS avg_tx_usage, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM network_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getNetworkAggregatedStats1Minute(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 5분 단위 집계
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('5 minutes', generate_time) AS bucket, " +
                    "  SUM(in_bytes_per_sec) AS total_in_bps, " +
                    "  SUM(out_bytes_per_sec) AS total_out_bps, " +
                    "  AVG(rx_usage) AS avg_rx_usage, " +
                    "  AVG(tx_usage) AS avg_tx_usage, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM network_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getNetworkAggregatedStats5Minutes(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 1시간 단위 집계
     */
    @Query(value =
            "SELECT " +
                    "  time_bucket('1 hour', generate_time) AS bucket, " +
                    "  SUM(in_bytes_per_sec) AS total_in_bps, " +
                    "  SUM(out_bytes_per_sec) AS total_out_bps, " +
                    "  AVG(rx_usage) AS avg_rx_usage, " +
                    "  AVG(tx_usage) AS avg_tx_usage, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM network_metrics " +
                    "WHERE equipment_id = :equipmentId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getNetworkAggregatedStats1Hour(
            @Param("equipmentId") Long equipmentId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== 일괄 조회 (Batch) ====================

    /**
     * 여러 장비의 최신 메트릭 일괄 조회 (모든 NIC 포함)
     */
    @Query(value =
            "SELECT * FROM network_metrics " +
                    "WHERE (equipment_id, generate_time) IN (" +
                    "  SELECT equipment_id, MAX(generate_time) " +
                    "  FROM network_metrics " +
                    "  WHERE equipment_id IN (:equipmentIds) " +
                    "  GROUP BY equipment_id" +
                    ")",
            nativeQuery = true)
    List<NetworkMetric> findLatestByEquipmentIds(@Param("equipmentIds") List<Long> equipmentIds);

    /**
     * 여러 장비의 최근 N개 데이터로 네트워크 사용률 통계 일괄 계산
     */
    @Query(value =
            "SELECT " +
                    "  equipment_id, " +
                    "  AVG(rx_usage) AS avg_rx, " +
                    "  MAX(rx_usage) AS max_rx, " +
                    "  MIN(rx_usage) AS min_rx " +
                    "FROM (" +
                    "  SELECT *, ROW_NUMBER() OVER (PARTITION BY equipment_id ORDER BY generate_time DESC) AS rn " +
                    "  FROM network_metrics " +
                    "  WHERE equipment_id IN (:equipmentIds) " +
                    ") AS ranked " +
                    "WHERE rn <= :limit " +
                    "GROUP BY equipment_id",
            nativeQuery = true)
    List<Object[]> getNetworkUsageStatsBatch(
            @Param("equipmentIds") List<Long> equipmentIds,
            @Param("limit") int limit
    );
}