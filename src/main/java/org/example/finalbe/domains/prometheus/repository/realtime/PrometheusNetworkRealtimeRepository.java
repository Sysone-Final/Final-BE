package org.example.finalbe.domains.prometheus.repository.realtime;

import org.example.finalbe.domains.prometheus.domain.PrometheusNetworkRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrometheusNetworkRealtimeRepository extends JpaRepository<PrometheusNetworkRealtime, Long> {

    /**
     * 최신 1개 조회 (SSE용)
     */
    @Query(value = "SELECT * FROM prometheus_network_realtime ORDER BY time DESC LIMIT 1", nativeQuery = true)
    Optional<PrometheusNetworkRealtime> findLatest();

    /**
     * 특정 시간 이후 데이터 조회 (초기 5분 로드용)
     */
    @Query("SELECT n FROM PrometheusNetworkRealtime n WHERE n.time >= :startTime ORDER BY n.time ASC")
    List<PrometheusNetworkRealtime> findByTimeAfter(@Param("startTime") Instant startTime);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT n FROM PrometheusNetworkRealtime n WHERE n.time BETWEEN :startTime AND :endTime ORDER BY n.time ASC")
    List<PrometheusNetworkRealtime> findByTimeBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * 특정 시간 이전의 오래된 데이터 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM PrometheusNetworkRealtime n WHERE n.time < :cutoffTime")
    int deleteByTimeBefore(@Param("cutoffTime") Instant cutoffTime);
}