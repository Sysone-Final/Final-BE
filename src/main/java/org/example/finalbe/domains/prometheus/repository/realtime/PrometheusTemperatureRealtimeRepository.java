package org.example.finalbe.domains.prometheus.repository.realtime;

import org.example.finalbe.domains.prometheus.domain.PrometheusTemperatureRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrometheusTemperatureRealtimeRepository extends JpaRepository<PrometheusTemperatureRealtime, Long> {

    /**
     * 최신 1개 조회 (SSE용)
     */
    @Query(value = "SELECT * FROM prometheus_temperature_realtime ORDER BY time DESC LIMIT 1", nativeQuery = true)
    Optional<PrometheusTemperatureRealtime> findLatest();

    /**
     * 특정 시간 이후 데이터 조회 (초기 5분 로드용)
     */
    @Query("SELECT t FROM PrometheusTemperatureRealtime t WHERE t.time >= :startTime ORDER BY t.time ASC")
    List<PrometheusTemperatureRealtime> findByTimeAfter(@Param("startTime") Instant startTime);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT t FROM PrometheusTemperatureRealtime t WHERE t.time BETWEEN :startTime AND :endTime ORDER BY t.time ASC")
    List<PrometheusTemperatureRealtime> findByTimeBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * 특정 시간 이전의 오래된 데이터 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM PrometheusTemperatureRealtime t WHERE t.time < :cutoffTime")
    int deleteByTimeBefore(@Param("cutoffTime") Instant cutoffTime);
}