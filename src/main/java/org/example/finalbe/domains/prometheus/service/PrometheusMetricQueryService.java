package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.domain.*;
import org.example.finalbe.domains.prometheus.dto.*;
import org.example.finalbe.domains.prometheus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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

    /**
     * 최근 15초간 데이터 조회 (SSE용)
     */
    public MetricsResponse getRecentMetrics(Instant since) {
        log.debug("최근 메트릭 조회: since={}", since);

        List<PrometheusCpuMetric> cpuMetrics = cpuMetricRepository.findRecentMetrics(since);
        List<PrometheusMemoryMetric> memoryMetrics = memoryMetricRepository.findRecentMetrics(since);
        List<PrometheusNetworkMetric> networkMetrics = networkMetricRepository.findRecentMetrics(since);
        List<PrometheusDiskMetric> diskMetrics = diskMetricRepository.findRecentMetrics(since);

        return MetricsResponse.of(
                Instant.now(),
                cpuMetrics,
                memoryMetrics,
                networkMetrics,
                diskMetrics
        );
    }

    /**
     * 최신 데이터만 조회 (현재 상태용)
     */
    public MetricsResponse getLatestMetrics() {
        log.debug("최신 메트릭 조회");

        List<PrometheusCpuMetric> cpuMetrics = cpuMetricRepository.findAllLatest();
        List<PrometheusMemoryMetric> memoryMetrics = memoryMetricRepository.findAllLatest();
        List<PrometheusNetworkMetric> networkMetrics = networkMetricRepository.findAllLatest();
        List<PrometheusDiskMetric> diskMetrics = diskMetricRepository.findAllLatest();

        return MetricsResponse.of(
                Instant.now(),
                cpuMetrics,
                memoryMetrics,
                networkMetrics,
                diskMetrics
        );
    }

    /**
     * 시간 범위 조회 (API용)
     */
    public MetricsApiResponse getMetricsByTimeRange(Instant start, Instant end) {
        log.debug("시간 범위 메트릭 조회: {} ~ {}", start, end);

        List<PrometheusCpuMetric> cpuMetrics = cpuMetricRepository.findByTimeBetween(start, end);
        List<PrometheusMemoryMetric> memoryMetrics = memoryMetricRepository.findByTimeBetween(start, end);
        List<PrometheusNetworkMetric> networkMetrics = networkMetricRepository.findByTimeBetween(start, end);
        List<PrometheusDiskMetric> diskMetrics = diskMetricRepository.findByTimeBetween(start, end);
        List<PrometheusTemperatureMetric> temperatureMetrics = temperatureMetricRepository.findByTimeBetween(start, end);

        String timeRange = start + " ~ " + end;

        return MetricsApiResponse.of(
                timeRange,
                cpuMetrics.stream().map(CpuMetricResponse::from).collect(Collectors.toList()),
                memoryMetrics.stream().map(MemoryMetricResponse::from).collect(Collectors.toList()),
                networkMetrics.stream().map(NetworkMetricResponse::from).collect(Collectors.toList()),
                diskMetrics.stream().map(DiskMetricResponse::from).collect(Collectors.toList()),
                temperatureMetrics.stream().map(TemperatureMetricResponse::from).collect(Collectors.toList())
        );
    }

    /**
     * CPU 메트릭만 조회
     */
    public List<CpuMetricResponse> getCpuMetrics(Instant start, Instant end) {
        return cpuMetricRepository.findByTimeBetween(start, end)
                .stream()
                .map(CpuMetricResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Memory 메트릭만 조회
     */
    public List<MemoryMetricResponse> getMemoryMetrics(Instant start, Instant end) {
        return memoryMetricRepository.findByTimeBetween(start, end)
                .stream()
                .map(MemoryMetricResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Network 메트릭만 조회
     */
    public List<NetworkMetricResponse> getNetworkMetrics(Instant start, Instant end) {
        return networkMetricRepository.findByTimeBetween(start, end)
                .stream()
                .map(NetworkMetricResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Disk 메트릭만 조회
     */
    public List<DiskMetricResponse> getDiskMetrics(Instant start, Instant end) {
        return diskMetricRepository.findByTimeBetween(start, end)
                .stream()
                .map(DiskMetricResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Temperature 메트릭만 조회
     */
    public List<TemperatureMetricResponse> getTemperatureMetrics(Instant start, Instant end) {
        return temperatureMetricRepository.findByTimeBetween(start, end)
                .stream()
                .map(TemperatureMetricResponse::from)
                .collect(Collectors.toList());
    }
}