package org.example.finalbe.domains.prometheus.repository.process;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PrometheusProcessMetricRepository {

    private final EntityManager entityManager;

    /**
     * 실행 중인 프로세스 수 추이
     */
    public List<Object[]> getRunningProcessesTrend(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                time,
                value::double precision as running_processes
            FROM prom_metric.node_procs_running
            WHERE time BETWEEN :startTime AND :endTime
            ORDER BY time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 블록된 프로세스 수 추이
     */
    public List<Object[]> getBlockedProcessesTrend(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                time,
                value::double precision as blocked_processes
            FROM prom_metric.node_procs_blocked
            WHERE time BETWEEN :startTime AND :endTime
            ORDER BY time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 현재 프로세스 상태
     */
    public Object[] getCurrentProcessStatus() {
        String query = """
            SELECT 
                (SELECT value FROM prom_metric.node_procs_running 
                 WHERE time = (SELECT MAX(time) FROM prom_metric.node_procs_running))::double precision as running,
                (SELECT value FROM prom_metric.node_procs_blocked 
                 WHERE time = (SELECT MAX(time) FROM prom_metric.node_procs_blocked))::double precision as blocked
            """;

        List<Object[]> results = entityManager.createNativeQuery(query).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}