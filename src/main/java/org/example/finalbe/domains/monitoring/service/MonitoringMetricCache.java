/**
 * 작성자: 황요한
 * 최신 모니터링 메트릭들을 In-Memory로 캐싱하여 SSE 클라이언트 초기 부팅 속도 개선
 */
package org.example.finalbe.domains.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.monitoring.dto.RackStatisticsDto;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MonitoringMetricCache {

    private final Map<Long, SystemMetric> latestSystemMetrics = new ConcurrentHashMap<>();
    private final Map<Long, DiskMetric> latestDiskMetrics = new ConcurrentHashMap<>();
    private final Map<Long, List<NetworkMetric>> latestNetworkMetrics = new ConcurrentHashMap<>();
    private final Map<Long, EnvironmentMetric> latestEnvironmentMetrics = new ConcurrentHashMap<>();
    private final Map<Long, RackStatisticsDto> latestRackStatistics = new ConcurrentHashMap<>();

    /** 최신 SystemMetric 업데이트 */
    public void updateSystemMetric(SystemMetric metric) {
        latestSystemMetrics.put(metric.getEquipmentId(), metric);
    }

    /** 최신 SystemMetric 조회 */
    public Optional<SystemMetric> getSystemMetric(Long equipmentId) {
        return Optional.ofNullable(latestSystemMetrics.get(equipmentId));
    }

    /** 최신 DiskMetric 업데이트 */
    public void updateDiskMetric(DiskMetric metric) {
        latestDiskMetrics.put(metric.getEquipmentId(), metric);
    }

    /** 최신 DiskMetric 조회 */
    public Optional<DiskMetric> getDiskMetric(Long equipmentId) {
        return Optional.ofNullable(latestDiskMetrics.get(equipmentId));
    }

    /** 최신 NetworkMetric 업데이트 (NIC 단위 중복 제거) */
    public void updateNetworkMetric(NetworkMetric metric) {
        latestNetworkMetrics.compute(metric.getEquipmentId(), (id, current) -> {
            List<NetworkMetric> list = current != null ? current : new ArrayList<>();
            list.removeIf(existing -> existing.getNicName().equals(metric.getNicName()));
            list.add(metric);
            return list;
        });
    }

    /** 최신 NetworkMetric 리스트 조회 */
    public List<NetworkMetric> getNetworkMetrics(Long equipmentId) {
        return latestNetworkMetrics.getOrDefault(equipmentId, Collections.emptyList());
    }

    /** 최신 EnvironmentMetric 업데이트 */
    public void updateEnvironmentMetric(EnvironmentMetric metric) {
        latestEnvironmentMetrics.put(metric.getRackId(), metric);
    }

    /** 최신 EnvironmentMetric 조회 */
    public Optional<EnvironmentMetric> getEnvironmentMetric(Long rackId) {
        return Optional.ofNullable(latestEnvironmentMetrics.get(rackId));
    }

    /** 최신 RackStatistics 업데이트 */
    public void updateRackStatistics(RackStatisticsDto statistics) {
        latestRackStatistics.put(statistics.getRackId(), statistics);
    }

    /** 최신 RackStatistics 조회 */
    public Optional<RackStatisticsDto> getRackStatistics(Long rackId) {
        return Optional.ofNullable(latestRackStatistics.get(rackId));
    }
}
