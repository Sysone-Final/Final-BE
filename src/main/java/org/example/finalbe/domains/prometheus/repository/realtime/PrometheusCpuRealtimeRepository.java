package org.example.finalbe.domains.prometheus.repository.realtime;

import org.example.finalbe.domains.prometheus.domain.PrometheusCpuRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
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
}