/**
 * 작성자: 황요한
 * 환경(온도/습도) 메트릭 조회 및 집계용 Repository
 */
package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface EnvironmentMetricRepository extends JpaRepository<EnvironmentMetric, Long> {

    // 랙의 시간 범위 내 환경 메트릭 조회
    @Query("""
        SELECT em FROM EnvironmentMetric em
        WHERE em.rackId = :rackId
        AND em.generateTime BETWEEN :startTime AND :endTime
        ORDER BY em.generateTime ASC
    """)
    List<EnvironmentMetric> findByRackIdAndTimeRange(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 랙의 최신 환경 메트릭 조회
    @Query(value = """
        SELECT * FROM environment_metrics
        WHERE rack_id = :rackId
        ORDER BY generate_time DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<EnvironmentMetric> findLatestByRackId(@Param("rackId") Long rackId);

    // 기본 환경 통계 조회 (평균/최대/최소)
    @Query(value = """
        SELECT 
            AVG(temperature),
            MAX(temperature),
            MIN(temperature),
            AVG(humidity)
        FROM environment_metrics
        WHERE rack_id = :rackId
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Object[] getEnvironmentStats(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1분 단위 환경 집계 통계
    @Query(value = """
        SELECT 
            time_bucket('1 minute', generate_time) AS bucket,
            AVG(temperature),
            MAX(temperature),
            MIN(temperature),
            AVG(humidity),
            COUNT(*)
        FROM environment_metrics
        WHERE rack_id = :rackId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY bucket
        ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getEnvironmentAggregatedStats1Minute(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 5분 단위 환경 집계 통계
    @Query(value = """
        SELECT 
            time_bucket('5 minutes', generate_time) AS bucket,
            AVG(temperature),
            MAX(temperature),
            MIN(temperature),
            AVG(humidity),
            COUNT(*)
        FROM environment_metrics
        WHERE rack_id = :rackId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY bucket
        ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getEnvironmentAggregatedStats5Minutes(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1시간 단위 환경 집계 통계
    @Query(value = """
        SELECT 
            time_bucket('1 hour', generate_time) AS bucket,
            AVG(temperature),
            MAX(temperature),
            MIN(temperature),
            AVG(humidity),
            COUNT(*)
        FROM environment_metrics
        WHERE rack_id = :rackId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY bucket
        ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getEnvironmentAggregatedStats1Hour(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 1일 단위 환경 집계 통계
    @Query(value = """
        SELECT 
            time_bucket('1 day', generate_time) AS bucket,
            AVG(temperature),
            MAX(temperature),
            MIN(temperature),
            AVG(humidity),
            COUNT(*)
        FROM environment_metrics
        WHERE rack_id = :rackId
        AND generate_time BETWEEN :startTime AND :endTime
        GROUP BY bucket
        ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getEnvironmentAggregatedStats1Day(
            @Param("rackId") Long rackId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // 여러 랙의 최신 환경 메트릭 일괄 조회
    @Query(value = """
        SELECT DISTINCT ON (rack_id) *
        FROM environment_metrics
        WHERE rack_id IN (:rackIds)
        ORDER BY rack_id, generate_time DESC
    """, nativeQuery = true)
    List<EnvironmentMetric> findLatestByRackIds(@Param("rackIds") List<Long> rackIds);

    // 여러 랙의 환경 통계 일괄 조회 (평균/최대/최소)
    @Query(value = """
        SELECT 
            rack_id,
            AVG(temperature),
            MAX(temperature),
            MIN(temperature)
        FROM (
            SELECT *,
                ROW_NUMBER() OVER (PARTITION BY rack_id ORDER BY generate_time DESC) AS rn
            FROM environment_metrics
            WHERE rack_id IN (:rackIds)
        ) ranked
        WHERE rn <= :limit
        GROUP BY rack_id
    """, nativeQuery = true)
    List<Object[]> getEnvironmentStatsBatch(
            @Param("rackIds") List<Long> rackIds,
            @Param("limit") int limit
    );

    // 특정 시점의 환경 메트릭 조회
    Optional<EnvironmentMetric> findByRackIdAndGenerateTime(
            Long rackId,
            LocalDateTime generateTime
    );

    // 여러 랙의 평균 환경 통계 조회
    @Query(value = """
        SELECT 
            AVG(temperature) AS avgTemperature,
            MAX(temperature) AS maxTemperature,
            MIN(temperature) AS minTemperature,
            AVG(humidity) AS avgHumidity,
            MAX(humidity) AS maxHumidity,
            MIN(humidity) AS minHumidity,
            SUM(CASE WHEN temperature_warning = true THEN 1 ELSE 0 END) AS temperatureWarnings,
            SUM(CASE WHEN humidity_warning = true THEN 1 ELSE 0 END) AS humidityWarnings,
            COUNT(DISTINCT rack_id) AS rackCount
        FROM environment_metrics
        WHERE rack_id IN :rackIds
        AND generate_time BETWEEN :startTime AND :endTime
    """, nativeQuery = true)
    Map<String, Object> getAverageEnvironmentStatsByRackIds(
            @Param("rackIds") List<Long> rackIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
