package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.domain.*;
import org.example.finalbe.domains.prometheus.dto.cpu.*;
import org.example.finalbe.domains.prometheus.dto.disk.*;
import org.example.finalbe.domains.prometheus.dto.memory.*;
import org.example.finalbe.domains.prometheus.dto.network.*;
import org.example.finalbe.domains.prometheus.dto.serverroom.ServerRoomMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.temperature.*;
import org.example.finalbe.domains.prometheus.repository.realtime.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Prometheus 실시간 메트릭 서비스
 *
 * Phase 2에서 생성된 실시간 테이블(prometheus_xxx_realtime)에서 데이터를 조회하여
 * DTO로 변환하여 반환합니다.
 *
 * 주요 기능:
 * 1. 최신 메트릭 조회 (SSE용)
 * 2. 시간 범위 메트릭 조회 (HTTP API용)
 *
 * 사용하는 Repository:
 * - PrometheusCpuRealtimeRepository
 * - PrometheusMemoryRealtimeRepository
 * - PrometheusDiskRealtimeRepository
 * - PrometheusNetworkRealtimeRepository
 * - PrometheusTemperatureRealtimeRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusRealtimeMetricService {

    private final PrometheusCpuRealtimeRepository cpuRealtimeRepo;
    private final PrometheusMemoryRealtimeRepository memoryRealtimeRepo;
    private final PrometheusDiskRealtimeRepository diskRealtimeRepo;
    private final PrometheusNetworkRealtimeRepository networkRealtimeRepo;
    private final PrometheusTemperatureRealtimeRepository temperatureRealtimeRepo;

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 최신 메트릭 조회 (SSE용)
     * 각 테이블에서 가장 최근 1개의 레코드만 조회
     */
    public ServerRoomMetricsResponse getLatestMetrics() {
        log.debug("최신 메트릭 조회 시작");

        return new ServerRoomMetricsResponse(
                getLatestCpuMetrics(),
                getLatestMemoryMetrics(),
                getLatestNetworkMetrics(),
                getLatestDiskMetrics(),
                getLatestTemperatureMetrics()
        );
    }

    /**
     * 시간 범위 메트릭 조회 (HTTP API용)
     * 지정된 시간 범위 내의 모든 데이터를 조회
     *
     * @param startTime 시작 시간 (Instant)
     * @param endTime 종료 시간 (Instant)
     */
    public ServerRoomMetricsResponse getMetricsByTimeRange(Instant startTime, Instant endTime) {
        log.debug("시간 범위 메트릭 조회 - start: {}, end: {}", startTime, endTime);

        return new ServerRoomMetricsResponse(
                getCpuMetricsByTimeRange(startTime, endTime),
                getMemoryMetricsByTimeRange(startTime, endTime),
                getNetworkMetricsByTimeRange(startTime, endTime),
                getDiskMetricsByTimeRange(startTime, endTime),
                getTemperatureMetricsByTimeRange(startTime, endTime)
        );
    }

    // ==================== CPU 메트릭 ====================

    /**
     * 최신 CPU 메트릭 조회
     */
    private CpuMetricsResponse getLatestCpuMetrics() {
        try {
            PrometheusCpuRealtime latest = cpuRealtimeRepo.findLatest()
                    .orElse(null);

            if (latest == null) {
                return createEmptyCpuMetrics();
            }

            return new CpuMetricsResponse(
                    latest.getCpuUsagePercent(),
                    List.of(convertToCpuUsageResponse(latest)),
                    List.of(),  // 단일 포인트라 분포 없음
                    List.of(convertToLoadAverageResponse(latest)),
                    List.of(convertToContextSwitchResponse(latest))
            );

        } catch (Exception e) {
            log.error("최신 CPU 메트릭 조회 실패", e);
            return createEmptyCpuMetrics();
        }
    }

    /**
     * 시간 범위 CPU 메트릭 조회
     */
    private CpuMetricsResponse getCpuMetricsByTimeRange(Instant startTime, Instant endTime) {
        try {
            List<PrometheusCpuRealtime> metrics = cpuRealtimeRepo.findByTimeBetween(startTime, endTime);

            if (metrics.isEmpty()) {
                return createEmptyCpuMetrics();
            }

            PrometheusCpuRealtime latest = metrics.get(metrics.size() - 1);

            return new CpuMetricsResponse(
                    latest.getCpuUsagePercent(),
                    metrics.stream()
                            .map(this::convertToCpuUsageResponse)
                            .collect(Collectors.toList()),
                    List.of(),
                    metrics.stream()
                            .map(this::convertToLoadAverageResponse)
                            .collect(Collectors.toList()),
                    metrics.stream()
                            .map(this::convertToContextSwitchResponse)
                            .collect(Collectors.toList())
            );

        } catch (Exception e) {
            log.error("시간 범위 CPU 메트릭 조회 실패", e);
            return createEmptyCpuMetrics();
        }
    }

    // ==================== Memory 메트릭 ====================

    /**
     * 최신 Memory 메트릭 조회
     */
    private MemoryMetricsResponse getLatestMemoryMetrics() {
        try {
            PrometheusMemoryRealtime latest = memoryRealtimeRepo.findLatest()
                    .orElse(null);

            if (latest == null) {
                return createEmptyMemoryMetrics();
            }

            return new MemoryMetricsResponse(
                    latest.getUsagePercent(),                              // currentMemoryUsagePercent
                    List.of(convertToMemoryUsageResponse(latest)),         // memoryUsageTrend
                    List.of(convertToMemoryCompositionResponse(latest)),   // memoryComposition
                    List.of(convertToSwapUsageResponse(latest)),           // swapUsageTrend
                    List.of()                                              // topNMemoryUsage
            );

        } catch (Exception e) {
            log.error("최신 Memory 메트릭 조회 실패", e);
            return createEmptyMemoryMetrics();
        }
    }

    /**
     * 시간 범위 Memory 메트릭 조회
     */
    private MemoryMetricsResponse getMemoryMetricsByTimeRange(Instant startTime, Instant endTime) {
        try {
            List<PrometheusMemoryRealtime> metrics = memoryRealtimeRepo.findByTimeBetween(startTime, endTime);

            if (metrics.isEmpty()) {
                return createEmptyMemoryMetrics();
            }

            PrometheusMemoryRealtime latest = metrics.get(metrics.size() - 1);

            return new MemoryMetricsResponse(
                    latest.getUsagePercent(),                              // currentMemoryUsagePercent
                    metrics.stream()                                        // memoryUsageTrend
                            .map(this::convertToMemoryUsageResponse)
                            .collect(Collectors.toList()),
                    metrics.stream()                                        // memoryComposition
                            .map(this::convertToMemoryCompositionResponse)
                            .collect(Collectors.toList()),
                    metrics.stream()                                        // swapUsageTrend
                            .map(this::convertToSwapUsageResponse)
                            .collect(Collectors.toList()),
                    List.of()                                              // topNMemoryUsage (5번째 파라미터)
            );

        } catch (Exception e) {
            log.error("시간 범위 Memory 메트릭 조회 실패", e);
            return createEmptyMemoryMetrics();
        }
    }

    // ==================== Disk 메트릭 ====================

    /**
     * 최신 Disk 메트릭 조회
     */
    private DiskMetricsResponse getLatestDiskMetrics() {
        try {
            PrometheusDiskRealtime latest = diskRealtimeRepo.findLatest()
                    .orElse(null);

            if (latest == null) {
                return createEmptyDiskMetrics();
            }

            return new DiskMetricsResponse(
                    latest.getUsagePercent(),
                    List.of(convertToDiskUsageResponse(latest)),
                    List.of(convertToDiskIoResponse(latest)),
                    List.of(),  // 예측은 여러 포인트 필요
                    List.of(convertToInodeUsageResponse(latest))
            );

        } catch (Exception e) {
            log.error("최신 Disk 메트릭 조회 실패", e);
            return createEmptyDiskMetrics();
        }
    }

    /**
     * 시간 범위 Disk 메트릭 조회
     */
    private DiskMetricsResponse getDiskMetricsByTimeRange(Instant startTime, Instant endTime) {
        try {
            List<PrometheusDiskRealtime> metrics = diskRealtimeRepo.findByTimeBetween(startTime, endTime);

            if (metrics.isEmpty()) {
                return createEmptyDiskMetrics();
            }

            PrometheusDiskRealtime latest = metrics.get(metrics.size() - 1);

            return new DiskMetricsResponse(
                    latest.getUsagePercent(),
                    metrics.stream()
                            .map(this::convertToDiskUsageResponse)
                            .collect(Collectors.toList()),
                    metrics.stream()
                            .map(this::convertToDiskIoResponse)
                            .collect(Collectors.toList()),
                    List.of(),
                    metrics.stream()
                            .map(this::convertToInodeUsageResponse)
                            .collect(Collectors.toList())
            );

        } catch (Exception e) {
            log.error("시간 범위 Disk 메트릭 조회 실패", e);
            return createEmptyDiskMetrics();
        }
    }

    // ==================== Network 메트릭 ====================

    /**
     * 최신 Network 메트릭 조회
     */
    private NetworkMetricsResponse getLatestNetworkMetrics() {
        try {
            PrometheusNetworkRealtime latest = networkRealtimeRepo.findLatest()
                    .orElse(null);

            if (latest == null) {
                return createEmptyNetworkMetrics();
            }

            return new NetworkMetricsResponse(
                    latest.getRxBps(),                                      // currentRxBytesPerSec
                    latest.getTxBps(),                                      // currentTxBytesPerSec
                    List.of(),                                              // networkUsageTrend (3번째 파라미터 - 비어있음)
                    List.of(convertToNetworkPacketsResponse(latest)),       // networkPacketsTrend
                    List.of(convertToNetworkBytesResponse(latest)),         // networkBytesTrend
                    List.of(convertToNetworkErrorsResponse(latest)),        // networkErrorsTrend
                    List.of()                                               // interfaceStatus (7번째 파라미터)
            );

        } catch (Exception e) {
            log.error("최신 Network 메트릭 조회 실패", e);
            return createEmptyNetworkMetrics();
        }
    }

    /**
     * 시간 범위 Network 메트릭 조회
     */
    private NetworkMetricsResponse getNetworkMetricsByTimeRange(Instant startTime, Instant endTime) {
        try {
            List<PrometheusNetworkRealtime> metrics = networkRealtimeRepo.findByTimeBetween(startTime, endTime);

            if (metrics.isEmpty()) {
                return createEmptyNetworkMetrics();
            }

            PrometheusNetworkRealtime latest = metrics.get(metrics.size() - 1);

            return new NetworkMetricsResponse(
                    latest.getRxBps(),                                      // currentRxBytesPerSec
                    latest.getTxBps(),                                      // currentTxBytesPerSec
                    List.of(),                                              // networkUsageTrend (3번째 파라미터)
                    metrics.stream()                                        // networkPacketsTrend
                            .map(this::convertToNetworkPacketsResponse)
                            .collect(Collectors.toList()),
                    metrics.stream()                                        // networkBytesTrend
                            .map(this::convertToNetworkBytesResponse)
                            .collect(Collectors.toList()),
                    metrics.stream()                                        // networkErrorsTrend
                            .map(this::convertToNetworkErrorsResponse)
                            .collect(Collectors.toList()),
                    List.of()                                               // interfaceStatus (7번째 파라미터)
            );

        } catch (Exception e) {
            log.error("시간 범위 Network 메트릭 조회 실패", e);
            return createEmptyNetworkMetrics();
        }
    }

    // ==================== Temperature 메트릭 ====================

    /**
     * 최신 Temperature 메트릭 조회
     */
    private TemperatureMetricsResponse getLatestTemperatureMetrics() {
        try {
            PrometheusTemperatureRealtime latest = temperatureRealtimeRepo.findLatest()
                    .orElse(null);

            if (latest == null) {
                return createEmptyTemperatureMetrics();
            }

            return new TemperatureMetricsResponse(
                    latest.getCelsius(),
                    List.of(convertToTemperatureResponse(latest))
            );

        } catch (Exception e) {
            log.error("최신 Temperature 메트릭 조회 실패", e);
            return createEmptyTemperatureMetrics();
        }
    }

    /**
     * 시간 범위 Temperature 메트릭 조회
     */
    private TemperatureMetricsResponse getTemperatureMetricsByTimeRange(Instant startTime, Instant endTime) {
        try {
            List<PrometheusTemperatureRealtime> metrics = temperatureRealtimeRepo.findByTimeBetween(startTime, endTime);

            if (metrics.isEmpty()) {
                return createEmptyTemperatureMetrics();
            }

            PrometheusTemperatureRealtime latest = metrics.get(metrics.size() - 1);

            return new TemperatureMetricsResponse(
                    latest.getCelsius(),
                    metrics.stream()
                            .map(this::convertToTemperatureResponse)
                            .collect(Collectors.toList())
            );

        } catch (Exception e) {
            log.error("시간 범위 Temperature 메트릭 조회 실패", e);
            return createEmptyTemperatureMetrics();
        }
    }

    // ==================== 변환 메서드 (Entity → DTO) ====================

    private CpuUsageResponse convertToCpuUsageResponse(PrometheusCpuRealtime entity) {
        return new CpuUsageResponse(
                entity.getTime().atZone(KST_ZONE),
                entity.getCpuUsagePercent()
        );
    }

    private LoadAverageResponse convertToLoadAverageResponse(PrometheusCpuRealtime entity) {
        return new LoadAverageResponse(
                entity.getTime().atZone(KST_ZONE),
                entity.getLoadAvg1(),
                entity.getLoadAvg5(),
                entity.getLoadAvg15()
        );
    }

    private ContextSwitchResponse convertToContextSwitchResponse(PrometheusCpuRealtime entity) {
        return new ContextSwitchResponse(
                entity.getTime().atZone(KST_ZONE),
                entity.getContextSwitches() != null ? entity.getContextSwitches().doubleValue() : 0.0
        );
    }

    private MemoryUsageResponse convertToMemoryUsageResponse(PrometheusMemoryRealtime entity) {
        Double total = entity.getTotalBytes() != null ? entity.getTotalBytes().doubleValue() : 0.0;
        Double available = entity.getAvailableBytes() != null ? entity.getAvailableBytes().doubleValue() : 0.0;

        return new MemoryUsageResponse(
                entity.getTime().atZone(KST_ZONE),          // time
                total,                                       // totalMemory
                available,                                   // availableMemory
                total - available,                           // usedMemory (5번째 파라미터 추가!)
                entity.getUsagePercent()                     // memoryUsagePercent
        );
    }

    private MemoryCompositionResponse convertToMemoryCompositionResponse(PrometheusMemoryRealtime entity) {
        return new MemoryCompositionResponse(
                entity.getTime().atZone(KST_ZONE),
                entity.getActiveBytes() != null ? entity.getActiveBytes().doubleValue() : 0.0,
                entity.getInactiveBytes() != null ? entity.getInactiveBytes().doubleValue() : 0.0,
                entity.getBuffersBytes() != null ? entity.getBuffersBytes().doubleValue() : 0.0,
                entity.getCachedBytes() != null ? entity.getCachedBytes().doubleValue() : 0.0,
                entity.getFreeBytes() != null ? entity.getFreeBytes().doubleValue() : 0.0
        );
    }


    private SwapUsageResponse convertToSwapUsageResponse(PrometheusMemoryRealtime entity) {
        Double total = entity.getSwapTotalBytes() != null ? entity.getSwapTotalBytes().doubleValue() : 0.0;
        Double free = entity.getSwapFreeBytes() != null ? entity.getSwapFreeBytes().doubleValue() : 0.0;

        return new SwapUsageResponse(
                entity.getTime().atZone(KST_ZONE),          // time
                total,                                       // totalSwap
                free,                                        // freeSwap
                total - free,                                // usedSwap (5번째 파라미터 추가!)
                entity.getSwapUsagePercent()                 // swapUsagePercent
        );
    }

    private DiskUsageResponse convertToDiskUsageResponse(PrometheusDiskRealtime entity) {
        Double total = entity.getTotalBytes() != null ? entity.getTotalBytes().doubleValue() : 0.0;
        Double free = entity.getFreeBytes() != null ? entity.getFreeBytes().doubleValue() : 0.0;

        return new DiskUsageResponse(
                entity.getTime().atZone(KST_ZONE),          // time
                total,                                       // totalBytes
                free,                                        // freeBytes
                total - free,                                // usedBytes (5번째 파라미터 추가!)
                entity.getUsagePercent()                     // usagePercent
        );
    }

    private DiskIoResponse convertToDiskIoResponse(PrometheusDiskRealtime entity) {
        return new DiskIoResponse(
                entity.getTime().atZone(KST_ZONE),
                entity.getReadBytesPerSec(),
                entity.getWriteBytesPerSec(),
                entity.getReadIops(),
                entity.getWriteIops(),
                entity.getIoUtilizationPercent()
        );
    }

    private InodeUsageResponse convertToInodeUsageResponse(PrometheusDiskRealtime entity) {
        Double total = entity.getTotalInodes() != null ? entity.getTotalInodes().doubleValue() : 0.0;
        Double free = entity.getFreeInodes() != null ? entity.getFreeInodes().doubleValue() : 0.0;

        return new InodeUsageResponse(
                entity.getDeviceId(),                        // deviceId (Integer)
                entity.getMountpointId(),                    // mountpointId (Integer)
                total,                                       // totalInodes
                free,                                        // freeInodes
                total - free,                                // usedInodes (6번째 파라미터 추가!)
                entity.getInodeUsagePercent()                // inodeUsagePercent
        );
    }

    private NetworkErrorsResponse convertToNetworkErrorsResponse(PrometheusNetworkRealtime entity) {
        return new NetworkErrorsResponse(
                entity.getTime().atZone(KST_ZONE),
                entity.getRxErrorsTotal() != null ? entity.getRxErrorsTotal().doubleValue() : 0.0,
                entity.getTxErrorsTotal() != null ? entity.getTxErrorsTotal().doubleValue() : 0.0,
                entity.getRxDroppedTotal() != null ? entity.getRxDroppedTotal().doubleValue() : 0.0,
                entity.getTxDroppedTotal() != null ? entity.getTxDroppedTotal().doubleValue() : 0.0
        );
    }

    private NetworkPacketsResponse convertToNetworkPacketsResponse(PrometheusNetworkRealtime entity) {
        return new NetworkPacketsResponse(
                entity.getTime().atZone(KST_ZONE),
                entity.getRxPacketsTotal() != null ? entity.getRxPacketsTotal().doubleValue() : 0.0,
                entity.getTxPacketsTotal() != null ? entity.getTxPacketsTotal().doubleValue() : 0.0
        );
    }

    private NetworkBytesResponse convertToNetworkBytesResponse(PrometheusNetworkRealtime entity) {
        return new NetworkBytesResponse(
                entity.getTime().atZone(KST_ZONE),
                entity.getRxBytesTotal() != null ? entity.getRxBytesTotal().doubleValue() : 0.0,
                entity.getTxBytesTotal() != null ? entity.getTxBytesTotal().doubleValue() : 0.0
        );
    }

    private TemperatureResponse convertToTemperatureResponse(PrometheusTemperatureRealtime entity) {
        Double celsius = entity.getCelsius() != null ? entity.getCelsius() : 0.0;

        return new TemperatureResponse(
                entity.getTime().atZone(KST_ZONE),          // time
                celsius,                                     // avgTemperature
                celsius,                                     // maxTemperature (동일값)
                celsius                                      // minTemperature (동일값, 4번째 파라미터 추가!)
        );
    }

    // ==================== Empty Response 생성 ====================

    private CpuMetricsResponse createEmptyCpuMetrics() {
        return new CpuMetricsResponse(
                0.0,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private MemoryMetricsResponse createEmptyMemoryMetrics() {
        return new MemoryMetricsResponse(
                0.0,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private DiskMetricsResponse createEmptyDiskMetrics() {
        return new DiskMetricsResponse(
                0.0,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }


    private NetworkMetricsResponse createEmptyNetworkMetrics() {
        return new NetworkMetricsResponse(
                0.0,        // currentRxBytesPerSec
                0.0,        // currentTxBytesPerSec
                List.of(),  // networkUsageTrend
                List.of(),  // networkPacketsTrend
                List.of(),  // networkBytesTrend
                List.of(),  // networkErrorsTrend
                List.of()   // interfaceStatus
        );
    }

    private TemperatureMetricsResponse createEmptyTemperatureMetrics() {
        return new TemperatureMetricsResponse(
                0.0,
                List.of()
        );
    }
}