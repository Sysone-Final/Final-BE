package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusPromMetricQueryService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * prom_metric에서 CPU 메트릭 조회
     */
    public List<CpuMetricResponse> queryCpuMetrics(String instance, Instant since) {
        try {
            // node_cpu_seconds_total에서 CPU 사용률 계산
            String sql = """
                WITH cpu_data AS (
                    SELECT 
                        time,
                        labels,
                        value,
                        LAG(value) OVER (PARTITION BY series_id ORDER BY time) as prev_value,
                        EXTRACT(EPOCH FROM (time - LAG(time) OVER (PARTITION BY series_id ORDER BY time))) as time_diff
                    FROM prom_metric.node_cpu_seconds_total
                    WHERE time >= ?
                    AND labels @> ARRAY[?]::text[]
                ),
                cpu_rate AS (
                    SELECT 
                        time,
                        labels,
                        CASE 
                            WHEN time_diff > 0 THEN ((value - prev_value) / time_diff) * 100
                            ELSE 0
                        END as rate
                    FROM cpu_data
                    WHERE prev_value IS NOT NULL
                )
                SELECT 
                    time,
                    labels[3] as instance,
                    labels[5] as mode,
                    AVG(rate) as avg_rate
                FROM cpu_rate
                GROUP BY time, labels[3], labels[5]
                ORDER BY time DESC
                LIMIT 100
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, since, instance);

            // mode별로 그룹화하여 CpuMetricResponse 생성
            Map<Instant, Map<String, Double>> timeGrouped = new HashMap<>();

            for (Map<String, Object> row : rows) {
                Instant time = ((java.sql.Timestamp) row.get("time")).toInstant();
                String mode = (String) row.get("mode");
                Double rate = ((Number) row.get("avg_rate")).doubleValue();

                timeGrouped.computeIfAbsent(time, k -> new HashMap<>()).put(mode, rate);
            }

            List<CpuMetricResponse> result = new ArrayList<>();
            for (Map.Entry<Instant, Map<String, Double>> entry : timeGrouped.entrySet()) {
                Map<String, Double> modes = entry.getValue();

                double idle = modes.getOrDefault("idle", 0.0);
                double cpuUsage = 100.0 - idle;

                result.add(CpuMetricResponse.builder()
                        .time(entry.getKey())
                        .instance(instance)
                        .cpuUsagePercent(cpuUsage)
                        .userPercent(modes.getOrDefault("user", 0.0))
                        .systemPercent(modes.getOrDefault("system", 0.0))
                        .iowaitPercent(modes.getOrDefault("iowait", 0.0))
                        .idlePercent(idle)
                        .nicePercent(modes.getOrDefault("nice", 0.0))
                        .irqPercent(modes.getOrDefault("irq", 0.0))
                        .softirqPercent(modes.getOrDefault("softirq", 0.0))
                        .stealPercent(modes.getOrDefault("steal", 0.0))
                        .contextSwitchesPerSec(null)
                        .loadAvg1(null)
                        .loadAvg5(null)
                        .loadAvg15(null)
                        .build());
            }

            return result;

        } catch (Exception e) {
            log.error("prom_metric CPU 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }

    /**
     * prom_metric에서 Memory 메트릭 조회
     */
    public List<MemoryMetricResponse> queryMemoryMetrics(String instance, Instant since) {
        try {
            String sql = """
                WITH memory_data AS (
                    SELECT DISTINCT ON (time)
                        time,
                        labels[2] as instance
                    FROM prom_metric."node_memory_MemTotal_bytes"
                    WHERE time >= ?
                    AND labels @> ARRAY[?]::text[]
                    ORDER BY time DESC
                )
                SELECT 
                    m.time,
                    m.instance,
                    total.value as total_bytes,
                    avail.value as available_bytes,
                    free.value as free_bytes,
                    buffers.value as buffers_bytes,
                    cached.value as cached_bytes,
                    active.value as active_bytes,
                    inactive.value as inactive_bytes,
                    swap_total.value as swap_total_bytes,
                    swap_free.value as swap_free_bytes
                FROM memory_data m
                LEFT JOIN prom_metric."node_memory_MemTotal_bytes" total 
                    ON total.time = m.time AND total.labels[2] = m.instance
                LEFT JOIN prom_metric."node_memory_MemAvailable_bytes" avail 
                    ON avail.time = m.time AND avail.labels[2] = m.instance
                LEFT JOIN prom_metric."node_memory_MemFree_bytes" free 
                    ON free.time = m.time AND free.labels[2] = m.instance
                LEFT JOIN prom_metric."node_memory_Buffers_bytes" buffers 
                    ON buffers.time = m.time AND buffers.labels[2] = m.instance
                LEFT JOIN prom_metric."node_memory_Cached_bytes" cached 
                    ON cached.time = m.time AND cached.labels[2] = m.instance
                LEFT JOIN prom_metric."node_memory_Active_bytes" active 
                    ON active.time = m.time AND active.labels[2] = m.instance
                LEFT JOIN prom_metric."node_memory_Inactive_bytes" inactive 
                    ON inactive.time = m.time AND inactive.labels[2] = m.instance
                LEFT JOIN prom_metric."node_memory_SwapTotal_bytes" swap_total 
                    ON swap_total.time = m.time AND swap_total.labels[2] = m.instance
                LEFT JOIN prom_metric."node_memory_SwapFree_bytes" swap_free 
                    ON swap_free.time = m.time AND swap_free.labels[2] = m.instance
                ORDER BY m.time DESC
                LIMIT 100
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, since, instance);

            return rows.stream().map(row -> {
                long totalBytes = ((Number) row.get("total_bytes")).longValue();
                long availableBytes = row.get("available_bytes") != null ?
                        ((Number) row.get("available_bytes")).longValue() : 0;
                long usedBytes = totalBytes - availableBytes;
                double usagePercent = totalBytes > 0 ? (usedBytes * 100.0 / totalBytes) : 0.0;

                Long swapTotal = row.get("swap_total_bytes") != null ?
                        ((Number) row.get("swap_total_bytes")).longValue() : null;
                Long swapFree = row.get("swap_free_bytes") != null ?
                        ((Number) row.get("swap_free_bytes")).longValue() : null;
                Long swapUsed = (swapTotal != null && swapFree != null) ? (swapTotal - swapFree) : null;
                Double swapUsagePercent = (swapTotal != null && swapTotal > 0 && swapUsed != null) ?
                        (swapUsed * 100.0 / swapTotal) : null;

                return MemoryMetricResponse.builder()
                        .time(((java.sql.Timestamp) row.get("time")).toInstant())
                        .instance((String) row.get("instance"))
                        .totalBytes(totalBytes)
                        .usedBytes(usedBytes)
                        .freeBytes(row.get("free_bytes") != null ?
                                ((Number) row.get("free_bytes")).longValue() : null)
                        .availableBytes(availableBytes)
                        .usagePercent(usagePercent)
                        .buffersBytes(row.get("buffers_bytes") != null ?
                                ((Number) row.get("buffers_bytes")).longValue() : null)
                        .cachedBytes(row.get("cached_bytes") != null ?
                                ((Number) row.get("cached_bytes")).longValue() : null)
                        .activeBytes(row.get("active_bytes") != null ?
                                ((Number) row.get("active_bytes")).longValue() : null)
                        .inactiveBytes(row.get("inactive_bytes") != null ?
                                ((Number) row.get("inactive_bytes")).longValue() : null)
                        .swapTotalBytes(swapTotal)
                        .swapUsedBytes(swapUsed)
                        .swapFreeBytes(swapFree)
                        .swapUsagePercent(swapUsagePercent)
                        .build();
            }).toList();

        } catch (Exception e) {
            log.error("prom_metric Memory 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }

    /**
     * prom_metric에서 Network 메트릭 조회
     */
    public List<NetworkMetricResponse> queryNetworkMetrics(String instance, Instant since) {
        try {
            String sql = """
                WITH network_data AS (
                    SELECT 
                        time,
                        labels[2] as device,
                        labels[3] as instance,
                        value
                    FROM prom_metric.node_network_receive_bytes_total
                    WHERE time >= ?
                    AND labels @> ARRAY[?]::text[]
                    AND labels[2] NOT IN ('lo', 'docker0')
                )
                SELECT 
                    n.time,
                    n.instance,
                    n.device,
                    rx.value as rx_bytes_total,
                    tx.value as tx_bytes_total,
                    rx_packets.value as rx_packets_total,
                    tx_packets.value as tx_packets_total,
                    rx_errors.value as rx_errors_total,
                    tx_errors.value as tx_errors_total,
                    rx_drop.value as rx_dropped_total,
                    tx_drop.value as tx_dropped_total,
                    up.value as interface_up
                FROM network_data n
                LEFT JOIN prom_metric.node_network_transmit_bytes_total tx
                    ON tx.time = n.time AND tx.labels[2] = n.device AND tx.labels[3] = n.instance
                LEFT JOIN prom_metric.node_network_receive_packets_total rx_packets
                    ON rx_packets.time = n.time AND rx_packets.labels[2] = n.device AND rx_packets.labels[3] = n.instance
                LEFT JOIN prom_metric.node_network_transmit_packets_total tx_packets
                    ON tx_packets.time = n.time AND tx_packets.labels[2] = n.device AND tx_packets.labels[3] = n.instance
                LEFT JOIN prom_metric.node_network_receive_errs_total rx_errors
                    ON rx_errors.time = n.time AND rx_errors.labels[2] = n.device AND rx_errors.labels[3] = n.instance
                LEFT JOIN prom_metric.node_network_transmit_errs_total tx_errors
                    ON tx_errors.time = n.time AND tx_errors.labels[2] = n.device AND tx_errors.labels[3] = n.instance
                LEFT JOIN prom_metric.node_network_receive_drop_total rx_drop
                    ON rx_drop.time = n.time AND rx_drop.labels[2] = n.device AND rx_drop.labels[3] = n.instance
                LEFT JOIN prom_metric.node_network_transmit_drop_total tx_drop
                    ON tx_drop.time = n.time AND tx_drop.labels[2] = n.device AND tx_drop.labels[3] = n.instance
                LEFT JOIN prom_metric.node_network_up up
                    ON up.time = n.time AND up.labels[2] = n.device AND up.labels[3] = n.instance
                ORDER BY n.time DESC, n.device
                LIMIT 100
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, since, instance);

            return rows.stream().map(row -> {
                return NetworkMetricResponse.builder()
                        .time(((java.sql.Timestamp) row.get("time")).toInstant())
                        .instance((String) row.get("instance"))
                        .device((String) row.get("device"))
                        .rxUsagePercent(null)
                        .txUsagePercent(null)
                        .totalUsagePercent(null)
                        .rxPacketsTotal(row.get("rx_packets_total") != null ?
                                ((Number) row.get("rx_packets_total")).longValue() : null)
                        .txPacketsTotal(row.get("tx_packets_total") != null ?
                                ((Number) row.get("tx_packets_total")).longValue() : null)
                        .rxBytesTotal(row.get("rx_bytes_total") != null ?
                                ((Number) row.get("rx_bytes_total")).longValue() : null)
                        .txBytesTotal(row.get("tx_bytes_total") != null ?
                                ((Number) row.get("tx_bytes_total")).longValue() : null)
                        .rxBytesPerSec(null)
                        .txBytesPerSec(null)
                        .rxPacketsPerSec(null)
                        .txPacketsPerSec(null)
                        .rxErrorsTotal(row.get("rx_errors_total") != null ?
                                ((Number) row.get("rx_errors_total")).longValue() : null)
                        .txErrorsTotal(row.get("tx_errors_total") != null ?
                                ((Number) row.get("tx_errors_total")).longValue() : null)
                        .rxDroppedTotal(row.get("rx_dropped_total") != null ?
                                ((Number) row.get("rx_dropped_total")).longValue() : null)
                        .txDroppedTotal(row.get("tx_dropped_total") != null ?
                                ((Number) row.get("tx_dropped_total")).longValue() : null)
                        .interfaceUp(row.get("interface_up") != null ?
                                ((Number) row.get("interface_up")).intValue() == 1 : null)
                        .build();
            }).toList();

        } catch (Exception e) {
            log.error("prom_metric Network 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }

    /**
     * prom_metric에서 Disk 메트릭 조회
     */
    public List<DiskMetricResponse> queryDiskMetrics(String instance, Instant since) {
        try {
            String sql = """
                WITH disk_data AS (
                    SELECT 
                        time,
                        labels[2] as device,
                        labels[4] as instance,
                        labels[6] as mountpoint,
                        value as total_bytes
                    FROM prom_metric.node_filesystem_size_bytes
                    WHERE time >= ?
                    AND labels @> ARRAY[?]::text[]
                    AND labels[3] NOT IN ('tmpfs', 'devtmpfs')
                )
                SELECT 
                    d.time,
                    d.instance,
                    d.device,
                    d.mountpoint,
                    d.total_bytes,
                    free.value as free_bytes
                FROM disk_data d
                LEFT JOIN prom_metric.node_filesystem_free_bytes free
                    ON free.time = d.time 
                    AND free.labels[2] = d.device 
                    AND free.labels[4] = d.instance
                    AND free.labels[6] = d.mountpoint
                ORDER BY d.time DESC, d.device
                LIMIT 100
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, since, instance);

            return rows.stream().map(row -> {
                long totalBytes = ((Number) row.get("total_bytes")).longValue();
                long freeBytes = row.get("free_bytes") != null ?
                        ((Number) row.get("free_bytes")).longValue() : 0;
                long usedBytes = totalBytes - freeBytes;
                double usagePercent = totalBytes > 0 ? (usedBytes * 100.0 / totalBytes) : 0.0;

                return DiskMetricResponse.builder()
                        .time(((java.sql.Timestamp) row.get("time")).toInstant())
                        .instance((String) row.get("instance"))
                        .device((String) row.get("device"))
                        .mountpoint((String) row.get("mountpoint"))
                        .totalBytes(totalBytes)
                        .usedBytes(usedBytes)
                        .freeBytes(freeBytes)
                        .usagePercent(usagePercent)
                        .readBytesPerSec(null)
                        .writeBytesPerSec(null)
                        .totalIoBytesPerSec(null)
                        .readIops(null)
                        .writeIops(null)
                        .ioUtilizationPercent(null)
                        .readTimePercent(null)
                        .writeTimePercent(null)
                        .totalInodes(null)
                        .usedInodes(null)
                        .freeInodes(null)
                        .inodeUsagePercent(null)
                        .build();
            }).toList();

        } catch (Exception e) {
            log.error("prom_metric Disk 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }

    /**
     * prom_metric에서 Temperature 메트릭 조회
     */
    public List<TemperatureMetricResponse> queryTemperatureMetrics(String instance, Instant since) {
        try {
            String sql = """
                SELECT 
                    time,
                    labels[2] as chip,
                    labels[3] as instance,
                    labels[4] as sensor,
                    value as temp_celsius
                FROM prom_metric.node_hwmon_temp_celsius
                WHERE time >= ?
                AND labels @> ARRAY[?]::text[]
                ORDER BY time DESC
                LIMIT 100
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, since, instance);

            return rows.stream().map(row -> {
                return TemperatureMetricResponse.builder()
                        .time(((java.sql.Timestamp) row.get("time")).toInstant())
                        .instance((String) row.get("instance"))
                        .chip((String) row.get("chip"))
                        .sensor((String) row.get("sensor"))
                        .tempCelsius(((Number) row.get("temp_celsius")).doubleValue())
                        .build();
            }).toList();

        } catch (Exception e) {
            log.error("prom_metric Temperature 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }
}