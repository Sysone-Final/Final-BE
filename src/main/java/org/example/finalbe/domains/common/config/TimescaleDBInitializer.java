package org.example.finalbe.domains.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * TimescaleDB 자동 설정
 * - Hypertable 생성
 * - Continuous Aggregates (Materialized View) 생성
 * - Compression Policy 설정
 * - Retention Policy 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimescaleDBInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeHypertables() {
        log.info("=".repeat(80));
        log.info("🔧 TimescaleDB 초기화 시작...");
        log.info("=".repeat(80));

        try {
            // 1. TimescaleDB 익스텐션 확인
            checkTimescaleExtension();

            // 2. Hypertable 생성
            createPrometheusHypertables();

            // 3. Continuous Aggregates 생성 (✅ 추가)
            setupContinuousAggregates();

            // 4. 압축 정책 설정
            setupCompressionPolicies();

            // 5. 보관 정책 설정
            setupRetentionPolicies();

            log.info("=".repeat(80));
            log.info("✅ TimescaleDB 초기화 완료!");
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.warn("=".repeat(80));
            log.warn("⚠️ TimescaleDB 초기화 실패 (일반 PostgreSQL로 동작): {}", e.getMessage());
            log.warn("💡 TimescaleDB를 사용하려면 DB에 timescaledb 익스텐션을 설치해주세요.");
            log.warn("=".repeat(80));
        }
    }

    // ========================================
    // 1. TimescaleDB 익스텐션 확인
    // ========================================

    /**
     * TimescaleDB 익스텐션 설치 확인
     */
    private void checkTimescaleExtension() {
        String sql = "SELECT COUNT(*) FROM pg_extension WHERE extname = 'timescaledb'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        if (count == null || count == 0) {
            throw new RuntimeException("TimescaleDB 익스텐션이 설치되지 않았습니다.");
        }

        log.info("✅ TimescaleDB 익스텐션 확인 완료");
    }

    // ========================================
    // 2. Hypertable 생성
    // ========================================

    /**
     * 프로메테우스 메트릭 테이블들을 Hypertable로 변환
     */
    private void createPrometheusHypertables() {
        log.info("📊 Hypertable 생성 중...");

        createHypertableIfNotExists("prometheus_cpu_metrics", "timestamp");
        createHypertableIfNotExists("prometheus_memory_metrics", "timestamp");
        createHypertableIfNotExists("prometheus_network_metrics", "timestamp");
        createHypertableIfNotExists("prometheus_disk_metrics", "timestamp");
        createHypertableIfNotExists("prometheus_temperature_metrics", "timestamp");

        log.info("✅ Hypertable 생성 완료");
    }

    /**
     * 개별 Hypertable 생성 (이미 존재하면 스킵)
     */
    private void createHypertableIfNotExists(String tableName, String timeColumn) {
        try {
            // 이미 hypertable인지 확인
            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.hypertables WHERE hypertable_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);

            if (count != null && count > 0) {
                log.info("  ⏭️  {} - 이미 hypertable로 설정됨 (스킵)", tableName);
                return;
            }

            // Hypertable 생성
            String createSql = String.format(
                    "SELECT create_hypertable('%s', '%s', chunk_time_interval => INTERVAL '1 day', if_not_exists => TRUE)",
                    tableName, timeColumn
            );
            jdbcTemplate.execute(createSql);

            log.info("  ✅ {} - Hypertable 변환 완료", tableName);

        } catch (Exception e) {
            log.warn("  ⚠️  {} - Hypertable 변환 실패: {}", tableName, e.getMessage());
        }
    }

    // ========================================
    // 3. Continuous Aggregates 생성 (✅ 추가)
    // ========================================

    /**
     * Continuous Aggregates (Materialized View) 생성
     * - 1분 단위 집계
     * - 5분 단위 집계
     * - 1시간 단위 집계
     */
    private void setupContinuousAggregates() {
        log.info("🔄 Continuous Aggregates 생성 중...");

        // CPU 메트릭 Continuous Aggregates
        createCpuContinuousAggregates();

        // 메모리 메트릭 Continuous Aggregates
        createMemoryContinuousAggregates();

        // 네트워크 메트릭 Continuous Aggregates
        createNetworkContinuousAggregates();

        // 디스크 메트릭 Continuous Aggregates
        createDiskContinuousAggregates();

        log.info("✅ Continuous Aggregates 생성 완료");
    }

    /**
     * CPU 메트릭 Continuous Aggregates 생성
     */
    private void createCpuContinuousAggregates() {
        // 1분 단위 집계
        createContinuousAggregate(
                "prometheus_cpu_metrics_1min",
                """
                CREATE MATERIALIZED VIEW prometheus_cpu_metrics_1min
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('1 minute', timestamp) AS bucket,
                    instance,
                    AVG(cpu_usage_percent) AS avg_cpu_usage,
                    AVG(user_percent) AS avg_user,
                    AVG(system_percent) AS avg_system,
                    AVG(iowait_percent) AS avg_iowait,
                    AVG(irq_percent) AS avg_irq,
                    AVG(softirq_percent) AS avg_softirq,
                    AVG(load1) AS avg_load1,
                    AVG(load5) AS avg_load5,
                    AVG(load15) AS avg_load15,
                    AVG(context_switches_per_sec) AS avg_context_switches,
                    COUNT(*) AS sample_count
                FROM prometheus_cpu_metrics
                GROUP BY bucket, instance
                """,
                "1 hour", "15 seconds", "15 seconds"
        );

        // 5분 단위 집계
        createContinuousAggregate(
                "prometheus_cpu_metrics_5min",
                """
                CREATE MATERIALIZED VIEW prometheus_cpu_metrics_5min
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('5 minutes', timestamp) AS bucket,
                    instance,
                    AVG(cpu_usage_percent) AS avg_cpu_usage,
                    AVG(user_percent) AS avg_user,
                    AVG(system_percent) AS avg_system,
                    AVG(iowait_percent) AS avg_iowait,
                    AVG(load1) AS avg_load1,
                    AVG(load5) AS avg_load5,
                    AVG(load15) AS avg_load15,
                    COUNT(*) AS sample_count
                FROM prometheus_cpu_metrics
                GROUP BY bucket, instance
                """,
                "6 hours", "1 minute", "1 minute"
        );

        // 1시간 단위 집계
        createContinuousAggregate(
                "prometheus_cpu_metrics_1hour",
                """
                CREATE MATERIALIZED VIEW prometheus_cpu_metrics_1hour
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('1 hour', timestamp) AS bucket,
                    instance,
                    AVG(cpu_usage_percent) AS avg_cpu_usage,
                    AVG(user_percent) AS avg_user,
                    AVG(system_percent) AS avg_system,
                    AVG(iowait_percent) AS avg_iowait,
                    AVG(load1) AS avg_load1,
                    AVG(load5) AS avg_load5,
                    AVG(load15) AS avg_load15,
                    COUNT(*) AS sample_count
                FROM prometheus_cpu_metrics
                GROUP BY bucket, instance
                """,
                "1 day", "5 minutes", "5 minutes"
        );
    }

    /**
     * 메모리 메트릭 Continuous Aggregates 생성
     */
    private void createMemoryContinuousAggregates() {
        // 1분 단위 집계
        createContinuousAggregate(
                "prometheus_memory_metrics_1min",
                """
                CREATE MATERIALIZED VIEW prometheus_memory_metrics_1min
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('1 minute', timestamp) AS bucket,
                    instance,
                    AVG(total_bytes) AS avg_total,
                    AVG(available_bytes) AS avg_available,
                    AVG(used_bytes) AS avg_used,
                    AVG(used_percent) AS avg_used_percent,
                    AVG(buffers_bytes) AS avg_buffers,
                    AVG(cached_bytes) AS avg_cached,
                    AVG(active_bytes) AS avg_active,
                    AVG(inactive_bytes) AS avg_inactive,
                    AVG(swap_total_bytes) AS avg_swap_total,
                    AVG(swap_used_bytes) AS avg_swap_used,
                    COUNT(*) AS sample_count
                FROM prometheus_memory_metrics
                GROUP BY bucket, instance
                """,
                "1 hour", "15 seconds", "15 seconds"
        );

        // 5분 단위 집계
        createContinuousAggregate(
                "prometheus_memory_metrics_5min",
                """
                CREATE MATERIALIZED VIEW prometheus_memory_metrics_5min
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('5 minutes', timestamp) AS bucket,
                    instance,
                    AVG(total_bytes) AS avg_total,
                    AVG(available_bytes) AS avg_available,
                    AVG(used_percent) AS avg_used_percent,
                    AVG(swap_total_bytes) AS avg_swap_total,
                    AVG(swap_used_bytes) AS avg_swap_used,
                    COUNT(*) AS sample_count
                FROM prometheus_memory_metrics
                GROUP BY bucket, instance
                """,
                "6 hours", "1 minute", "1 minute"
        );

        // 1시간 단위 집계
        createContinuousAggregate(
                "prometheus_memory_metrics_1hour",
                """
                CREATE MATERIALIZED VIEW prometheus_memory_metrics_1hour
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('1 hour', timestamp) AS bucket,
                    instance,
                    AVG(total_bytes) AS avg_total,
                    AVG(available_bytes) AS avg_available,
                    AVG(used_percent) AS avg_used_percent,
                    AVG(swap_total_bytes) AS avg_swap_total,
                    AVG(swap_used_bytes) AS avg_swap_used,
                    COUNT(*) AS sample_count
                FROM prometheus_memory_metrics
                GROUP BY bucket, instance
                """,
                "1 day", "5 minutes", "5 minutes"
        );
    }

    /**
     * 네트워크 메트릭 Continuous Aggregates 생성
     */
    private void createNetworkContinuousAggregates() {
        // 1분 단위 집계
        createContinuousAggregate(
                "prometheus_network_metrics_1min",
                """
                CREATE MATERIALIZED VIEW prometheus_network_metrics_1min
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('1 minute', timestamp) AS bucket,
                    instance,
                    device,
                    AVG(rx_utilization_percent) AS avg_rx_usage,
                    AVG(tx_utilization_percent) AS avg_tx_usage,
                    SUM(rx_packets_total) AS sum_rx_packets,
                    SUM(tx_packets_total) AS sum_tx_packets,
                    SUM(rx_bytes_total) AS sum_rx_bytes,
                    SUM(tx_bytes_total) AS sum_tx_bytes,
                    AVG(rx_bytes_per_sec) AS avg_rx_bps,
                    AVG(tx_bytes_per_sec) AS avg_tx_bps,
                    AVG(rx_packets_per_sec) AS avg_rx_pps,
                    AVG(tx_packets_per_sec) AS avg_tx_pps,
                    SUM(rx_errors_total) AS sum_rx_errors,
                    SUM(tx_errors_total) AS sum_tx_errors,
                    SUM(rx_dropped_total) AS sum_rx_dropped,
                    SUM(tx_dropped_total) AS sum_tx_dropped,
                    COUNT(*) AS sample_count
                FROM prometheus_network_metrics
                GROUP BY bucket, instance, device
                """,
                "1 hour", "15 seconds", "15 seconds"
        );

        // 5분 단위 집계
        createContinuousAggregate(
                "prometheus_network_metrics_5min",
                """
                CREATE MATERIALIZED VIEW prometheus_network_metrics_5min
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('5 minutes', timestamp) AS bucket,
                    instance,
                    device,
                    AVG(rx_utilization_percent) AS avg_rx_usage,
                    AVG(tx_utilization_percent) AS avg_tx_usage,
                    SUM(rx_bytes_total) AS sum_rx_bytes,
                    SUM(tx_bytes_total) AS sum_tx_bytes,
                    AVG(rx_bytes_per_sec) AS avg_rx_bps,
                    AVG(tx_bytes_per_sec) AS avg_tx_bps,
                    COUNT(*) AS sample_count
                FROM prometheus_network_metrics
                GROUP BY bucket, instance, device
                """,
                "6 hours", "1 minute", "1 minute"
        );

        // 1시간 단위 집계
        createContinuousAggregate(
                "prometheus_network_metrics_1hour",
                """
                CREATE MATERIALIZED VIEW prometheus_network_metrics_1hour
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('1 hour', timestamp) AS bucket,
                    instance,
                    device,
                    AVG(rx_utilization_percent) AS avg_rx_usage,
                    AVG(tx_utilization_percent) AS avg_tx_usage,
                    SUM(rx_bytes_total) AS sum_rx_bytes,
                    SUM(tx_bytes_total) AS sum_tx_bytes,
                    COUNT(*) AS sample_count
                FROM prometheus_network_metrics
                GROUP BY bucket, instance, device
                """,
                "1 day", "5 minutes", "5 minutes"
        );
    }

    /**
     * 디스크 메트릭 Continuous Aggregates 생성
     */
    private void createDiskContinuousAggregates() {
        // 1분 단위 집계
        createContinuousAggregate(
                "prometheus_disk_metrics_1min",
                """
                CREATE MATERIALIZED VIEW prometheus_disk_metrics_1min
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('1 minute', timestamp) AS bucket,
                    instance,
                    mountpoint,
                    AVG(total_bytes) AS avg_total,
                    AVG(free_bytes) AS avg_free,
                    AVG(used_percent) AS avg_used_percent,
                    AVG(read_bytes_per_sec) AS avg_read_bps,
                    AVG(write_bytes_per_sec) AS avg_write_bps,
                    AVG(read_iops) AS avg_read_iops,
                    AVG(write_iops) AS avg_write_iops,
                    AVG(io_utilization_percent) AS avg_io_util,
                    AVG(read_time_percent) AS avg_read_time,
                    AVG(write_time_percent) AS avg_write_time,
                    AVG(total_inodes) AS avg_total_inodes,
                    AVG(free_inodes) AS avg_free_inodes,
                    AVG(inode_used_percent) AS avg_inode_used,
                    COUNT(*) AS sample_count
                FROM prometheus_disk_metrics
                GROUP BY bucket, instance, mountpoint
                """,
                "1 hour", "15 seconds", "15 seconds"
        );

        // 5분 단위 집계
        createContinuousAggregate(
                "prometheus_disk_metrics_5min",
                """
                CREATE MATERIALIZED VIEW prometheus_disk_metrics_5min
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('5 minutes', timestamp) AS bucket,
                    instance,
                    mountpoint,
                    AVG(total_bytes) AS avg_total,
                    AVG(free_bytes) AS avg_free,
                    AVG(used_percent) AS avg_used_percent,
                    AVG(read_bytes_per_sec) AS avg_read_bps,
                    AVG(write_bytes_per_sec) AS avg_write_bps,
                    AVG(io_utilization_percent) AS avg_io_util,
                    AVG(inode_used_percent) AS avg_inode_used,
                    COUNT(*) AS sample_count
                FROM prometheus_disk_metrics
                GROUP BY bucket, instance, mountpoint
                """,
                "6 hours", "1 minute", "1 minute"
        );

        // 1시간 단위 집계
        createContinuousAggregate(
                "prometheus_disk_metrics_1hour",
                """
                CREATE MATERIALIZED VIEW prometheus_disk_metrics_1hour
                WITH (timescaledb.continuous) AS
                SELECT 
                    time_bucket('1 hour', timestamp) AS bucket,
                    instance,
                    mountpoint,
                    AVG(total_bytes) AS avg_total,
                    AVG(free_bytes) AS avg_free,
                    AVG(used_percent) AS avg_used_percent,
                    AVG(io_utilization_percent) AS avg_io_util,
                    AVG(inode_used_percent) AS avg_inode_used,
                    COUNT(*) AS sample_count
                FROM prometheus_disk_metrics
                GROUP BY bucket, instance, mountpoint
                """,
                "1 day", "5 minutes", "5 minutes"
        );
    }

    /**
     * 개별 Continuous Aggregate 생성
     */
    private void createContinuousAggregate(String viewName, String createSql,
                                           String startOffset, String endOffset, String scheduleInterval) {
        try {
            // 이미 존재하는지 확인
            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.continuous_aggregates WHERE view_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, viewName);

            if (count != null && count > 0) {
                log.info("  ⏭️  {} - 이미 존재함 (스킵)", viewName);
                return;
            }

            // Continuous Aggregate 생성
            jdbcTemplate.execute(createSql);

            // Refresh Policy 추가
            String policySql = String.format(
                    "SELECT add_continuous_aggregate_policy('%s', " +
                            "start_offset => INTERVAL '%s', " +
                            "end_offset => INTERVAL '%s', " +
                            "schedule_interval => INTERVAL '%s')",
                    viewName, startOffset, endOffset, scheduleInterval
            );
            jdbcTemplate.execute(policySql);

            log.info("  ✅ {} - Continuous Aggregate 생성 완료 (refresh: {})", viewName, scheduleInterval);

        } catch (Exception e) {
            log.warn("  ⚠️ {} - Continuous Aggregate 생성 실패: {}", viewName, e.getMessage());
        }
    }

    // ========================================
    // 4. 압축 정책 설정
    // ========================================

    /**
     * 압축 정책 설정 (7일 지난 데이터 자동 압축)
     */
    private void setupCompressionPolicies() {
        log.info("🗜️ 압축 정책 설정 중...");

        setupCompressionForTable("prometheus_cpu_metrics", "instance");
        setupCompressionForTable("prometheus_memory_metrics", "instance");
        setupCompressionForTable("prometheus_network_metrics", "instance,device");
        setupCompressionForTable("prometheus_disk_metrics", "instance,mountpoint");
        setupCompressionForTable("prometheus_temperature_metrics", "instance");

        log.info("✅ 압축 정책 설정 완료 (7일 후 자동 압축)");
    }

    /**
     * 특정 테이블에 압축 설정
     */
    private void setupCompressionForTable(String tableName, String segmentBy) {
        try {
            // 이미 압축 설정됐는지 확인
            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.compression_settings WHERE hypertable_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);

            if (count != null && count > 0) {
                log.info("  ⏭️  {} - 이미 압축 설정됨 (스킵)", tableName);
                return;
            }

            // 압축 활성화
            String alterSql = String.format(
                    "ALTER TABLE %s SET (timescaledb.compress, timescaledb.compress_segmentby = '%s')",
                    tableName, segmentBy
            );
            jdbcTemplate.execute(alterSql);

            // 압축 정책 추가 (7일 지난 데이터)
            String policySql = String.format(
                    "SELECT add_compression_policy('%s', INTERVAL '7 days')",
                    tableName
            );
            jdbcTemplate.execute(policySql);

            log.info("  ✅ {} - 압축 정책 추가 완료", tableName);

        } catch (Exception e) {
            log.debug("  ⚠️ {} - 압축 정책 추가 실패: {}", tableName, e.getMessage());
        }
    }

    // ========================================
    // 5. 보관 정책 설정
    // ========================================

    /**
     * 보관 정책 설정
     * - 원본 데이터: 7일 보관
     * - 1분 집계: 30일 보관
     * - 5분 집계: 90일 보관
     * - 1시간 집계: 365일 보관
     */
    private void setupRetentionPolicies() {
        log.info("🗑️ 보관 정책 설정 중...");

        // 원본 데이터 (7일)
        setupRetentionForTable("prometheus_cpu_metrics", 7);
        setupRetentionForTable("prometheus_memory_metrics", 7);
        setupRetentionForTable("prometheus_network_metrics", 7);
        setupRetentionForTable("prometheus_disk_metrics", 7);
        setupRetentionForTable("prometheus_temperature_metrics", 7);

        // 1분 집계 (30일)
        setupRetentionForTable("prometheus_cpu_metrics_1min", 30);
        setupRetentionForTable("prometheus_memory_metrics_1min", 30);
        setupRetentionForTable("prometheus_network_metrics_1min", 30);
        setupRetentionForTable("prometheus_disk_metrics_1min", 30);

        // 5분 집계 (90일)
        setupRetentionForTable("prometheus_cpu_metrics_5min", 90);
        setupRetentionForTable("prometheus_memory_metrics_5min", 90);
        setupRetentionForTable("prometheus_network_metrics_5min", 90);
        setupRetentionForTable("prometheus_disk_metrics_5min", 90);

        // 1시간 집계 (365일)
        setupRetentionForTable("prometheus_cpu_metrics_1hour", 365);
        setupRetentionForTable("prometheus_memory_metrics_1hour", 365);
        setupRetentionForTable("prometheus_network_metrics_1hour", 365);
        setupRetentionForTable("prometheus_disk_metrics_1hour", 365);

        log.info("✅ 보관 정책 설정 완료");
    }

    /**
     * 특정 테이블에 보관 정책 설정
     */
    private void setupRetentionForTable(String tableName, int retentionDays) {
        try {
            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.jobs " +
                    "WHERE proc_name = 'policy_retention' " +
                    "AND hypertable_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);

            if (count != null && count > 0) {
                log.info("  ⏭️  {} - 이미 보관 정책 설정됨 (스킵)", tableName);
                return;
            }

            String policySql = String.format(
                    "SELECT add_retention_policy('%s', INTERVAL '%d days')",
                    tableName, retentionDays
            );
            jdbcTemplate.execute(policySql);

            log.info("  ✅ {} - 보관 정책 추가 완료 ({}일 보관)", tableName, retentionDays);

        } catch (Exception e) {
            log.debug("  ⚠️ {} - 보관 정책 추가 실패: {}", tableName, e.getMessage());
        }
    }
}