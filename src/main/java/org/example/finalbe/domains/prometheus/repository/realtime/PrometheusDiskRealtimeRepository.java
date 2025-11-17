package org.example.finalbe.domains.prometheus.repository.realtime;

import org.example.finalbe.domains.prometheus.domain.PrometheusDiskRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrometheusDiskRealtimeRepository extends JpaRepository<PrometheusDiskRealtime, Long> {

    /**
     * 최신 1개 조회 (SSE용)
     */
    @Query(value = "SELECT * FROM prometheus_disk_realtime ORDER BY time DESC LIMIT 1", nativeQuery = true)
    Optional<PrometheusDiskRealtime> findLatest();

    /**
     * 특정 시간 이후 데이터 조회 (초기 5분 로드용)
     */
    @Query("SELECT d FROM PrometheusDiskRealtime d WHERE d.time >= :startTime ORDER BY d.time ASC")
    List<PrometheusDiskRealtime> findByTimeAfter(@Param("startTime") Instant startTime);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT d FROM PrometheusDiskRealtime d WHERE d.time BETWEEN :startTime AND :endTime ORDER BY d.time ASC")
    List<PrometheusDiskRealtime> findByTimeBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}