package org.example.finalbe.domains.prometheus.repository.network;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PrometheusNetworkMetricRepository {

    private final EntityManager entityManager;

    /**
     * 네트워크 RX/TX 사용률 추이
     */
    public List<Object[]> getNetworkUsageTrend(Instant startTime, Instant endTime) {
        String query = """
            WITH rx_data AS (
                SELECT 
                    time,
                    device_id,
                    value,
                    LAG(value) OVER (PARTITION BY device_id ORDER BY time) as prev_value
                FROM prom_metric.node_network_receive_bytes_total
                WHERE time BETWEEN :startTime AND :endTime
            ),
            tx_data AS (
                SELECT 
                    time,
                    device_id,
                    value,
                    LAG(value) OVER (PARTITION BY device_id ORDER BY time) as prev_value
                FROM prom_metric.node_network_transmit_bytes_total
                WHERE time BETWEEN :startTime AND :endTime
            )
            SELECT 
                rx.time,
                SUM(CASE WHEN rx.prev_value IS NOT NULL 
                    THEN (rx.value - rx.prev_value) ELSE 0 END)::double precision as rx_bytes_per_sec,
                SUM(CASE WHEN tx.prev_value IS NOT NULL 
                    THEN (tx.value - tx.prev_value) ELSE 0 END)::double precision as tx_bytes_per_sec
            FROM rx_data rx
            JOIN tx_data tx ON rx.time = tx.time AND rx.device_id = tx.device_id
            GROUP BY rx.time
            ORDER BY rx.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 네트워크 패킷 수 추이 (RX/TX) - 그래프 3.3, 3.4
     */
    public List<Object[]> getNetworkPacketsTrend(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                rx.time,
                SUM(rx.value)::double precision as total_rx_packets,
                SUM(tx.value)::double precision as total_tx_packets
            FROM prom_metric.node_network_receive_packets_total rx
            JOIN prom_metric.node_network_transmit_packets_total tx 
                ON rx.time = tx.time AND rx.device_id = tx.device_id
            WHERE rx.time BETWEEN :startTime AND :endTime
            GROUP BY rx.time
            ORDER BY rx.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 네트워크 바이트 누적 추이 - 그래프 3.5, 3.6
     */
    public List<Object[]> getNetworkBytesTrend(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                rx.time,
                SUM(rx.value)::double precision as total_rx_bytes,
                SUM(tx.value)::double precision as total_tx_bytes
            FROM prom_metric.node_network_receive_bytes_total rx
            JOIN prom_metric.node_network_transmit_bytes_total tx 
                ON rx.time = tx.time AND rx.device_id = tx.device_id
            WHERE rx.time BETWEEN :startTime AND :endTime
            GROUP BY rx.time
            ORDER BY rx.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 네트워크 에러 및 드롭 패킷 - 그래프 3.8
     */
    public List<Object[]> getNetworkErrorsAndDrops(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                re.time,
                SUM(re.value)::double precision as rx_errors,
                SUM(te.value)::double precision as tx_errors,
                SUM(rd.value)::double precision as rx_drops,
                SUM(td.value)::double precision as tx_drops
            FROM prom_metric.node_network_receive_errs_total re
            JOIN prom_metric.node_network_transmit_errs_total te 
                ON re.time = te.time AND re.device_id = te.device_id
            JOIN prom_metric.node_network_receive_drop_total rd 
                ON re.time = rd.time AND re.device_id = rd.device_id
            JOIN prom_metric.node_network_transmit_drop_total td 
                ON re.time = td.time AND re.device_id = td.device_id
            WHERE re.time BETWEEN :startTime AND :endTime
            GROUP BY re.time
            ORDER BY re.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 네트워크 인터페이스 상태 - 그래프 3.9
     */
    public List<Object[]> getNetworkInterfaceStatus(Instant time) {
        String query = """
            SELECT\s
                d.value as device_name,
                nu.value::integer as oper_status
            FROM prom_metric.node_network_up nu
            LEFT JOIN prom_metric.node_network_device_id d
                ON nu.device_id = d.device_id AND nu.time = d.time
            WHERE nu.time = :time
            ORDER BY d.value
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("time", time)
                .getResultList();
    }

    /**
     * 현재 네트워크 사용률
     */
    public Object[] getCurrentNetworkUsage() {
        String query = """
            WITH latest_rx AS (
                SELECT 
                    value,
                    LAG(value) OVER (ORDER BY time) as prev_value
                FROM prom_metric.node_network_receive_bytes_total
                WHERE time >= NOW() - INTERVAL '1 minute'
                ORDER BY time DESC
                LIMIT 2
            ),
            latest_tx AS (
                SELECT 
                    value,
                    LAG(value) OVER (ORDER BY time) as prev_value
                FROM prom_metric.node_network_transmit_bytes_total
                WHERE time >= NOW() - INTERVAL '1 minute'
                ORDER BY time DESC
                LIMIT 2
            )
            SELECT 
                SUM(CASE WHEN latest_rx.prev_value IS NOT NULL 
                    THEN (latest_rx.value - latest_rx.prev_value) ELSE 0 END)::double precision as current_rx_bps,
                SUM(CASE WHEN latest_tx.prev_value IS NOT NULL 
                    THEN (latest_tx.value - latest_tx.prev_value) ELSE 0 END)::double precision as current_tx_bps
            FROM latest_rx, latest_tx
            """;

        List<Object[]> results = entityManager.createNativeQuery(query).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}