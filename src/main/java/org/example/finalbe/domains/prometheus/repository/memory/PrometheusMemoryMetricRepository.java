package org.example.finalbe.domains.prometheus.repository.memory;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PrometheusMemoryMetricRepository {

    private final EntityManager entityManager;

    /**
     * 메모리 사용률 계산
     * (Total - Available) / Total * 100
     */
    public List<Object[]> getMemoryUsageTrend(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                mt.time,
                mt.value as total_memory,
                ma.value as available_memory,
                (((mt.value - ma.value) / mt.value * 100)::double precision) as memory_usage_percent
            FROM prom_metric.node_memory_memtotal_bytes mt
            JOIN prom_metric.node_memory_memavailable_bytes ma 
                ON mt.time = ma.time AND mt.instance_id = ma.instance_id
            WHERE mt.time BETWEEN :startTime AND :endTime
            ORDER BY mt.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 메모리 구성 상세 (Active, Inactive, Buffers, Cached, Free)
     */
    public List<Object[]> getMemoryComposition(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                ma.time,
                ma.value as active,
                mi.value as inactive,
                mb.value as buffers,
                mc.value as cached,
                mf.value as free
            FROM prom_metric.node_memory_active_bytes ma
            JOIN prom_metric.node_memory_inactive_bytes mi 
                ON ma.time = mi.time AND ma.instance_id = mi.instance_id
            JOIN prom_metric.node_memory_buffers_bytes mb 
                ON ma.time = mb.time AND ma.instance_id = mb.instance_id
            JOIN prom_metric.node_memory_cached_bytes mc 
                ON ma.time = mc.time AND ma.instance_id = mc.instance_id
            JOIN prom_metric.node_memory_memfree_bytes mf 
                ON ma.time = mf.time AND ma.instance_id = mf.instance_id
            WHERE ma.time BETWEEN :startTime AND :endTime
            ORDER BY ma.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * SWAP 메모리 사용 추이
     */
    public List<Object[]> getSwapUsageTrend(Instant startTime, Instant endTime) {
        String query = """
            SELECT 
                st.time,
                st.value as total_swap,
                sf.value as free_swap,
                (st.value - sf.value) as used_swap,
                (((st.value - sf.value) / NULLIF(st.value, 0) * 100)::double precision) as swap_usage_percent
            FROM prom_metric.node_memory_swaptotal_bytes st
            JOIN prom_metric.node_memory_swapfree_bytes sf 
                ON st.time = sf.time AND st.instance_id = sf.instance_id
            WHERE st.time BETWEEN :startTime AND :endTime
            ORDER BY st.time ASC
            """;

        return entityManager.createNativeQuery(query)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getResultList();
    }

    /**
     * 현재 메모리 사용률 (Gauge용)
     */
    public Object[] getCurrentMemoryUsage() {
        String query = """
            SELECT 
                mt.value as total_memory,
                ma.value as available_memory,
                (((mt.value - ma.value) / mt.value * 100)::double precision) as memory_usage_percent
            FROM prom_metric.node_memory_memtotal_bytes mt
            JOIN prom_metric.node_memory_memavailable_bytes ma 
                ON mt.instance_id = ma.instance_id
            WHERE mt.time = (SELECT MAX(time) FROM prom_metric.node_memory_memtotal_bytes)
            LIMIT 1
            """;

        List<Object[]> results = entityManager.createNativeQuery(query).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}