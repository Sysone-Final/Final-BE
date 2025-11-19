package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prometheus 그래프 데이터 서비스
 * 대시보드 그래프용 데이터 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusGraphService {

    private final PrometheusCpuMetricRepository cpuRepo;
    private final PrometheusMemoryMetricRepository memoryRepo;
    private final PrometheusNetworkMetricRepository networkRepo;
    private final PrometheusDiskMetricRepository diskRepo;

    // ==================== CPU 섹션 ====================

    /**
     * CPU 섹션 전체 데이터
     */
    public CpuSectionDto getCpuSection(String instance, Instant start, Instant end) {
        log.info("CPU 섹션 데이터 조회 - instance: {}, 기간: {} ~ {}", instance, start, end);

        return CpuSectionDto.builder()
                .cpuUsage(getCpuUsageGraph(instance, start, end))
                .cpuModes(getCpuModesGraph(instance, start, end))
                .systemLoad(getSystemLoadGraph(instance, start, end))
                .contextSwitches(getContextSwitchesGraph(instance, start, end))
                .build();
    }

    /**
     * 1.1 CPU 사용률 시계열 그래프
     */
    private List<CpuUsagePointDto> getCpuUsageGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = cpuRepo.getCpuUsageTimeSeries(instance, start, end);

        return results.stream()
                .map(row -> CpuUsagePointDto.builder()
                        .timestamp(toLocalDateTime((Timestamp) row[0]))
                        .cpuUsagePercent(toDouble(row[1]))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 1.2 CPU 모드별 분포 (적층 영역 차트)
     */
    private List<CpuModePointDto> getCpuModesGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = cpuRepo.getCpuModeDistribution(instance, start, end);

        return results.stream()
                .map(row -> CpuModePointDto.builder()
                        .timestamp(toLocalDateTime((Timestamp) row[0]))
                        .userPercent(toDouble(row[1]))
                        .systemPercent(toDouble(row[2]))
                        .iowaitPercent(toDouble(row[3]))
                        .irqPercent(toDouble(row[4]))
                        .softirqPercent(toDouble(row[5]))
                        .idlePercent(toDouble(row[6]))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 1.3 시스템 부하 (라인 차트)
     */
    private List<SystemLoadPointDto> getSystemLoadGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = cpuRepo.getSystemLoad(instance, start, end);

        return results.stream()
                .map(row -> SystemLoadPointDto.builder()
                        .timestamp(toLocalDateTime((Timestamp) row[0]))
                        .load1(toDouble(row[1]))
                        .load5(toDouble(row[2]))
                        .load15(toDouble(row[3]))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 1.4 컨텍스트 스위치 (라인 차트)
     */
    private List<ContextSwitchPointDto> getContextSwitchesGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = cpuRepo.getContextSwitches(instance, start, end);

        return results.stream()
                .map(row -> ContextSwitchPointDto.builder()
                        .timestamp(toLocalDateTime((Timestamp) row[0]))
                        .contextSwitchesPerSec(toDouble(row[1]))
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 메모리 섹션 ====================

    /**
     * 메모리 섹션 전체 데이터
     */
    public MemorySectionDto getMemorySection(String instance, Instant start, Instant end) {
        log.info("메모리 섹션 데이터 조회 - instance: {}, 기간: {} ~ {}", instance, start, end);

        return MemorySectionDto.builder()
                .memoryUsage(getMemoryUsageGraph(instance, start, end))
                .memoryComposition(getMemoryCompositionGraph(instance, start, end))
                .swapUsage(getSwapUsageGraph(instance, start, end))
                .currentStats(getMemoryCurrentStats(instance))
                .build();
    }

    /**
     * 2.1 메모리 사용률 (게이지 + 시계열)
     */
    private List<MemoryUsagePointDto> getMemoryUsageGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = memoryRepo.getMemoryUsageTimeSeries(instance, start, end);

        return results.stream()
                .map(row -> MemoryUsagePointDto.builder()
                        .timestamp(toLocalDateTime((Timestamp) row[0]))
                        .usagePercent(toDouble(row[1]))
                        .usedBytes(toLong(row[2]))
                        .availableBytes(toLong(row[3]))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 2.2 메모리 구성 상세 (적층 바 차트)
     */
    private List<MemoryCompositionPointDto> getMemoryCompositionGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = memoryRepo.getMemoryComposition(instance, start, end);

        return results.stream()
                .map(row -> MemoryCompositionPointDto.builder()
                        .timestamp(toLocalDateTime((Timestamp) row[0]))
                        .activeBytes(toLong(row[1]))
                        .inactiveBytes(toLong(row[2]))
                        .buffersBytes(toLong(row[3]))
                        .cachedBytes(toLong(row[4]))
                        .freeBytes(toLong(row[5]))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 2.3 스왑 메모리 사용 (라인 차트)
     */
    private List<SwapUsagePointDto> getSwapUsageGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = memoryRepo.getSwapMemoryUsage(instance, start, end);

        return results.stream()
                .map(row -> SwapUsagePointDto.builder()
                        .timestamp(toLocalDateTime((Timestamp) row[0]))
                        .swapTotalBytes(toLong(row[1]))
                        .swapUsedBytes(toLong(row[2]))
                        .swapUsagePercent(toDouble(row[3]))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 메모리 현재 상태 (게이지용)
     */
    private MemoryCurrentStatsDto getMemoryCurrentStats(String instance) {
        List<Object[]> results = memoryRepo.getMemoryCurrentStats(instance);

        if (results.isEmpty()) {
            return MemoryCurrentStatsDto.builder().build();
        }

        Object[] row = results.get(0);
        return MemoryCurrentStatsDto.builder()
                .usagePercent(toDouble(row[0]))
                .usedBytes(toLong(row[1]))
                .totalBytes(toLong(row[2]))
                .availableBytes(toLong(row[3]))
                .swapUsagePercent(toDouble(row[4]))
                .build();
    }

    // ==================== 네트워크 섹션 ====================

    /**
     * 네트워크 섹션 전체 데이터
     */
    public NetworkSectionDto getNetworkSection(String instance, Instant start, Instant end) {
        log.info("네트워크 섹션 데이터 조회 - instance: {}, 기간: {} ~ {}", instance, start, end);

        return NetworkSectionDto.builder()
                .rxUsage(getNetworkRxUsageGraph(instance, start, end))
                .txUsage(getNetworkTxUsageGraph(instance, start, end))
                .packets(getNetworkPacketsGraph(instance, start, end))
                .bytes(getNetworkBytesGraph(instance, start, end))
                .bandwidth(getNetworkBandwidthGraph(instance, start, end))
                .errors(getNetworkErrorsGraph(instance, start, end))
                .interfaceStatus(getInterfaceStatusList(instance))
                .build();
    }

    /**
     * 3.1 네트워크 수신(RX) 사용률
     */
    private Map<String, List<NetworkUsagePointDto>> getNetworkRxUsageGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = networkRepo.getNetworkRxUsage(instance, start, end);

        Map<String, List<NetworkUsagePointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            NetworkUsagePointDto point = NetworkUsagePointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .usagePercent(toDouble(row[2]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 3.2 네트워크 송신(TX) 사용률
     */
    private Map<String, List<NetworkUsagePointDto>> getNetworkTxUsageGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = networkRepo.getNetworkTxUsage(instance, start, end);

        Map<String, List<NetworkUsagePointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            NetworkUsagePointDto point = NetworkUsagePointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .usagePercent(toDouble(row[2]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 3.3 & 3.4 네트워크 패킷 수 (누적 그래프)
     */
    private Map<String, List<NetworkPacketPointDto>> getNetworkPacketsGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = networkRepo.getNetworkPackets(instance, start, end);

        Map<String, List<NetworkPacketPointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            NetworkPacketPointDto point = NetworkPacketPointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .rxPacketsTotal(toLong(row[2]))
                    .txPacketsTotal(toLong(row[3]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 3.5 & 3.6 네트워크 바이트 (트래픽 그래프)
     */
    private Map<String, List<NetworkBytesPointDto>> getNetworkBytesGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = networkRepo.getNetworkBytes(instance, start, end);

        Map<String, List<NetworkBytesPointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            NetworkBytesPointDto point = NetworkBytesPointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .rxBytesTotal(toLong(row[2]))
                    .txBytesTotal(toLong(row[3]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 3.7 네트워크 대역폭 사용률 (양방향)
     */
    private Map<String, List<NetworkBandwidthPointDto>> getNetworkBandwidthGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = networkRepo.getNetworkBandwidth(instance, start, end);

        Map<String, List<NetworkBandwidthPointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            NetworkBandwidthPointDto point = NetworkBandwidthPointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .rxMbps(toDouble(row[2]))
                    .txMbps(toDouble(row[3]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 3.8 네트워크 에러 및 드롭 패킷
     */
    private Map<String, List<NetworkErrorPointDto>> getNetworkErrorsGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = networkRepo.getNetworkErrors(instance, start, end);

        Map<String, List<NetworkErrorPointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            NetworkErrorPointDto point = NetworkErrorPointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .rxErrors(toLong(row[2]))
                    .txErrors(toLong(row[3]))
                    .rxDropped(toLong(row[4]))
                    .txDropped(toLong(row[5]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 3.9 네트워크 인터페이스 상태 (상태 패널)
     */
    private List<InterfaceStatusDto> getInterfaceStatusList(String instance) {
        List<Object[]> results = networkRepo.getInterfaceStatus(instance);

        return results.stream()
                .map(row -> InterfaceStatusDto.builder()
                        .device((String) row[0])
                        .interfaceUp((Boolean) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 디스크 섹션 ====================

    /**
     * 디스크 섹션 전체 데이터
     */
    public DiskSectionDto getDiskSection(String instance, Instant start, Instant end) {
        log.info("디스크 섹션 데이터 조회 - instance: {}, 기간: {} ~ {}", instance, start, end);

        return DiskSectionDto.builder()
                .diskUsage(getDiskUsageGauges(instance))
                .ioUtilization(getDiskIoGraph(instance, start, end))
                .throughput(getDiskThroughputGraph(instance, start, end))
                .iops(getDiskIopsGraph(instance, start, end))
                .spaceTrend(getDiskSpaceTrendGraph(instance, start, end))
                .inodeUsage(getInodeUsageGauges(instance))
                .build();
    }

    /**
     * 4.1 디스크 사용률 (게이지 그리드)
     */
    private List<DiskUsageGaugeDto> getDiskUsageGauges(String instance) {
        List<Object[]> results = diskRepo.getDiskUsageCurrent(instance);

        return results.stream()
                .map(row -> DiskUsageGaugeDto.builder()
                        .device((String) row[0])
                        .mountpoint((String) row[1])
                        .usagePercent(toDouble(row[2]))
                        .totalBytes(toLong(row[3]))
                        .usedBytes(toLong(row[4]))
                        .freeBytes(toLong(row[5]))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 4.2 디스크 I/O 사용률 (라인 차트)
     */
    private Map<String, List<DiskIoPointDto>> getDiskIoGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = diskRepo.getDiskIoUtilization(instance, start, end);

        Map<String, List<DiskIoPointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            DiskIoPointDto point = DiskIoPointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .ioUtilizationPercent(toDouble(row[2]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 4.3 디스크 읽기/쓰기 속도 (영역 차트)
     */
    private Map<String, List<DiskThroughputPointDto>> getDiskThroughputGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = diskRepo.getDiskThroughput(instance, start, end);

        Map<String, List<DiskThroughputPointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            DiskThroughputPointDto point = DiskThroughputPointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .readBytesPerSec(toDouble(row[2]))
                    .writeBytesPerSec(toDouble(row[3]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 4.4 디스크 IOPS (라인 차트)
     */
    private Map<String, List<DiskIopsPointDto>> getDiskIopsGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = diskRepo.getDiskIops(instance, start, end);

        Map<String, List<DiskIopsPointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            DiskIopsPointDto point = DiskIopsPointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .readIops(toDouble(row[2]))
                    .writeIops(toDouble(row[3]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 4.5 디스크 공간 추이 (시계열 차트)
     */
    private Map<String, List<DiskSpacePointDto>> getDiskSpaceTrendGraph(String instance, Instant start, Instant end) {
        List<Object[]> results = diskRepo.getDiskSpaceTrend(instance, start, end);

        Map<String, List<DiskSpacePointDto>> deviceMap = new HashMap<>();

        for (Object[] row : results) {
            String device = (String) row[1];
            DiskSpacePointDto point = DiskSpacePointDto.builder()
                    .timestamp(toLocalDateTime((Timestamp) row[0]))
                    .freeBytes(toLong(row[2]))
                    .build();

            deviceMap.computeIfAbsent(device, k -> new ArrayList<>()).add(point);
        }

        return deviceMap;
    }

    /**
     * 4.6 inode 사용률 (게이지)
     */
    private List<InodeUsageGaugeDto> getInodeUsageGauges(String instance) {
        List<Object[]> results = diskRepo.getInodeUsageCurrent(instance);

        return results.stream()
                .map(row -> InodeUsageGaugeDto.builder()
                        .device((String) row[0])
                        .mountpoint((String) row[1])
                        .inodeUsagePercent(toDouble(row[2]))
                        .totalInodes(toLong(row[3]))
                        .usedInodes(toLong(row[4]))
                        .freeInodes(toLong(row[5]))
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 유틸리티 메서드 ====================

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}