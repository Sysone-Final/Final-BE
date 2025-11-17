package org.example.finalbe.domains.prometheus.repository.cpu;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PrometheusCpuMetricRepository {

    private final EntityManager entityManager;

    /**
     * CPU 사용률 계산 (idle 모드 제외)
     * 100 - idle% = 실제 CPU 사용률
     */
    public List<Object[]> getCpuUsageTrend(Instant startTime, Instant endTime) {
        String query = """
            WITH cpu_data AS (
                SELECT 
                    time,
                    mode_id,
                    value,
                    LAG(value) OVER (PARTITION BY mode_id ORDER BY time) as prev_value
                FROM prom_metric.node_cpu_seconds_total
                WHERE time BETWEEN :startTime AND :endTime
            ),
            cpu_rates AS (
                SELECT 
                    time,
                    mode_id,
                    CASE 
                        WHEN prev_value IS NOT NULL 
                        THEN (value - prev_value) 
                        ELSE 0 
                    END as rate
                FROM cpu_data
            )
            SELECT 
                time,
                (100 - (SUM(CASE WHEN mode_id = 1 THEN rate ELSE 0 END) * 100.0 / NULLIF(SUM(rate), 0)))::double precision as cpu_usage
            FROM cpu_rates
            GROUP BY time
            ORDER BY time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * CPU 모드별 분포 (user, system, iowait, irq, softirq)
     */
    public List<Object[]> getCpuModeDistribution(Instant startTime, Instant endTime) {
        String query = """
            WITH cpu_data AS (
                SELECT 
                    time,
                    mode_id,
                    value,
                    LAG(value) OVER (PARTITION BY mode_id ORDER BY time) as prev_value
                FROM prom_metric.node_cpu_seconds_total
                WHERE time BETWEEN :startTime AND :endTime
            ),
            cpu_rates AS (
                SELECT 
                    time,
                    mode_id,
                    CASE 
                        WHEN prev_value IS NOT NULL 
                        THEN (value - prev_value) 
                        ELSE 0 
                    END as rate
                FROM cpu_data
            )
            SELECT 
                time,
                SUM(CASE WHEN mode_id = 2 THEN rate ELSE 0 END)::double precision as user_mode,
                SUM(CASE WHEN mode_id = 3 THEN rate ELSE 0 END)::double precision as system_mode,
                SUM(CASE WHEN mode_id = 4 THEN rate ELSE 0 END)::double precision as iowait_mode,
                SUM(CASE WHEN mode_id = 5 THEN rate ELSE 0 END)::double precision as irq_mode,
                SUM(CASE WHEN mode_id = 6 THEN rate ELSE 0 END)::double precision as softirq_mode
            FROM cpu_rates
            GROUP BY time
            ORDER BY time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 시스템 부하 (1분, 5분, 15분 평균)
     */
    public List<Object[]> getLoadAverage(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                l1.time,
                l1.value::double precision as load1,
                l5.value::double precision as load5,
                l15.value::double precision as load15
            FROM prom_metric.node_load1 l1
            JOIN prom_metric.node_load5 l5 ON l1.time = l5.time
            JOIN prom_metric.node_load15 l15 ON l1.time = l15.time
            WHERE l1.time BETWEEN :startTime AND :endTime
            ORDER BY l1.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 컨텍스트 스위치 추이
     */
    public List<Object[]> getContextSwitchTrend(Instant startTime, Instant endTime) {
        String query = """
            WITH context_data AS (
                SELECT 
                    time,
                    value,
                    LAG(value) OVER (ORDER BY time) as prev_value
                FROM prom_metric.node_context_switches_total
                WHERE time BETWEEN :startTime AND :endTime
            )
            SELECT 
                time,
                (CASE 
                    WHEN prev_value IS NOT NULL 
                    THEN (value - prev_value)
                    ELSE 0 
                END)::double precision as context_switches_per_sec
            FROM context_data
            ORDER BY time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 현재 CPU 사용률 (최신값)
     */
    public Double getCurrentCpuUsage() {
        String query = """
            WITH latest_cpu AS (
                SELECT
                    mode_id,
                    value,
                    time,
                    LAG(value) OVER (PARTITION BY mode_id ORDER BY time) as prev_value
                FROM prom_metric.node_cpu_seconds_total
                WHERE time >= NOW() - INTERVAL '1 minute'
            )
            SELECT
                (100 - (SUM(CASE WHEN mode_id = 1 AND prev_value IS NOT NULL
                    THEN (value - prev_value) ELSE 0 END) * 100.0 /
                    NULLIF(SUM(CASE WHEN prev_value IS NOT NULL
                    THEN (value - prev_value) ELSE 0 END), 0)))::double precision as current_cpu_usage
            FROM latest_cpu
            """;

        List<Double> results = entityManager.createNativeQuery(query).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
    /**
     * 모든 인스턴스의 최신 CPU 데이터 조회
     */
    public List<Object[]> getLatestCpuUsageAllInstances() {
        String query = """
        WITH latest_time AS (
            SELECT MAX(time) as max_time
            FROM prom_metric.node_cpu_seconds_total
        )
        SELECT 
            cpu.instance_id,
            100 - AVG(CASE WHEN cpu.mode_id = (SELECT id FROM prom_metric.mode WHERE mode = 'idle') 
                THEN cpu.value * 100 ELSE 0 END) as cpu_usage_percent
        FROM prom_metric.node_cpu_seconds_total cpu
        CROSS JOIN latest_time lt
        WHERE cpu.time = lt.max_time
        GROUP BY cpu.instance_id
        """;

        return entityManager.createNativeQuery(query).getResultList();
    }

    /**
     * 모든 인스턴스의 최신 Load Average 조회
     */
    public List<Object[]> getLatestLoadAverageAllInstances() {
        String query = """
        WITH latest_time AS (
            SELECT MAX(time) as max_time FROM prom_metric.node_load1
        )
        SELECT 
            l1.instance_id,
            l1.value as load1,
            l5.value as load5,
            l15.value as load15
        FROM prom_metric.node_load1 l1
        JOIN prom_metric.node_load5 l5 
            ON l1.time = l5.time AND l1.instance_id = l5.instance_id
        JOIN prom_metric.node_load15 l15 
            ON l1.time = l15.time AND l1.instance_id = l15.instance_id
        CROSS JOIN latest_time lt
        WHERE l1.time = lt.max_time
        """;

        return entityManager.createNativeQuery(query).getResultList();
    }

    /**
     * 모든 인스턴스의 최신 컨텍스트 스위치 조회
     */
    public List<Object[]> getLatestContextSwitchesAllInstances() {
        String query = """
        WITH latest_time AS (
            SELECT MAX(time) as max_time 
            FROM prom_metric.node_context_switches_total
        )
        SELECT 
            instance_id,
            value as context_switches
        FROM prom_metric.node_context_switches_total
        CROSS JOIN latest_time lt
        WHERE time = lt.max_time
        """;

        return entityManager.createNativeQuery(query).getResultList();
    }
}