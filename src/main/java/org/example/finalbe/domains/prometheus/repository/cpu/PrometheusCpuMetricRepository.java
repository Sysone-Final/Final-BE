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
            ),
            idle_mode AS (
                SELECT id as mode_id FROM _prom_catalog.label WHERE key = 'mode' AND value = 'idle'
            )
            SELECT 
                time,
                (100 - (SUM(CASE WHEN cr.mode_id = im.mode_id 
                    THEN rate ELSE 0 END) * 100.0 / NULLIF(SUM(rate), 0)))::double precision as cpu_usage
            FROM cpu_rates cr
            CROSS JOIN idle_mode im
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
            ),
            mode_ids AS (
                SELECT 
                    MAX(CASE WHEN value = 'user' THEN id END) as user_id,
                    MAX(CASE WHEN value = 'system' THEN id END) as system_id,
                    MAX(CASE WHEN value = 'iowait' THEN id END) as iowait_id,
                    MAX(CASE WHEN value = 'irq' THEN id END) as irq_id,
                    MAX(CASE WHEN value = 'softirq' THEN id END) as softirq_id
                FROM _prom_catalog.label
                WHERE key = 'mode'
            )
            SELECT 
                cr.time,
                SUM(CASE WHEN cr.mode_id = m.user_id THEN rate ELSE 0 END)::double precision as user_mode,
                SUM(CASE WHEN cr.mode_id = m.system_id THEN rate ELSE 0 END)::double precision as system_mode,
                SUM(CASE WHEN cr.mode_id = m.iowait_id THEN rate ELSE 0 END)::double precision as iowait_mode,
                SUM(CASE WHEN cr.mode_id = m.irq_id THEN rate ELSE 0 END)::double precision as irq_mode,
                SUM(CASE WHEN cr.mode_id = m.softirq_id THEN rate ELSE 0 END)::double precision as softirq_mode
            FROM cpu_rates cr
            CROSS JOIN mode_ids m
            GROUP BY cr.time
            ORDER BY cr.time ASC
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
            WITH recent_cpu AS (
                SELECT
                    mode_id,
                    value,
                    time,
                    LAG(value) OVER (PARTITION BY mode_id ORDER BY time) as prev_value
                FROM prom_metric.node_cpu_seconds_total
                WHERE time >= NOW() - INTERVAL '10 minutes'
            ),
            idle_mode AS (
                SELECT id as mode_id FROM _prom_catalog.label WHERE key = 'mode' AND value = 'idle'
            )
            SELECT
                (100 - (SUM(CASE WHEN rc.mode_id = im.mode_id AND prev_value IS NOT NULL
                    THEN (value - prev_value) ELSE 0 END) * 100.0 /
                    NULLIF(SUM(CASE WHEN prev_value IS NOT NULL
                    THEN (value - prev_value) ELSE 0 END), 0)))::double precision as current_cpu_usage
            FROM recent_cpu rc
            CROSS JOIN idle_mode im
            """;

        List<Double> results = entityManager.createNativeQuery(query).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 모든 인스턴스의 최신 CPU 데이터 조회
     */
    public List<Object[]> getLatestCpuUsageAllInstances() {
        String query = """
        WITH idle_mode AS (
            SELECT id as mode_id 
            FROM _prom_catalog.label 
            WHERE key = 'mode' AND value = 'idle'
        ),
        recent_data AS (
            SELECT *
            FROM prom_metric.node_cpu_seconds_total
            WHERE time >= NOW() - INTERVAL '10 minutes'
        ),
        latest_time_per_instance AS (
            SELECT 
                instance_id,
                MAX(time) as max_time
            FROM recent_data
            GROUP BY instance_id
        )
        SELECT 
            cpu.instance_id,
            100 - (SUM(CASE WHEN cpu.mode_id = idle.mode_id 
                THEN cpu.value ELSE 0 END) * 100.0 / NULLIF(SUM(cpu.value), 0)) as cpu_usage_percent
        FROM recent_data cpu
        JOIN latest_time_per_instance lt 
            ON cpu.instance_id = lt.instance_id AND cpu.time = lt.max_time
        CROSS JOIN idle_mode idle
        GROUP BY cpu.instance_id
        ORDER BY cpu.instance_id
        """;

        return entityManager.createNativeQuery(query).getResultList();
    }

    /**
     * 모든 인스턴스의 최신 Load Average 조회
     */
    public List<Object[]> getLatestLoadAverageAllInstances() {
        String query = """
        WITH latest_time AS (
            SELECT MAX(time) as max_time 
            FROM prom_metric.node_load1
            WHERE time >= NOW() - INTERVAL '10 minutes'
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
            WHERE time >= NOW() - INTERVAL '10 minutes'
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