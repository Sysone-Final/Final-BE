package org.example.finalbe.domains.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * TimescaleDB Hypertable 자동 설정
 * 애플리케이션 시작 시 메트릭 테이블을 hypertable로 자동 변환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimescaleDBInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initializeHypertables() {
        log.info("🔧 TimescaleDB Hypertable 초기화 시작...");

        try {
            // TimescaleDB 익스텐션 확인
            checkTimescaleExtension();

            // 각 메트릭 테이블을 hypertable로 변환
            createHypertableIfNotExists("system_metrics", "generate_time");
            createHypertableIfNotExists("disk_metrics", "generate_time");
            createHypertableIfNotExists("network_metrics", "generate_time");
            createHypertableIfNotExists("environment_metrics", "generate_time");

            // 압축 정책 설정 (7일 후 압축)
            setupCompressionPolicies();

            // 보관 정책 설정 (30일 후 삭제)
            setupRetentionPolicies();

            log.info("✅ TimescaleDB Hypertable 초기화 완료!");

        } catch (Exception e) {
            log.warn("⚠️ TimescaleDB 초기화 실패 (일반 PostgreSQL로 동작): {}", e.getMessage());
            log.warn("💡 TimescaleDB를 사용하려면 DB에 timescaledb 익스텐션을 설치해주세요.");
        }
    }

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

    /**
     * Hypertable 생성 (이미 존재하면 스킵)
     */
    private void createHypertableIfNotExists(String tableName, String timeColumn) {
        try {
            // 이미 hypertable인지 확인
            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.hypertables WHERE hypertable_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);

            if (count != null && count > 0) {
                log.info("⏭️  {} - 이미 hypertable로 설정됨 (스킵)", tableName);
                return;
            }

            // Hypertable 생성
            String createSql = String.format(
                    "SELECT create_hypertable('%s', '%s', chunk_time_interval => INTERVAL '1 day', if_not_exists => TRUE)",
                    tableName, timeColumn
            );
            jdbcTemplate.execute(createSql);

            log.info("✅ {} - Hypertable 변환 완료", tableName);

        } catch (Exception e) {
            log.warn("⚠️  {} - Hypertable 변환 실패: {}", tableName, e.getMessage());
        }
    }

    /**
     * 압축 정책 설정 (7일 지난 데이터 자동 압축)
     */
    private void setupCompressionPolicies() {
        try {
            setupCompressionForTable("system_metrics", "device_id");
            setupCompressionForTable("disk_metrics", "device_id,partition_path");
            setupCompressionForTable("network_metrics", "device_id,nic_name");
            setupCompressionForTable("environment_metrics", "device_id");

            log.info("✅ 압축 정책 설정 완료 (7일 후 자동 압축)");

        } catch (Exception e) {
            log.warn("⚠️ 압축 정책 설정 실패: {}", e.getMessage());
        }
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
                log.info("⏭️  {} - 이미 압축 설정됨 (스킵)", tableName);
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

            log.info("✅ {} - 압축 정책 추가 완료", tableName);

        } catch (Exception e) {
            log.debug("⚠️ {} - 압축 정책 추가 실패: {}", tableName, e.getMessage());
        }
    }

    /**
     * 보관 정책 설정 (30일 지난 데이터 자동 삭제)
     */
    private void setupRetentionPolicies() {
        try {
            setupRetentionForTable("system_metrics");
            setupRetentionForTable("disk_metrics");
            setupRetentionForTable("network_metrics");
            setupRetentionForTable("environment_metrics");

            log.info("✅ 보관 정책 설정 완료 (30일 후 자동 삭제)");

        } catch (Exception e) {
            log.warn("⚠️ 보관 정책 설정 실패: {}", e.getMessage());
        }
    }

    /**
     * 특정 테이블에 보관 정책 설정
     */
    private void setupRetentionForTable(String tableName) {
        try {
            // 이미 보관 정책이 있는지 확인
            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.jobs " +
                    "WHERE proc_name = 'policy_retention' " +
                    "AND hypertable_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);

            if (count != null && count > 0) {
                log.info("⏭️  {} - 이미 보관 정책 설정됨 (스킵)", tableName);
                return;
            }

            // 보관 정책 추가 (30일 지난 chunk 자동 삭제)
            String policySql = String.format(
                    "SELECT add_retention_policy('%s', INTERVAL '30 days')",
                    tableName
            );
            jdbcTemplate.execute(policySql);

            log.info("✅ {} - 보관 정책 추가 완료 (30일 보관)", tableName);

        } catch (Exception e) {
            log.debug("⚠️ {} - 보관 정책 추가 실패: {}", tableName, e.getMessage());
        }
    }
}