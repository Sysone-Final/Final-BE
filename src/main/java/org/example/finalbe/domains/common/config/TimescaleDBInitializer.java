//package org.example.finalbe.domains.common.config;
//
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
///**
// * TimescaleDB 자동 설정
// * - Hypertable 생성
// * - Compression Policy 설정
// * - Retention Policy 설정
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class TimescaleDBInitializer {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    @PostConstruct
//    public void initializeHypertables() {
//        log.info("=".repeat(80));
//        log.info("🔧 TimescaleDB 초기화 시작...");
//        log.info("=".repeat(80));
//
//        try {
//            // 1. TimescaleDB 익스텐션 확인
//            checkTimescaleExtension();
//
//            // 2. Hypertable 생성
//            createHypertables();
//
//            // 3. 압축 정책 설정
//            setupCompressionPolicies();
//
//            // 4. 보관 정책 설정
//            setupRetentionPolicies();
//
//            log.info("=".repeat(80));
//            log.info("✅ TimescaleDB 초기화 완료!");
//            log.info("=".repeat(80));
//
//        } catch (Exception e) {
//            log.warn("=".repeat(80));
//            log.warn("⚠️ TimescaleDB 초기화 실패 (일반 PostgreSQL로 동작): {}", e.getMessage());
//            log.warn("💡 TimescaleDB를 사용하려면 DB에 timescaledb 익스텐션을 설치해주세요.");
//            log.warn("=".repeat(80));
//        }
//    }
//
//    // ========================================
//    // 1. TimescaleDB 익스텐션 확인
//    // ========================================
//
//    private void checkTimescaleExtension() {
//        String sql = "SELECT COUNT(*) FROM pg_extension WHERE extname = 'timescaledb'";
//        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
//
//        if (count == null || count == 0) {
//            throw new RuntimeException("TimescaleDB 익스텐션이 설치되지 않았습니다.");
//        }
//
//        log.info("✅ TimescaleDB 익스텐션 확인 완료");
//    }
//
//    // ========================================
//    // 2. Hypertable 생성
//    // ========================================
//
//    private void createHypertables() {
//        log.info("📊 Hypertable 생성 중...");
//
//        // 우리가 실제로 사용하는 테이블들
//        createHypertableIfNotExists("system_metrics", "generate_time");
//        createHypertableIfNotExists("disk_metrics", "generate_time");
//        createHypertableIfNotExists("network_metrics", "generate_time");
//        createHypertableIfNotExists("environment_metrics", "generate_time");
//
//        log.info("✅ Hypertable 생성 완료");
//    }
//
//    private void createHypertableIfNotExists(String tableName, String timeColumn) {
//        try {
//            // 이미 hypertable인지 확인
//            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.hypertables WHERE hypertable_name = ?";
//            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);
//
//            if (count != null && count > 0) {
//                log.info("  ⏭️  {} - 이미 hypertable로 설정됨 (스킵)", tableName);
//                return;
//            }
//
//            // Hypertable 생성
//            String createSql = String.format(
//                    "SELECT create_hypertable('%s', '%s', chunk_time_interval => INTERVAL '1 day', if_not_exists => TRUE)",
//                    tableName, timeColumn
//            );
//            jdbcTemplate.execute(createSql);
//
//            log.info("  ✅ {} - Hypertable 변환 완료", tableName);
//
//        } catch (Exception e) {
//            log.warn("  ⚠️  {} - Hypertable 변환 실패: {}", tableName, e.getMessage());
//        }
//    }
//
//    // ========================================
//    // 3. 압축 정책 설정
//    // ========================================
//
//    private void setupCompressionPolicies() {
//        log.info("🗜️ 압축 정책 설정 중...");
//
//        setupCompressionForTable("system_metrics", "equipment_id");
//        setupCompressionForTable("disk_metrics", "equipment_id");
//        setupCompressionForTable("network_metrics", "equipment_id,nic_name");
//        setupCompressionForTable("environment_metrics", "rack_id");
//
//        log.info("✅ 압축 정책 설정 완료 (7일 후 자동 압축)");
//    }
//
//    private void setupCompressionForTable(String tableName, String segmentBy) {
//        try {
//            // 이미 압축 설정됐는지 확인
//            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.compression_settings WHERE hypertable_name = ?";
//            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);
//
//            if (count != null && count > 0) {
//                log.info("  ⏭️  {} - 이미 압축 설정됨 (스킵)", tableName);
//                return;
//            }
//
//            // 압축 활성화
//            String alterSql = String.format(
//                    "ALTER TABLE %s SET (timescaledb.compress, timescaledb.compress_segmentby = '%s')",
//                    tableName, segmentBy
//            );
//            jdbcTemplate.execute(alterSql);
//
//            // 압축 정책 추가 (7일 지난 데이터)
//            String policySql = String.format(
//                    "SELECT add_compression_policy('%s', INTERVAL '7 days')",
//                    tableName
//            );
//            jdbcTemplate.execute(policySql);
//
//            log.info("  ✅ {} - 압축 정책 추가 완료", tableName);
//
//        } catch (Exception e) {
//            log.debug("  ⚠️ {} - 압축 정책 추가 실패: {}", tableName, e.getMessage());
//        }
//    }
//
//    // ========================================
//    // 4. 보관 정책 설정
//    // ========================================
//
//    private void setupRetentionPolicies() {
//        log.info("🗑️ 보관 정책 설정 중...");
//
//        // 모든 메트릭 테이블: 90일 보관
//        setupRetentionForTable("system_metrics", 90);
//        setupRetentionForTable("disk_metrics", 90);
//        setupRetentionForTable("network_metrics", 90);
//        setupRetentionForTable("environment_metrics", 90);
//
//        log.info("✅ 보관 정책 설정 완료");
//    }
//
//    private void setupRetentionForTable(String tableName, int retentionDays) {
//        try {
//            String checkSql = "SELECT COUNT(*) FROM timescaledb_information.jobs " +
//                    "WHERE proc_name = 'policy_retention' " +
//                    "AND hypertable_name = ?";
//            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName);
//
//            if (count != null && count > 0) {
//                log.info("  ⏭️  {} - 이미 보관 정책 설정됨 (스킵)", tableName);
//                return;
//            }
//
//            String policySql = String.format(
//                    "SELECT add_retention_policy('%s', INTERVAL '%d days')",
//                    tableName, retentionDays
//            );
//            jdbcTemplate.execute(policySql);
//
//            log.info("  ✅ {} - 보관 정책 추가 완료 ({}일 보관)", tableName, retentionDays);
//
//        } catch (Exception e) {
//            log.debug("  ⚠️ {} - 보관 정책 추가 실패: {}", tableName, e.getMessage());
//        }
//    }
//}