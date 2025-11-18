package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.prometheus.dto.*;
import org.example.finalbe.domains.prometheus.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusMetricQueryService {

    private final PrometheusCpuMetricRepository cpuMetricRepository;
    private final PrometheusMemoryMetricRepository memoryMetricRepository;
    private final PrometheusNetworkMetricRepository networkMetricRepository;
    private final PrometheusDiskMetricRepository diskMetricRepository;
    private final PrometheusTemperatureMetricRepository temperatureMetricRepository;
    private final EquipmentRepository equipmentRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final int SEVEN_DAYS_IN_SECONDS = 7 * 24 * 60 * 60;

    /**
     * 최근 메트릭 조회 (SSE 브로드캐스트용)
     */
    public MetricsResponse getRecentMetrics(Instant since) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Instant sevenDaysAgoInstant = sevenDaysAgo.atZone(ZoneId.systemDefault()).toInstant();

        if (since.isAfter(sevenDaysAgoInstant)) {
            return getMetricsFromPublic(since);
        } else {
            return getMetricsFromPromMetric(since);
        }
    }

    /**
     * 단일 장비의 최신 메트릭 조회
     */
    public EquipmentMetricsResponse getLatestMetricsByEquipment(Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장비입니다: " + equipmentId));

        String instance = equipment.getCode();
        Instant since = Instant.now().minusSeconds(60);

        return EquipmentMetricsResponse.builder()
                .equipmentId(equipmentId)
                .instance(instance)
                .cpu(getCpuMetricsByInstance(instance, since))
                .memory(getMemoryMetricsByInstance(instance, since))
                .network(getNetworkMetricsByInstance(instance, since))
                .disk(getDiskMetricsByInstance(instance, since))
                .temperature(getTemperatureMetricsByInstance(instance, since))
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 여러 장비의 최신 메트릭 조회
     */
    public List<EquipmentMetricsResponse> getLatestMetricsByEquipments(Set<Long> equipmentIds) {
        return equipmentIds.stream()
                .map(this::getLatestMetricsByEquipment)
                .collect(Collectors.toList());
    }

    /**
     * 랙별 집계 메트릭 조회 (평균)
     */
    public AggregatedMetricsResponse getAggregatedMetricsByRack(Long rackId, Set<Long> equipmentIds) {
        List<EquipmentMetricsResponse> equipmentMetrics = getLatestMetricsByEquipments(equipmentIds);
        return aggregateMetrics(equipmentMetrics, "rack", rackId);
    }

    /**
     * 서버실별 집계 메트릭 조회 (평균)
     */
    public AggregatedMetricsResponse getAggregatedMetricsByServerRoom(Long serverRoomId, Set<Long> equipmentIds) {
        List<EquipmentMetricsResponse> equipmentMetrics = getLatestMetricsByEquipments(equipmentIds);
        return aggregateMetrics(equipmentMetrics, "serverRoom", serverRoomId);
    }

    /**
     * 데이터센터별 집계 메트릭 조회 (평균)
     */
    public AggregatedMetricsResponse getAggregatedMetricsByDataCenter(Long dataCenterId, Set<Long> equipmentIds) {
        List<EquipmentMetricsResponse> equipmentMetrics = getLatestMetricsByEquipments(equipmentIds);
        return aggregateMetrics(equipmentMetrics, "dataCenter", dataCenterId);
    }

    /**
     * public 스키마에서 메트릭 조회 (7일 이내)
     */
    private MetricsResponse getMetricsFromPublic(Instant since) {
        List<CpuMetricResponse> cpuMetrics = cpuMetricRepository.findRecentMetrics(since).stream()
                .map(CpuMetricResponse::from)
                .collect(Collectors.toList());

        List<MemoryMetricResponse> memoryMetrics = memoryMetricRepository.findRecentMetrics(since).stream()
                .map(MemoryMetricResponse::from)
                .collect(Collectors.toList());

        List<NetworkMetricResponse> networkMetrics = networkMetricRepository.findRecentMetrics(since).stream()
                .map(NetworkMetricResponse::from)
                .collect(Collectors.toList());

        List<DiskMetricResponse> diskMetrics = diskMetricRepository.findRecentMetrics(since).stream()
                .map(DiskMetricResponse::from)
                .collect(Collectors.toList());

        List<TemperatureMetricResponse> temperatureMetrics = temperatureMetricRepository.findRecentMetrics(since).stream()
                .map(TemperatureMetricResponse::from)
                .collect(Collectors.toList());

        int totalRecords = cpuMetrics.size() + memoryMetrics.size() +
                networkMetrics.size() + diskMetrics.size() + temperatureMetrics.size();

        return MetricsResponse.builder()
                .cpu(cpuMetrics)
                .memory(memoryMetrics)
                .network(networkMetrics)
                .disk(diskMetrics)
                .temperature(temperatureMetrics)
                .totalRecords(totalRecords)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * prom_metric 스키마에서 메트릭 조회 (7일 이후)
     */
    private MetricsResponse getMetricsFromPromMetric(Instant since) {
        List<CpuMetricResponse> cpuMetrics = queryCpuFromPromMetric(since);
        List<MemoryMetricResponse> memoryMetrics = queryMemoryFromPromMetric(since);
        List<NetworkMetricResponse> networkMetrics = queryNetworkFromPromMetric(since);
        List<DiskMetricResponse> diskMetrics = queryDiskFromPromMetric(since);
        List<TemperatureMetricResponse> temperatureMetrics = queryTemperatureFromPromMetric(since);

        int totalRecords = cpuMetrics.size() + memoryMetrics.size() +
                networkMetrics.size() + diskMetrics.size() + temperatureMetrics.size();

        return MetricsResponse.builder()
                .cpu(cpuMetrics)
                .memory(memoryMetrics)
                .network(networkMetrics)
                .disk(diskMetrics)
                .temperature(temperatureMetrics)
                .totalRecords(totalRecords)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * instance별 CPU 메트릭 조회
     */
    private List<CpuMetricResponse> getCpuMetricsByInstance(String instance, Instant since) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Instant sevenDaysAgoInstant = sevenDaysAgo.atZone(ZoneId.systemDefault()).toInstant();

        if (since.isAfter(sevenDaysAgoInstant)) {
            return cpuMetricRepository.findRecentMetrics(since).stream()
                    .filter(m -> m.getInstance().equals(instance))
                    .map(CpuMetricResponse::from)
                    .collect(Collectors.toList());
        } else {
            return queryCpuFromPromMetricByInstance(instance, since);
        }
    }

    /**
     * instance별 Memory 메트릭 조회
     */
    private List<MemoryMetricResponse> getMemoryMetricsByInstance(String instance, Instant since) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Instant sevenDaysAgoInstant = sevenDaysAgo.atZone(ZoneId.systemDefault()).toInstant();

        if (since.isAfter(sevenDaysAgoInstant)) {
            return memoryMetricRepository.findRecentMetrics(since).stream()
                    .filter(m -> m.getInstance().equals(instance))
                    .map(MemoryMetricResponse::from)
                    .collect(Collectors.toList());
        } else {
            return queryMemoryFromPromMetricByInstance(instance, since);
        }
    }

    /**
     * instance별 Network 메트릭 조회
     */
    private List<NetworkMetricResponse> getNetworkMetricsByInstance(String instance, Instant since) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Instant sevenDaysAgoInstant = sevenDaysAgo.atZone(ZoneId.systemDefault()).toInstant();

        if (since.isAfter(sevenDaysAgoInstant)) {
            return networkMetricRepository.findRecentMetrics(since).stream()
                    .filter(m -> m.getInstance().equals(instance))
                    .map(NetworkMetricResponse::from)
                    .collect(Collectors.toList());
        } else {
            return queryNetworkFromPromMetricByInstance(instance, since);
        }
    }

    /**
     * instance별 Disk 메트릭 조회
     */
    private List<DiskMetricResponse> getDiskMetricsByInstance(String instance, Instant since) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Instant sevenDaysAgoInstant = sevenDaysAgo.atZone(ZoneId.systemDefault()).toInstant();

        if (since.isAfter(sevenDaysAgoInstant)) {
            return diskMetricRepository.findRecentMetrics(since).stream()
                    .filter(m -> m.getInstance().equals(instance))
                    .map(DiskMetricResponse::from)
                    .collect(Collectors.toList());
        } else {
            return queryDiskFromPromMetricByInstance(instance, since);
        }
    }

    /**
     * instance별 Temperature 메트릭 조회
     */
    private List<TemperatureMetricResponse> getTemperatureMetricsByInstance(String instance, Instant since) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Instant sevenDaysAgoInstant = sevenDaysAgo.atZone(ZoneId.systemDefault()).toInstant();

        if (since.isAfter(sevenDaysAgoInstant)) {
            return temperatureMetricRepository.findRecentMetrics(since).stream()
                    .filter(m -> m.getInstance().equals(instance))
                    .map(TemperatureMetricResponse::from)
                    .collect(Collectors.toList());
        } else {
            return queryTemperatureFromPromMetricByInstance(instance, since);
        }
    }

    /**
     * 메트릭 집계 (평균 계산)
     */
    private AggregatedMetricsResponse aggregateMetrics(
            List<EquipmentMetricsResponse> equipmentMetrics,
            String aggregationType,
            Long aggregationId) {

        if (equipmentMetrics.isEmpty()) {
            return AggregatedMetricsResponse.empty(aggregationType, aggregationId);
        }

        Double avgCpuUsage = equipmentMetrics.stream()
                .flatMap(em -> em.cpu().stream())
                .mapToDouble(CpuMetricResponse::cpuUsagePercent)
                .average()
                .orElse(0.0);

        Double avgMemoryUsage = equipmentMetrics.stream()
                .flatMap(em -> em.memory().stream())
                .mapToDouble(MemoryMetricResponse::usagePercent)
                .average()
                .orElse(0.0);

        Double avgNetworkUsage = equipmentMetrics.stream()
                .flatMap(em -> em.network().stream())
                .filter(n -> n.totalUsagePercent() != null)
                .mapToDouble(NetworkMetricResponse::totalUsagePercent)
                .average()
                .orElse(0.0);

        Double avgDiskUsage = equipmentMetrics.stream()
                .flatMap(em -> em.disk().stream())
                .filter(d -> d.usagePercent() != null)
                .mapToDouble(DiskMetricResponse::usagePercent)
                .average()
                .orElse(0.0);

        Double avgTemperature = equipmentMetrics.stream()
                .flatMap(em -> em.temperature().stream())
                .mapToDouble(TemperatureMetricResponse::tempCelsius)
                .average()
                .orElse(0.0);

        return AggregatedMetricsResponse.builder()
                .aggregationType(aggregationType)
                .aggregationId(aggregationId)
                .equipmentCount(equipmentMetrics.size())
                .avgCpuUsagePercent(avgCpuUsage)
                .avgMemoryUsagePercent(avgMemoryUsage)
                .avgNetworkUsagePercent(avgNetworkUsage)
                .avgDiskUsagePercent(avgDiskUsage)
                .avgTemperatureCelsius(avgTemperature)
                .timestamp(Instant.now())
                .build();
    }

    // ========== prom_metric 스키마 조회 메서드들 (JDBC Template) ==========

    /**
     * prom_metric에서 CPU 메트릭 조회
     */
    private List<CpuMetricResponse> queryCpuFromPromMetric(Instant since) {
        try {
            String sql = """
                WITH cpu_modes AS (
                    SELECT 
                        time,
                        labels[3] as instance,
                        labels[5] as mode,
                        value
                    FROM prom_metric.node_cpu_seconds_total
                    WHERE time >= ?
                    ORDER BY time DESC
                    LIMIT 1000
                )
                SELECT 
                    time,
                    instance,
                    MAX(CASE WHEN mode = 'user' THEN value END) as user_val,
                    MAX(CASE WHEN mode = 'system' THEN value END) as system_val,
                    MAX(CASE WHEN mode = 'iowait' THEN value END) as iowait_val,
                    MAX(CASE WHEN mode = 'idle' THEN value END) as idle_val,
                    MAX(CASE WHEN mode = 'nice' THEN value END) as nice_val,
                    MAX(CASE WHEN mode = 'irq' THEN value END) as irq_val,
                    MAX(CASE WHEN mode = 'softirq' THEN value END) as softirq_val,
                    MAX(CASE WHEN mode = 'steal' THEN value END) as steal_val
                FROM cpu_modes
                GROUP BY time, instance
                ORDER BY time DESC
                """;

            return jdbcTemplate.query(sql, new Object[]{java.sql.Timestamp.from(since)}, (rs, rowNum) -> {
                double idle = rs.getDouble("idle_val");
                double cpuUsage = 100.0 - (idle > 0 ? idle : 0.0);

                return CpuMetricResponse.builder()
                        .time(rs.getTimestamp("time").toInstant())
                        .instance(rs.getString("instance"))
                        .cpuUsagePercent(cpuUsage)
                        .userPercent(rs.getDouble("user_val"))
                        .systemPercent(rs.getDouble("system_val"))
                        .iowaitPercent(rs.getDouble("iowait_val"))
                        .idlePercent(idle)
                        .nicePercent(rs.getDouble("nice_val"))
                        .irqPercent(rs.getDouble("irq_val"))
                        .softirqPercent(rs.getDouble("softirq_val"))
                        .stealPercent(rs.getDouble("steal_val"))
                        .contextSwitchesPerSec(null)
                        .loadAvg1(null)
                        .loadAvg5(null)
                        .loadAvg15(null)
                        .build();
            });
        } catch (Exception e) {
            log.error("prom_metric CPU 조회 실패", e);
            return Collections.emptyList();
        }
    }

    private List<CpuMetricResponse> queryCpuFromPromMetricByInstance(String instance, Instant since) {
        try {
            String sql = """
                WITH cpu_modes AS (
                    SELECT 
                        time,
                        labels[3] as instance,
                        labels[5] as mode,
                        value
                    FROM prom_metric.node_cpu_seconds_total
                    WHERE time >= ? AND labels[3] = ?
                    ORDER BY time DESC
                    LIMIT 100
                )
                SELECT 
                    time,
                    instance,
                    MAX(CASE WHEN mode = 'user' THEN value END) as user_val,
                    MAX(CASE WHEN mode = 'system' THEN value END) as system_val,
                    MAX(CASE WHEN mode = 'iowait' THEN value END) as iowait_val,
                    MAX(CASE WHEN mode = 'idle' THEN value END) as idle_val,
                    MAX(CASE WHEN mode = 'nice' THEN value END) as nice_val,
                    MAX(CASE WHEN mode = 'irq' THEN value END) as irq_val,
                    MAX(CASE WHEN mode = 'softirq' THEN value END) as softirq_val,
                    MAX(CASE WHEN mode = 'steal' THEN value END) as steal_val
                FROM cpu_modes
                GROUP BY time, instance
                ORDER BY time DESC
                """;

            return jdbcTemplate.query(sql, new Object[]{java.sql.Timestamp.from(since), instance}, (rs, rowNum) -> {
                double idle = rs.getDouble("idle_val");
                double cpuUsage = 100.0 - (idle > 0 ? idle : 0.0);

                return CpuMetricResponse.builder()
                        .time(rs.getTimestamp("time").toInstant())
                        .instance(rs.getString("instance"))
                        .cpuUsagePercent(cpuUsage)
                        .userPercent(rs.getDouble("user_val"))
                        .systemPercent(rs.getDouble("system_val"))
                        .iowaitPercent(rs.getDouble("iowait_val"))
                        .idlePercent(idle)
                        .nicePercent(rs.getDouble("nice_val"))
                        .irqPercent(rs.getDouble("irq_val"))
                        .softirqPercent(rs.getDouble("softirq_val"))
                        .stealPercent(rs.getDouble("steal_val"))
                        .contextSwitchesPerSec(null)
                        .loadAvg1(null)
                        .loadAvg5(null)
                        .loadAvg15(null)
                        .build();
            });
        } catch (Exception e) {
            log.error("prom_metric CPU 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }

    /**
     * prom_metric에서 Memory 메트릭 조회
     */
    private List<MemoryMetricResponse> queryMemoryFromPromMetric(Instant since) {
        try {
            String sql = """
                SELECT DISTINCT ON (mt.time, mt.labels[2])
                    mt.time,
                    mt.labels[2] as instance,
                    mt.value as total_bytes,
                    ma.value as available_bytes,
                    mf.value as free_bytes,
                    mb.value as buffers_bytes,
                    mc.value as cached_bytes,
                    mactive.value as active_bytes,
                    minactive.value as inactive_bytes,
                    st.value as swap_total_bytes,
                    sf.value as swap_free_bytes
                FROM prom_metric."node_memory_MemTotal_bytes" mt
                LEFT JOIN prom_metric."node_memory_MemAvailable_bytes" ma 
                    ON ma.time = mt.time AND ma.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_MemFree_bytes" mf 
                    ON mf.time = mt.time AND mf.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_Buffers_bytes" mb 
                    ON mb.time = mt.time AND mb.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_Cached_bytes" mc 
                    ON mc.time = mt.time AND mc.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_Active_bytes" mactive 
                    ON mactive.time = mt.time AND mactive.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_Inactive_bytes" minactive 
                    ON minactive.time = mt.time AND minactive.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_SwapTotal_bytes" st 
                    ON st.time = mt.time AND st.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_SwapFree_bytes" sf 
                    ON sf.time = mt.time AND sf.labels[2] = mt.labels[2]
                WHERE mt.time >= ?
                ORDER BY mt.time DESC, mt.labels[2]
                LIMIT 1000
                """;

            return jdbcTemplate.query(sql, new Object[]{java.sql.Timestamp.from(since)}, (rs, rowNum) -> {
                long totalBytes = rs.getLong("total_bytes");
                long availableBytes = rs.getLong("available_bytes");
                long usedBytes = totalBytes - availableBytes;
                double usagePercent = totalBytes > 0 ? (usedBytes * 100.0 / totalBytes) : 0.0;

                Long swapTotal = rs.getObject("swap_total_bytes") != null ? rs.getLong("swap_total_bytes") : null;
                Long swapFree = rs.getObject("swap_free_bytes") != null ? rs.getLong("swap_free_bytes") : null;
                Long swapUsed = (swapTotal != null && swapFree != null) ? (swapTotal - swapFree) : null;
                Double swapUsagePercent = (swapTotal != null && swapTotal > 0 && swapUsed != null) ?
                        (swapUsed * 100.0 / swapTotal) : null;

                return MemoryMetricResponse.builder()
                        .time(rs.getTimestamp("time").toInstant())
                        .instance(rs.getString("instance"))
                        .totalBytes(totalBytes)
                        .usedBytes(usedBytes)
                        .freeBytes(rs.getLong("free_bytes"))
                        .availableBytes(availableBytes)
                        .usagePercent(usagePercent)
                        .buffersBytes(rs.getObject("buffers_bytes") != null ? rs.getLong("buffers_bytes") : null)
                        .cachedBytes(rs.getObject("cached_bytes") != null ? rs.getLong("cached_bytes") : null)
                        .activeBytes(rs.getObject("active_bytes") != null ? rs.getLong("active_bytes") : null)
                        .inactiveBytes(rs.getObject("inactive_bytes") != null ? rs.getLong("inactive_bytes") : null)
                        .swapTotalBytes(swapTotal)
                        .swapUsedBytes(swapUsed)
                        .swapFreeBytes(swapFree)
                        .swapUsagePercent(swapUsagePercent)
                        .build();
            });
        } catch (Exception e) {
            log.error("prom_metric Memory 조회 실패", e);
            return Collections.emptyList();
        }
    }

    private List<MemoryMetricResponse> queryMemoryFromPromMetricByInstance(String instance, Instant since) {
        try {
            String sql = """
                SELECT DISTINCT ON (mt.time)
                    mt.time,
                    mt.labels[2] as instance,
                    mt.value as total_bytes,
                    ma.value as available_bytes,
                    mf.value as free_bytes,
                    mb.value as buffers_bytes,
                    mc.value as cached_bytes,
                    mactive.value as active_bytes,
                    minactive.value as inactive_bytes,
                    st.value as swap_total_bytes,
                    sf.value as swap_free_bytes
                FROM prom_metric."node_memory_MemTotal_bytes" mt
                LEFT JOIN prom_metric."node_memory_MemAvailable_bytes" ma 
                    ON ma.time = mt.time AND ma.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_MemFree_bytes" mf 
                    ON mf.time = mt.time AND mf.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_Buffers_bytes" mb 
                    ON mb.time = mt.time AND mb.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_Cached_bytes" mc 
                    ON mc.time = mt.time AND mc.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_Active_bytes" mactive 
                    ON mactive.time = mt.time AND mactive.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_Inactive_bytes" minactive 
                    ON minactive.time = mt.time AND minactive.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_SwapTotal_bytes" st 
                    ON st.time = mt.time AND st.labels[2] = mt.labels[2]
                LEFT JOIN prom_metric."node_memory_SwapFree_bytes" sf 
                    ON sf.time = mt.time AND sf.labels[2] = mt.labels[2]
                WHERE mt.time >= ? AND mt.labels[2] = ?
                ORDER BY mt.time DESC
                LIMIT 100
                """;

            return jdbcTemplate.query(sql, new Object[]{java.sql.Timestamp.from(since), instance}, (rs, rowNum) -> {
                long totalBytes = rs.getLong("total_bytes");
                long availableBytes = rs.getLong("available_bytes");
                long usedBytes = totalBytes - availableBytes;
                double usagePercent = totalBytes > 0 ? (usedBytes * 100.0 / totalBytes) : 0.0;

                Long swapTotal = rs.getObject("swap_total_bytes") != null ? rs.getLong("swap_total_bytes") : null;
                Long swapFree = rs.getObject("swap_free_bytes") != null ? rs.getLong("swap_free_bytes") : null;
                Long swapUsed = (swapTotal != null && swapFree != null) ? (swapTotal - swapFree) : null;
                Double swapUsagePercent = (swapTotal != null && swapTotal > 0 && swapUsed != null) ?
                        (swapUsed * 100.0 / swapTotal) : null;

                return MemoryMetricResponse.builder()
                        .time(rs.getTimestamp("time").toInstant())
                        .instance(rs.getString("instance"))
                        .totalBytes(totalBytes)
                        .usedBytes(usedBytes)
                        .freeBytes(rs.getLong("free_bytes"))
                        .availableBytes(availableBytes)
                        .usagePercent(usagePercent)
                        .buffersBytes(rs.getObject("buffers_bytes") != null ? rs.getLong("buffers_bytes") : null)
                        .cachedBytes(rs.getObject("cached_bytes") != null ? rs.getLong("cached_bytes") : null)
                        .activeBytes(rs.getObject("active_bytes") != null ? rs.getLong("active_bytes") : null)
                        .inactiveBytes(rs.getObject("inactive_bytes") != null ? rs.getLong("inactive_bytes") : null)
                        .swapTotalBytes(swapTotal)
                        .swapUsedBytes(swapUsed)
                        .swapFreeBytes(swapFree)
                        .swapUsagePercent(swapUsagePercent)
                        .build();
            });
        } catch (Exception e) {
            log.error("prom_metric Memory 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }

    /**
     * prom_metric에서 Network 메트릭 조회
     */
    private List<NetworkMetricResponse> queryNetworkFromPromMetric(Instant since) {
        try {
            String sql = """
                SELECT DISTINCT ON (rx.time, rx.labels[2], rx.labels[3])
                    rx.time,
                    rx.labels[2] as device,
                    rx.labels[3] as instance,
                    rx.value as rx_bytes_total,
                    tx.value as tx_bytes_total,
                    rxp.value as rx_packets_total,
                    txp.value as tx_packets_total,
                    rxe.value as rx_errors_total,
                    txe.value as tx_errors_total,
                    rxd.value as rx_dropped_total,
                    txd.value as tx_dropped_total,
                    up.value as interface_up
                FROM prom_metric.node_network_receive_bytes_total rx
                LEFT JOIN prom_metric.node_network_transmit_bytes_total tx
                    ON tx.time = rx.time AND tx.labels[2] = rx.labels[2] AND tx.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_receive_packets_total rxp
                    ON rxp.time = rx.time AND rxp.labels[2] = rx.labels[2] AND rxp.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_transmit_packets_total txp
                    ON txp.time = rx.time AND txp.labels[2] = rx.labels[2] AND txp.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_receive_errs_total rxe
                    ON rxe.time = rx.time AND rxe.labels[2] = rx.labels[2] AND rxe.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_transmit_errs_total txe
                    ON txe.time = rx.time AND txe.labels[2] = rx.labels[2] AND txe.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_receive_drop_total rxd
                    ON rxd.time = rx.time AND rxd.labels[2] = rx.labels[2] AND rxd.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_transmit_drop_total txd
                    ON txd.time = rx.time AND txd.labels[2] = rx.labels[2] AND txd.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_up up
                    ON up.time = rx.time AND up.labels[2] = rx.labels[2] AND up.labels[3] = rx.labels[3]
                WHERE rx.time >= ?
                AND rx.labels[2] NOT IN ('lo', 'docker0')
                ORDER BY rx.time DESC, rx.labels[2], rx.labels[3]
                LIMIT 1000
                """;

            return jdbcTemplate.query(sql, new Object[]{java.sql.Timestamp.from(since)}, (rs, rowNum) -> {
                return NetworkMetricResponse.builder()
                        .time(rs.getTimestamp("time").toInstant())
                        .instance(rs.getString("instance"))
                        .device(rs.getString("device"))
                        .rxUsagePercent(null)
                        .txUsagePercent(null)
                        .totalUsagePercent(null)
                        .rxPacketsTotal(rs.getObject("rx_packets_total") != null ? rs.getLong("rx_packets_total") : null)
                        .txPacketsTotal(rs.getObject("tx_packets_total") != null ? rs.getLong("tx_packets_total") : null)
                        .rxBytesTotal(rs.getObject("rx_bytes_total") != null ? rs.getLong("rx_bytes_total") : null)
                        .txBytesTotal(rs.getObject("tx_bytes_total") != null ? rs.getLong("tx_bytes_total") : null)
                        .rxBytesPerSec(null)
                        .txBytesPerSec(null)
                        .rxPacketsPerSec(null)
                        .txPacketsPerSec(null)
                        .rxErrorsTotal(rs.getObject("rx_errors_total") != null ? rs.getLong("rx_errors_total") : null)
                        .txErrorsTotal(rs.getObject("tx_errors_total") != null ? rs.getLong("tx_errors_total") : null)
                        .rxDroppedTotal(rs.getObject("rx_dropped_total") != null ? rs.getLong("rx_dropped_total") : null)
                        .txDroppedTotal(rs.getObject("tx_dropped_total") != null ? rs.getLong("tx_dropped_total") : null)
                        .interfaceUp(rs.getObject("interface_up") != null && rs.getDouble("interface_up") == 1.0)
                        .build();
            });
        } catch (Exception e) {
            log.error("prom_metric Network 조회 실패", e);
            return Collections.emptyList();
        }
    }

    private List<NetworkMetricResponse> queryNetworkFromPromMetricByInstance(String instance, Instant since) {
        try {
            String sql = """
                SELECT DISTINCT ON (rx.time, rx.labels[2])
                    rx.time,
                    rx.labels[2] as device,
                    rx.labels[3] as instance,
                    rx.value as rx_bytes_total,
                    tx.value as tx_bytes_total,
                    rxp.value as rx_packets_total,
                    txp.value as tx_packets_total,
                    rxe.value as rx_errors_total,
                    txe.value as tx_errors_total,
                    rxd.value as rx_dropped_total,
                    txd.value as tx_dropped_total,
                    up.value as interface_up
                FROM prom_metric.node_network_receive_bytes_total rx
                LEFT JOIN prom_metric.node_network_transmit_bytes_total tx
                    ON tx.time = rx.time AND tx.labels[2] = rx.labels[2] AND tx.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_receive_packets_total rxp
                    ON rxp.time = rx.time AND rxp.labels[2] = rx.labels[2] AND rxp.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_transmit_packets_total txp
                    ON txp.time = rx.time AND txp.labels[2] = rx.labels[2] AND txp.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_receive_errs_total rxe
                    ON rxe.time = rx.time AND rxe.labels[2] = rx.labels[2] AND rxe.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_transmit_errs_total txe
                    ON txe.time = rx.time AND txe.labels[2] = rx.labels[2] AND txe.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_receive_drop_total rxd
                    ON rxd.time = rx.time AND rxd.labels[2] = rx.labels[2] AND rxd.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_transmit_drop_total txd
                    ON txd.time = rx.time AND txd.labels[2] = rx.labels[2] AND txd.labels[3] = rx.labels[3]
                LEFT JOIN prom_metric.node_network_up up
                    ON up.time = rx.time AND up.labels[2] = rx.labels[2] AND up.labels[3] = rx.labels[3]
                WHERE rx.time >= ? AND rx.labels[3] = ?
                AND rx.labels[2] NOT IN ('lo', 'docker0')
                ORDER BY rx.time DESC, rx.labels[2]
                LIMIT 100
                """;

            return jdbcTemplate.query(sql, new Object[]{java.sql.Timestamp.from(since), instance}, (rs, rowNum) -> {
                return NetworkMetricResponse.builder()
                        .time(rs.getTimestamp("time").toInstant())
                        .instance(rs.getString("instance"))
                        .device(rs.getString("device"))
                        .rxUsagePercent(null)
                        .txUsagePercent(null)
                        .totalUsagePercent(null)
                        .rxPacketsTotal(rs.getObject("rx_packets_total") != null ? rs.getLong("rx_packets_total") : null)
                        .txPacketsTotal(rs.getObject("tx_packets_total") != null ? rs.getLong("tx_packets_total") : null)
                        .rxBytesTotal(rs.getObject("rx_bytes_total") != null ? rs.getLong("rx_bytes_total") : null)
                        .txBytesTotal(rs.getObject("tx_bytes_total") != null ? rs.getLong("tx_bytes_total") : null)
                        .rxBytesPerSec(null)
                        .txBytesPerSec(null)
                        .rxPacketsPerSec(null)
                        .txPacketsPerSec(null)
                        .rxErrorsTotal(rs.getObject("rx_errors_total") != null ? rs.getLong("rx_errors_total") : null)
                        .txErrorsTotal(rs.getObject("tx_errors_total") != null ? rs.getLong("tx_errors_total") : null)
                        .rxDroppedTotal(rs.getObject("rx_dropped_total") != null ? rs.getLong("rx_dropped_total") : null)
                        .txDroppedTotal(rs.getObject("tx_dropped_total") != null ? rs.getLong("tx_dropped_total") : null)
                        .interfaceUp(rs.getObject("interface_up") != null && rs.getDouble("interface_up") == 1.0)
                        .build();
            });
        } catch (Exception e) {
            log.error("prom_metric Network 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }

    /**
     * prom_metric에서 Disk 메트릭 조회
     */
    private List<DiskMetricResponse> queryDiskFromPromMetric(Instant since) {
        try {
            String sql = """
                SELECT DISTINCT ON (size.time, size.labels[2], size.labels[4], size.labels[6])
                    size.time,
                    size.labels[2] as device,
                    size.labels[4] as instance,
                    size.labels[6] as mountpoint,
                    size.value as total_bytes,
                    free.value as free_bytes
                FROM prom_metric.node_filesystem_size_bytes size
                LEFT JOIN prom_metric.node_filesystem_free_bytes free
                    ON free.time = size.time 
                    AND free.labels[2] = size.labels[2] 
                    AND free.labels[4] = size.labels[4]
                    AND free.labels[6] = size.labels[6]
                WHERE size.time >= ?
                AND size.labels[3] NOT IN ('tmpfs', 'devtmpfs')
                ORDER BY size.time DESC, size.labels[2], size.labels[4], size.labels[6]
                LIMIT 1000
                """;

            return jdbcTemplate.query(sql, new Object[]{java.sql.Timestamp.from(since)}, (rs, rowNum) -> {
                long totalBytes = rs.getLong("total_bytes");
                long freeBytes = rs.getObject("free_bytes") != null ? rs.getLong("free_bytes") : 0;
                long usedBytes = totalBytes - freeBytes;
                double usagePercent = totalBytes > 0 ? (usedBytes * 100.0 / totalBytes) : 0.0;

                return DiskMetricResponse.builder()
                        .time(rs.getTimestamp("time").toInstant())
                        .instance(rs.getString("instance"))
                        .device(rs.getString("device"))
                        .mountpoint(rs.getString("mountpoint"))
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
            });
        } catch (Exception e) {
            log.error("prom_metric Disk 조회 실패", e);
            return Collections.emptyList();
        }
    }

    private List<DiskMetricResponse> queryDiskFromPromMetricByInstance(String instance, Instant since) {
        try {
            String sql = """
                SELECT DISTINCT ON (size.time, size.labels[2], size.labels[6])
                    size.time,
                    size.labels[2] as device,
                    size.labels[4] as instance,
                    size.labels[6] as mountpoint,
                    size.value as total_bytes,
                    free.value as free_bytes
                FROM prom_metric.node_filesystem_size_bytes size
                LEFT JOIN prom_metric.node_filesystem_free_bytes free
                    ON free.time = size.time 
                    AND free.labels[2] = size.labels[2] 
                    AND free.labels[4] = size.labels[4]
                    AND free.labels[6] = size.labels[6]
                WHERE size.time >= ? AND size.labels[4] = ?
                AND size.labels[3] NOT IN ('tmpfs', 'devtmpfs')
                ORDER BY size.time DESC, size.labels[2], size.labels[6]
                LIMIT 100
                """;

            return jdbcTemplate.query(sql, new Object[]{java.sql.Timestamp.from(since), instance}, (rs, rowNum) -> {
                long totalBytes = rs.getLong("total_bytes");
                long freeBytes = rs.getObject("free_bytes") != null ? rs.getLong("free_bytes") : 0;
                long usedBytes = totalBytes - freeBytes;
                double usagePercent = totalBytes > 0 ? (usedBytes * 100.0 / totalBytes) : 0.0;

                return DiskMetricResponse.builder()
                        .time(rs.getTimestamp("time").toInstant())
                        .instance(rs.getString("instance"))
                        .device(rs.getString("device"))
                        .mountpoint(rs.getString("mountpoint"))
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
            });
        } catch (Exception e) {
            log.error("prom_metric Disk 조회 실패 - instance: {}", instance, e);
            return Collections.emptyList();
        }
    }

    /**
     * prom_metric에서 Temperature 메트릭 조회
     */
    private List<TemperatureMetricResponse> queryTemperatureFromPromMetric(Instant since) {
        log.warn("prom_metric Temperature 조회 미구현");
        return Collections.emptyList();
    }

    private List<TemperatureMetricResponse> queryTemperatureFromPromMetricByInstance(String instance, Instant since) {
        log.warn("prom_metric Temperature 조회 미구현 - instance: {}", instance);
        return Collections.emptyList();
    }
}