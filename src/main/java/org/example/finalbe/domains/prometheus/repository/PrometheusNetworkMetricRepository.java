package org.example.finalbe.domains.prometheus.repository;

import org.example.finalbe.domains.prometheus.domain.PrometheusNetworkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PrometheusNetworkMetricRepository extends JpaRepository<PrometheusNetworkMetric, Long> {

    /**
     * 최근 데이터 조회 (SSE용)
     */
    @Query("SELECT n FROM PrometheusNetworkMetric n WHERE n.time >= :since ORDER BY n.time DESC")
    List<PrometheusNetworkMetric> findRecentMetrics(@Param("since") Instant since);

    /**
     * 인스턴스+디바이스별 최신 메트릭
     */
    @Query(value = """
        SELECT DISTINCT ON (instance, device) *
        FROM prometheus_network_metrics
        WHERE time >= :since
        ORDER BY instance, device, time DESC
        """, nativeQuery = true)
    List<PrometheusNetworkMetric> findLatestByInstanceAndDevice(@Param("since") Instant since);

    /**
     * 시간 범위 조회
     */
    @Query("SELECT n FROM PrometheusNetworkMetric n WHERE n.time BETWEEN :start AND :end ORDER BY n.time DESC")
    List<PrometheusNetworkMetric> findByTimeBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 전체 인스턴스+디바이스 최신 1개
     */
    @Query(value = """
        SELECT *
        FROM prometheus_network_metrics
        WHERE time = (SELECT MAX(time) FROM prometheus_network_metrics)
        ORDER BY instance, device
        """, nativeQuery = true)
    List<PrometheusNetworkMetric> findAllLatest();

    /**
     * ✅ 네트워크 RX 사용률 (게이지 + 시계열)
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        device,
        AVG(rx_usage_percent) AS avg_rx_usage
    FROM prometheus_network_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
      AND device NOT LIKE 'lo%'
      AND device NOT LIKE 'veth%'
    GROUP BY bucket, device
    ORDER BY bucket ASC, device
    """, nativeQuery = true)
    List<Object[]> getNetworkRxUsage(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ 네트워크 TX 사용률
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        device,
        AVG(tx_usage_percent) AS avg_tx_usage
    FROM prometheus_network_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
      AND device NOT LIKE 'lo%'
      AND device NOT LIKE 'veth%'
    GROUP BY bucket, device
    ORDER BY bucket ASC, device
    """, nativeQuery = true)
    List<Object[]> getNetworkTxUsage(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ 네트워크 패킷 수 (누적)
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        device,
        SUM(rx_packets_total) AS total_rx_packets,
        SUM(tx_packets_total) AS total_tx_packets
    FROM prometheus_network_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
      AND device NOT LIKE 'lo%'
      AND device NOT LIKE 'veth%'
    GROUP BY bucket, device
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getNetworkPackets(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ 네트워크 바이트 (트래픽)
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        device,
        SUM(rx_bytes_total) AS total_rx_bytes,
        SUM(tx_bytes_total) AS total_tx_bytes
    FROM prometheus_network_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
      AND device NOT LIKE 'lo%'
      AND device NOT LIKE 'veth%'
    GROUP BY bucket, device
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getNetworkBytes(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ 네트워크 에러 및 드롭
     */
    @Query(value = """
    SELECT 
        time_bucket('1 minute', time) AS bucket,
        device,
        AVG(rx_errors_total) AS rx_errors,
        AVG(tx_errors_total) AS tx_errors,
        AVG(rx_dropped_total) AS rx_dropped,
        AVG(tx_dropped_total) AS tx_dropped
    FROM prometheus_network_metrics
    WHERE instance = :instance
      AND time BETWEEN :start AND :end
      AND device NOT LIKE 'lo%'
      AND device NOT LIKE 'veth%'
    GROUP BY bucket, device
    ORDER BY bucket ASC
    """, nativeQuery = true)
    List<Object[]> getNetworkErrors(
            @Param("instance") String instance,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /**
     * ✅ 인터페이스 상태 (최신값)
     */
    @Query(value = """
    SELECT DISTINCT ON (device)
        device,
        interface_up
    FROM prometheus_network_metrics
    WHERE instance = :instance
      AND device NOT LIKE 'lo%'
      AND device NOT LIKE 'veth%'
    ORDER BY device, time DESC
    """, nativeQuery = true)
    List<Object[]> getInterfaceStatus(@Param("instance") String instance);
}