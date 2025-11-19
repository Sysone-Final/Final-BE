package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnvironmentMetricRepository extends JpaRepository<EnvironmentMetric, Long> {

    // ==================== 기본 조회 (rackId 기준) ====================

    @Query("SELECT em FROM EnvironmentMetric em " +
            "WHERE em.rackId = :rackId " +
            "AND em.generateTime BETWEEN :startTime AND :endTime " +
            "ORDER BY em.generateTime ASC")
    List<EnvironmentMetric> findByRackIdAndTimeRange(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value = "SELECT * FROM environment_metrics " +
            "WHERE rack_id = :rackId " +
            "ORDER BY generate_time DESC " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<EnvironmentMetric> findLatestByRackId(@Param("rackId") Long rackId);

    // ==================== 환경 통계 조회 ====================

    @Query(value = "SELECT " +
            "AVG(temperature) AS avg_temp, " +
            "MAX(temperature) AS max_temp, " +
            "MIN(temperature) AS min_temp, " +
            "AVG(humidity) AS avg_humidity " +
            "FROM environment_metrics " +
            "WHERE rack_id = :rackId " +
            "AND generate_time BETWEEN :startTime AND :endTime",
            nativeQuery = true)
    Object[] getEnvironmentStats(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ==================== 집계 쿼리 (time_bucket, rackId 기준) ====================

    @Query(value =
            "SELECT " +
                    "  time_bucket('1 minute', generate_time) AS bucket, " +
                    "  AVG(temperature) AS avg_temp, " +
                    "  MAX(temperature) AS max_temp, " +
                    "  MIN(temperature) AS min_temp, " +
                    "  AVG(humidity) AS avg_humidity, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM environment_metrics " +
                    "WHERE rack_id = :rackId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getEnvironmentAggregatedStats1Minute(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value =
            "SELECT " +
                    "  time_bucket('5 minutes', generate_time) AS bucket, " +
                    "  AVG(temperature) AS avg_temp, " +
                    "  MAX(temperature) AS max_temp, " +
                    "  MIN(temperature) AS min_temp, " +
                    "  AVG(humidity) AS avg_humidity, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM environment_metrics " +
                    "WHERE rack_id = :rackId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getEnvironmentAggregatedStats5Minutes(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value =
            "SELECT " +
                    "  time_bucket('1 hour', generate_time) AS bucket, " +
                    "  AVG(temperature) AS avg_temp, " +
                    "  MAX(temperature) AS max_temp, " +
                    "  MIN(temperature) AS min_temp, " +
                    "  AVG(humidity) AS avg_humidity, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM environment_metrics " +
                    "WHERE rack_id = :rackId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getEnvironmentAggregatedStats1Hour(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value =
            "SELECT " +
                    "  time_bucket('1 day', generate_time) AS bucket, " +
                    "  AVG(temperature) AS avg_temp, " +
                    "  MAX(temperature) AS max_temp, " +
                    "  MIN(temperature) AS min_temp, " +
                    "  AVG(humidity) AS avg_humidity, " +
                    "  COUNT(*) AS sample_count " +
                    "FROM environment_metrics " +
                    "WHERE rack_id = :rackId " +
                    "AND generate_time BETWEEN :startTime AND :endTime " +
                    "GROUP BY bucket " +
                    "ORDER BY bucket ASC",
            nativeQuery = true)
    List<Object[]> getEnvironmentAggregatedStats1Day(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    // ==================== 일괄 조회 (Batch, rackId 기준) ====================

    @Query(value =
            "SELECT DISTINCT ON (rack_id) * " +
                    "FROM environment_metrics " +
                    "WHERE rack_id IN (:rackIds) " +
                    "ORDER BY rack_id, generate_time DESC",
            nativeQuery = true)
    List<EnvironmentMetric> findLatestByRackIds(@Param("rackIds") List<Long> rackIds);

    @Query(value =
            "SELECT " +
                    "  rack_id, " +
                    "  AVG(temperature) AS avg_temp, " +
                    "  MAX(temperature) AS max_temp, " +
                    "  MIN(temperature) AS min_temp " +
                    "FROM (" +
                    "  SELECT *, ROW_NUMBER() OVER (PARTITION BY rack_id ORDER BY generate_time DESC) AS rn " +
                    "  FROM environment_metrics " +
                    "  WHERE rack_id IN (:rackIds) " +
                    ") AS ranked " +
                    "WHERE rn <= :limit " +
                    "GROUP BY rack_id",
            nativeQuery = true)
    List<Object[]> getEnvironmentStatsBatch(
            @Param("rackIds") List<Long> rackIds,
            @Param("limit") int limit
    );
}