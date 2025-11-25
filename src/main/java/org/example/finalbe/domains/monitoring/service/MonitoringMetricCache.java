package org.example.finalbe.domains.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.monitoring.dto.RackStatisticsDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for latest monitoring metrics used to bootstrap SSE clients immediately.
 */
@Slf4j
@Component
public class MonitoringMetricCache {

    private final Map<Long, SystemMetric> latestSystemMetrics = new ConcurrentHashMap<>();
    private final Map<Long, DiskMetric> latestDiskMetrics = new ConcurrentHashMap<>();
    private final Map<Long, List<NetworkMetric>> latestNetworkMetrics = new ConcurrentHashMap<>();
    private final Map<Long, EnvironmentMetric> latestEnvironmentMetrics = new ConcurrentHashMap<>();
    private final Map<Long, RackStatisticsDto> latestRackStatistics = new ConcurrentHashMap<>();

    public void updateSystemMetric(SystemMetric metric) {
        latestSystemMetrics.put(metric.getEquipmentId(), metric);
    }

    public Optional<SystemMetric> getSystemMetric(Long equipmentId) {
        return Optional.ofNullable(latestSystemMetrics.get(equipmentId));
    }

    public void updateDiskMetric(DiskMetric metric) {
        latestDiskMetrics.put(metric.getEquipmentId(), metric);
    }

    public Optional<DiskMetric> getDiskMetric(Long equipmentId) {
        return Optional.ofNullable(latestDiskMetrics.get(equipmentId));
    }

    public void updateNetworkMetric(NetworkMetric metric) {
        latestNetworkMetrics.compute(metric.getEquipmentId(), (id, current) -> {
            List<NetworkMetric> list = current != null ? current : new ArrayList<>();
            list.removeIf(existing -> existing.getNicName().equals(metric.getNicName()));
            list.add(metric);
            return list;
        });
    }

    public List<NetworkMetric> getNetworkMetrics(Long equipmentId) {
        return latestNetworkMetrics.getOrDefault(equipmentId, Collections.emptyList());
    }

    public void updateEnvironmentMetric(EnvironmentMetric metric) {
        latestEnvironmentMetrics.put(metric.getRackId(), metric);
    }

    public Optional<EnvironmentMetric> getEnvironmentMetric(Long rackId) {
        return Optional.ofNullable(latestEnvironmentMetrics.get(rackId));
    }

    public void updateRackStatistics(RackStatisticsDto statistics) {
        latestRackStatistics.put(statistics.getRackId(), statistics);
    }

    public Optional<RackStatisticsDto> getRackStatistics(Long rackId) {
        return Optional.ofNullable(latestRackStatistics.get(rackId));
    }
}