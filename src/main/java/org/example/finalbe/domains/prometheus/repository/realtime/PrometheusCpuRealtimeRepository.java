package org.example.finalbe.domains.prometheus.repository.realtime;

import org.example.finalbe.domains.prometheus.domain.PrometheusCpuRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrometheusCpuRealtimeRepository extends JpaRepository<PrometheusCpuRealtime, Long> {

    /**
     * 최신 1개 조회 (SSE용)
     */
    @Query(value = "SELECT * FROM prometheus_cpu_realtime ORDER BY time DESC LIMIT 1", nativeQuery = true)
    Optional<PrometheusCpuRealtime> findLatest();

    /**
     * 특정 시간 이후 데이터 조회 (초기 5분 로드용)
     */
    @Query("SELECT c FROM PrometheusCpuRealtime c WHERE c.time >= :startTime ORDER BY c.time ASC")
    List<PrometheusCpuRealtime> findByTimeAfter(@Param("startTime") Instant startTime);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT c FROM PrometheusCpuRealtime c WHERE c.time BETWEEN :startTime AND :endTime ORDER BY c.time ASC")
    List<PrometheusCpuRealtime> findByTimeBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * 특정 시간 이전의 오래된 데이터 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM PrometheusCpuRealtime c WHERE c.time < :cutoffTime")
    int deleteByTimeBefore(@Param("cutoffTime") Instant cutoffTime);

    /**
     * 특정 인스턴스의 가장 최근 CPU 메트릭 조회
     */
    @Query("SELECT c FROM PrometheusCpuRealtime c WHERE c.instanceId = :instanceId " +
            "AND c.time = (SELECT MAX(c2.time) FROM PrometheusCpuRealtime c2 WHERE c2.instanceId = :instanceId)")
    PrometheusCpuRealtime findLatestByInstanceId(@Param("instanceId") Integer instanceId);


    /**
     * 전체 레코드 수 조회
     */
    long count();

    /**
     * 특정 인스턴스의 특정 시간 범위 데이터 조회
     */
    @Query("SELECT c FROM PrometheusCpuRealtime c WHERE c.instanceId = :instanceId " +
            "AND c.time BETWEEN :startTime AND :endTime ORDER BY c.time ASC")
    List<PrometheusCpuRealtime> findByInstanceIdAndTimeRange(
            @Param("instanceId") Integer instanceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * 모든 인스턴스의 특정 시간 범위 데이터 조회
     */
    @Query("SELECT c FROM PrometheusCpuRealtime c WHERE c.time BETWEEN :startTime AND :endTime " +
            "ORDER BY c.instanceId, c.time ASC")
    List<PrometheusCpuRealtime> findByTimeRange(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}