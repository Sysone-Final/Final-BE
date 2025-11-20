package org.example.finalbe.domains.monitoring.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerRoomDataSimulator {

    private final SystemMetricRepository systemMetricRepository;
    private final DiskMetricRepository diskMetricRepository;
    private final NetworkMetricRepository networkMetricRepository;
    private final EnvironmentMetricRepository environmentMetricRepository;
    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;

    private final JdbcTemplate jdbcTemplate;

    private final SseService sseService;

    private final MonitoringMetricCache monitoringMetricCache;

    private static final Map<Long, List<String>> EQUIPMENT_NICS = new HashMap<>();

    private final Map<Long, AnomalyState> anomalyStates = new HashMap<>();
    private final Map<Long, AnomalyState> rackAnomalyStates = new HashMap<>();
    private final Random random = new Random();

    // 누적 카운터
    private final Map<String, Long> cumulativeInPackets = new HashMap<>();
    private final Map<String, Long> cumulativeOutPackets = new HashMap<>();
    private final Map<String, Long> cumulativeInBytes = new HashMap<>();
    private final Map<String, Long> cumulativeOutBytes = new HashMap<>();
    private final Map<String, Long> cumulativeInErrors = new HashMap<>();
    private final Map<String, Long> cumulativeOutErrors = new HashMap<>();
    private final Map<String, Long> cumulativeInDiscards = new HashMap<>();
    private final Map<String, Long> cumulativeOutDiscards = new HashMap<>();
    private final Map<String, Long> cumulativeContextSwitches = new HashMap<>();
    private final Map<String, Long> cumulativeIoReads = new HashMap<>();
    private final Map<String, Long> cumulativeIoWrites = new HashMap<>();

    // 환경 메트릭 추적용 (랙별 최저/최고값 계산)
    private final Map<Long, Double> minTemperatureTracker = new HashMap<>();
    private final Map<Long, Double> maxTemperatureTracker = new HashMap<>();
    private final Map<Long, Double> minHumidityTracker = new HashMap<>();
    private final Map<Long, Double> maxHumidityTracker = new HashMap<>();

    // DB에서 조회한 장비/랙 목록 캐시
    private List<Equipment> activeEquipments = new CopyOnWriteArrayList<>();
    private List<Rack> activeRacks = new ArrayList<>();




    @PostConstruct
    public void init() {
        log.info("🚀 서버실 데이터 시뮬레이터 초기화 시작...");


        activeEquipments = new CopyOnWriteArrayList<>(equipmentRepository.findAll());
        activeRacks = new CopyOnWriteArrayList<>(rackRepository.findAll());

        if (activeEquipments.isEmpty()) {
            log.warn("⚠️  등록된 장비가 없습니다. 시뮬레이터가 동작하지 않습니다.");
            return;
        }

        // 각 장비별 NIC 구성 초기화
        for (Equipment equipment : activeEquipments) {
            Long equipmentId = equipment.getId();
            EquipmentType type = equipment.getType();

            // NIC 설정 (SERVER, SWITCH, ROUTER, FIREWALL, LOAD_BALANCER)
            if (hasNetworkMetric(type)) {
                EQUIPMENT_NICS.put(equipmentId, generateDefaultNics(type));
            }

            // 이상 징후 상태 초기화
            anomalyStates.put(equipmentId, new AnomalyState());
        }

        // 각 랙별 환경 메트릭 초기화
        for (Rack rack : activeRacks) {
            Long rackId = rack.getId();
            rackAnomalyStates.put(rackId, new AnomalyState());

            minTemperatureTracker.put(rackId, 22.0);
            maxTemperatureTracker.put(rackId, 22.0);
            minHumidityTracker.put(rackId, 45.0);
            maxHumidityTracker.put(rackId, 45.0);
        }

        log.info("✅ 초기화 완료! {}개 장비 + {}개 랙 모니터링 시작", activeEquipments.size(), activeRacks.size());

        // 장비 타입별 통계
//        Map<EquipmentType, Long> typeCounts = activeEquipments.stream()
//                .collect(java.util.stream.Collectors.groupingBy(Equipment::getType, java.util.stream.Collectors.counting()));
//
//        log.info("📊 장비 타입별 수량:");
//        typeCounts.forEach((type, count) -> {
//            log.info("   - {}: {}개 (System:{}, Disk:{}, Network:{})",
//                    type, count,
//                    hasSystemMetric(type) ? "✅" : "❌",
//                    hasDiskMetric(type) ? "✅" : "❌",
//                    hasNetworkMetric(type) ? "✅" : "❌"
//            );
//        });
    }

    /**
     * 장비 타입별 기본 NIC 생성
     */
    private List<String> generateDefaultNics(EquipmentType type) {
        switch (type) {
            case SERVER:
                return Arrays.asList("eth0", "eth1");
            case SWITCH:
                return Arrays.asList("GigabitEthernet1/0/1", "GigabitEthernet1/0/2", "GigabitEthernet1/0/3", "GigabitEthernet1/0/4");
            case ROUTER:
                return Arrays.asList("GigabitEthernet0/0", "GigabitEthernet0/1", "GigabitEthernet0/2");
            case FIREWALL:
                return Arrays.asList("port1", "port2", "port3", "port4");
            case LOAD_BALANCER:
                return Arrays.asList("nic1", "nic2");
            default:
                return Arrays.asList("eth0");
        }
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 2000)
    @Transactional
    public void generateRealtimeMetrics() {
        if (activeEquipments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        long startTime = System.currentTimeMillis(); // 실행 시간 측정 시작

        // 1. 저장할 데이터들을 모아둘 리스트 생성
        List<SystemMetric> systemMetricsToSave = new ArrayList<>();
        List<DiskMetric> diskMetricsToSave = new ArrayList<>();
        List<NetworkMetric> networkMetricsToSave = new ArrayList<>();
        List<EnvironmentMetric> environmentMetricsToSave = new ArrayList<>();

        try {
            // 1. 장비별 메트릭 생성 (DB 저장 X, 리스트 추가 O)
            for (Equipment equipment : activeEquipments) {
                Long equipmentId = equipment.getId();
                EquipmentType type = equipment.getType();

                // System 메트릭
                if (hasSystemMetric(type)) {
                    SystemMetric sysMetric = generateSystemMetric(equipmentId, now);
                    systemMetricsToSave.add(sysMetric);
                    monitoringMetricCache.updateSystemMetric(sysMetric);
                    sseService.sendToEquipment(equipmentId, "system", sysMetric); // SSE는 바로 전송 (빠름)
                }

                // Disk 메트릭
                if (hasDiskMetric(type)) {
                    DiskMetric diskMetric = generateDiskMetric(equipmentId, now);
                    diskMetricsToSave.add(diskMetric);
                    monitoringMetricCache.updateDiskMetric(diskMetric);
                    sseService.sendToEquipment(equipmentId, "disk", diskMetric);
                }

                // Network 메트릭
                if (hasNetworkMetric(type)) {
                    List<String> nics = EQUIPMENT_NICS.get(equipmentId);
                    if (nics != null) {
                        for (String nic : nics) {
                            NetworkMetric nicMetric = generateNetworkMetric(equipmentId, nic, now);
                            networkMetricsToSave.add(nicMetric);
                            monitoringMetricCache.updateNetworkMetric(nicMetric);
                            sseService.sendToEquipment(equipmentId, "network", nicMetric);
                        }
                    }
                }
            }

            // 2. 랙별 환경 메트릭 생성
            for (Rack rack : activeRacks) {
                Long rackId = rack.getId();
                EnvironmentMetric envMetric = generateEnvironmentMetric(rackId, now);
                environmentMetricsToSave.add(envMetric);
                monitoringMetricCache.updateEnvironmentMetric(envMetric);
                sseService.sendToRack(rackId, "environment", envMetric);
            }

            // 3.  DB에 한 번에 저장 (Batch Insert)
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                long dbStart = System.currentTimeMillis();
                if (!systemMetricsToSave.isEmpty()) batchInsertSystemMetrics(systemMetricsToSave);
                if (!diskMetricsToSave.isEmpty()) batchInsertDiskMetrics(diskMetricsToSave);
                if (!networkMetricsToSave.isEmpty()) batchInsertNetworkMetrics(networkMetricsToSave);
                if (!environmentMetricsToSave.isEmpty()) batchInsertEnvironmentMetrics(environmentMetricsToSave);

                long dbDuration = System.currentTimeMillis() - dbStart;
                log.info("💾 DB 저장 완료 (백그라운드): {}ms 소요", dbDuration);
            });

            maybeUpdateAnomalies();

            long duration = System.currentTimeMillis() - startTime;
            log.info("🚀 SSE 전송 완료 & DB 작업 할당 끝: {}ms 소요", duration);
        } catch (Exception e) {
            log.error("❌ 메트릭 생성 중 오류 발생", e);
        }
    }
    private void batchInsertSystemMetrics(List<SystemMetric> metrics) {
        String sql = "INSERT INTO system_metrics (equipment_id, generate_time, " +
                "cpu_idle, cpu_user, cpu_system, cpu_wait, cpu_nice, cpu_irq, cpu_softirq, cpu_steal, " +
                "load_avg1, load_avg5, load_avg15, context_switches, " +
                "total_memory, used_memory, free_memory, used_memory_percentage, " +
                "memory_buffers, memory_cached, memory_active, memory_inactive, " +
                "total_swap, used_swap, used_swap_percentage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, metrics, metrics.size(),
                (ps, metric) -> {
                    ps.setLong(1, metric.getEquipmentId());
                    ps.setObject(2, metric.getGenerateTime());
                    ps.setObject(3, metric.getCpuIdle());
                    ps.setObject(4, metric.getCpuUser());
                    ps.setObject(5, metric.getCpuSystem());
                    ps.setObject(6, metric.getCpuWait());
                    ps.setObject(7, metric.getCpuNice());
                    ps.setObject(8, metric.getCpuIrq());
                    ps.setObject(9, metric.getCpuSoftirq());
                    ps.setObject(10, metric.getCpuSteal());
                    ps.setObject(11, metric.getLoadAvg1());
                    ps.setObject(12, metric.getLoadAvg5());
                    ps.setObject(13, metric.getLoadAvg15());
                    ps.setObject(14, metric.getContextSwitches());
                    ps.setObject(15, metric.getTotalMemory());
                    ps.setObject(16, metric.getUsedMemory());
                    ps.setObject(17, metric.getFreeMemory());
                    ps.setObject(18, metric.getUsedMemoryPercentage());
                    ps.setObject(19, metric.getMemoryBuffers());
                    ps.setObject(20, metric.getMemoryCached());
                    ps.setObject(21, metric.getMemoryActive());
                    ps.setObject(22, metric.getMemoryInactive());
                    ps.setObject(23, metric.getTotalSwap());
                    ps.setObject(24, metric.getUsedSwap());
                    ps.setObject(25, metric.getUsedSwapPercentage());
                });
    }

    private void batchInsertDiskMetrics(List<DiskMetric> metrics) {
        String sql = "INSERT INTO disk_metrics (equipment_id, generate_time, " +
                "total_bytes, used_bytes, free_bytes, used_percentage, " +
                "io_read_bps, io_write_bps, io_time_percentage, io_read_count, io_write_count, " +
                "total_inodes, used_inodes, free_inodes, used_inode_percentage) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, metrics, metrics.size(),
                (ps, metric) -> {
                    ps.setLong(1, metric.getEquipmentId());
                    ps.setObject(2, metric.getGenerateTime());
                    ps.setObject(3, metric.getTotalBytes());
                    ps.setObject(4, metric.getUsedBytes());
                    ps.setObject(5, metric.getFreeBytes());
                    ps.setObject(6, metric.getUsedPercentage());
                    ps.setObject(7, metric.getIoReadBps());
                    ps.setObject(8, metric.getIoWriteBps());
                    ps.setObject(9, metric.getIoTimePercentage());
                    ps.setObject(10, metric.getIoReadCount());
                    ps.setObject(11, metric.getIoWriteCount());
                    ps.setObject(12, metric.getTotalInodes());
                    ps.setObject(13, metric.getUsedInodes());
                    ps.setObject(14, metric.getFreeInodes());
                    ps.setObject(15, metric.getUsedInodePercentage());
                });
    }

    private void batchInsertNetworkMetrics(List<NetworkMetric> metrics) {
        String sql = "INSERT INTO network_metrics (equipment_id, nic_name, generate_time, " +
                "rx_usage, tx_usage, in_pkts_tot, out_pkts_tot, in_bytes_tot, out_bytes_tot, " +
                "in_bytes_per_sec, out_bytes_per_sec, in_pkts_per_sec, out_pkts_per_sec, " +
                "in_error_pkts_tot, out_error_pkts_tot, in_discard_pkts_tot, out_discard_pkts_tot, oper_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, metrics, metrics.size(),
                (ps, metric) -> {
                    ps.setLong(1, metric.getEquipmentId());
                    ps.setString(2, metric.getNicName());
                    ps.setObject(3, metric.getGenerateTime());
                    ps.setObject(4, metric.getRxUsage());
                    ps.setObject(5, metric.getTxUsage());
                    ps.setObject(6, metric.getInPktsTot());
                    ps.setObject(7, metric.getOutPktsTot());
                    ps.setObject(8, metric.getInBytesTot());
                    ps.setObject(9, metric.getOutBytesTot());
                    ps.setObject(10, metric.getInBytesPerSec());
                    ps.setObject(11, metric.getOutBytesPerSec());
                    ps.setObject(12, metric.getInPktsPerSec());
                    ps.setObject(13, metric.getOutPktsPerSec());
                    ps.setObject(14, metric.getInErrorPktsTot());
                    ps.setObject(15, metric.getOutErrorPktsTot());
                    ps.setObject(16, metric.getInDiscardPktsTot());
                    ps.setObject(17, metric.getOutDiscardPktsTot());
                    ps.setObject(18, metric.getOperStatus());
                });
    }

    private void batchInsertEnvironmentMetrics(List<EnvironmentMetric> metrics) {
        String sql = "INSERT INTO environment_metrics (rack_id, generate_time, " +
                "temperature, min_temperature, max_temperature, " +
                "humidity, min_humidity, max_humidity, " +
                "temperature_warning, humidity_warning) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, metrics, metrics.size(),
                (ps, metric) -> {
                    ps.setLong(1, metric.getRackId());
                    ps.setObject(2, metric.getGenerateTime());
                    ps.setObject(3, metric.getTemperature());
                    ps.setObject(4, metric.getMinTemperature());
                    ps.setObject(5, metric.getMaxTemperature());
                    ps.setObject(6, metric.getHumidity());
                    ps.setObject(7, metric.getMinHumidity());
                    ps.setObject(8, metric.getMaxHumidity());
                    ps.setObject(9, metric.getTemperatureWarning());
                    ps.setObject(10, metric.getHumidityWarning());
                });
    }

    /**
     * 장비 타입별 메트릭 수집 가능 여부
     */
    private boolean hasSystemMetric(EquipmentType type) {
        return type == EquipmentType.SERVER || type == EquipmentType.STORAGE;
    }

    private boolean hasDiskMetric(EquipmentType type) {
        return type == EquipmentType.SERVER || type == EquipmentType.STORAGE;
    }

    private boolean hasNetworkMetric(EquipmentType type) {
        return type == EquipmentType.SERVER ||
                type == EquipmentType.SWITCH ||
                type == EquipmentType.ROUTER ||
                type == EquipmentType.FIREWALL ||
                type == EquipmentType.LOAD_BALANCER;
    }

    /**
     * 시스템 메트릭 생성 - 모든 그래프 지원
     */
    private SystemMetric generateSystemMetric(Long equipmentId, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        SystemMetric metric = SystemMetric.builder()
                .equipmentId(equipmentId)
                .generateTime(time)
                .build();

        // ===== CPU 메트릭 (그래프 1.1, 1.2) =====
        double baseCpu = 15 + rand.nextDouble() * 20;
        double cpuUsage = state.hasCpuAnomaly ?
                Math.min(95, baseCpu + 50 + rand.nextDouble() * 20) : baseCpu;

        metric.setCpuIdle(100 - cpuUsage);
        metric.setCpuUser(cpuUsage * 0.55);
        metric.setCpuSystem(cpuUsage * 0.20);
        metric.setCpuWait(cpuUsage * 0.10);
        metric.setCpuNice(cpuUsage * 0.02);
        metric.setCpuIrq(cpuUsage * 0.05);
        metric.setCpuSoftirq(cpuUsage * 0.05);
        metric.setCpuSteal(cpuUsage * 0.03);

        // ===== 시스템 부하 (그래프 1.3) =====
        double baseLoad = cpuUsage / 25.0;
        metric.setLoadAvg1(baseLoad + rand.nextDouble() * 0.5);
        metric.setLoadAvg5(baseLoad * 0.9 + rand.nextDouble() * 0.3);
        metric.setLoadAvg15(baseLoad * 0.8 + rand.nextDouble() * 0.2);

        // ===== 컨텍스트 스위치 (그래프 1.4) =====
        String contextKey = "context_" + equipmentId;
        long prevContext = cumulativeContextSwitches.getOrDefault(contextKey, 0L);
        long contextInc = (long)(cpuUsage * 100 + rand.nextInt(15000));
        long newContext = prevContext + contextInc;
        cumulativeContextSwitches.put(contextKey, newContext);
        metric.setContextSwitches(newContext);

        // ===== 메모리 메트릭 (그래프 2.1, 2.2) =====
        long totalMemory = 16L * 1024 * 1024 * 1024;  // 16GB
        double baseMemUsage = 40 + rand.nextDouble() * 20;
        double memUsagePercent = state.hasMemoryAnomaly ?
                Math.min(95, baseMemUsage + 30 + rand.nextDouble() * 15) : baseMemUsage;

        long usedMemory = (long)(totalMemory * memUsagePercent / 100);
        long freeMemory = totalMemory - usedMemory;

        metric.setTotalMemory(totalMemory);
        metric.setUsedMemory(usedMemory);
        metric.setFreeMemory(freeMemory);
        metric.setUsedMemoryPercentage(memUsagePercent);

        // 메모리 구성 상세
        metric.setMemoryActive(usedMemory / 2);
        metric.setMemoryInactive(usedMemory / 4);
        metric.setMemoryBuffers(usedMemory / 10);
        metric.setMemoryCached(usedMemory / 5);

        // ===== SWAP 메트릭 (그래프 2.3) =====
        long totalSwap = 8L * 1024 * 1024 * 1024;  // 8GB
        double swapUsagePercent = memUsagePercent > 85 ?
                rand.nextDouble() * 50 : rand.nextDouble() * 5;

        long usedSwap = (long)(totalSwap * swapUsagePercent / 100);

        metric.setTotalSwap(totalSwap);
        metric.setUsedSwap(usedSwap);
        metric.setUsedSwapPercentage(swapUsagePercent);

        return metric;
    }

    /**
     * 디스크 메트릭 생성 - 모든 그래프 지원
     */
    private DiskMetric generateDiskMetric(Long equipmentId, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        DiskMetric metric = DiskMetric.builder()
                .equipmentId(equipmentId)
                .generateTime(time)
                .build();

        // ===== 디스크 용량 (그래프 4.1, 4.5) =====
        long totalBytes = 500L * 1024 * 1024 * 1024;  // 500GB
        double baseUsage = 30 + rand.nextDouble() * 40;
        double usedPercent = Math.min(95, baseUsage);

        long usedBytes = (long)(totalBytes * usedPercent / 100);
        long freeBytes = totalBytes - usedBytes;

        metric.setTotalBytes(totalBytes);
        metric.setUsedBytes(usedBytes);
        metric.setFreeBytes(freeBytes);
        metric.setUsedPercentage(usedPercent);

        // ===== 디스크 I/O (그래프 4.2, 4.3, 4.4) =====
        double baseReadBps = 5_000_000 + rand.nextDouble() * 10_000_000;  // 5~15 MB/s
        double baseWriteBps = 3_000_000 + rand.nextDouble() * 7_000_000;  // 3~10 MB/s

        double ioReadBps = state.hasDiskAnomaly ?
                baseReadBps * (2 + rand.nextDouble() * 3) : baseReadBps;

        double ioWriteBps = state.hasDiskAnomaly ?
                baseWriteBps * (2 + rand.nextDouble() * 3) : baseWriteBps;

        metric.setIoReadBps(ioReadBps);
        metric.setIoWriteBps(ioWriteBps);

        // I/O 사용률
        double ioTimePercentage = state.hasDiskAnomaly ?
                Math.min(95, 30 + rand.nextDouble() * 50) : 5 + rand.nextDouble() * 20;

        metric.setIoTimePercentage(ioTimePercentage);

        // 누적 I/O 카운터
        String key = "disk_" + equipmentId;

        long prevReadCount = cumulativeIoReads.getOrDefault(key, 0L);
        long prevWriteCount = cumulativeIoWrites.getOrDefault(key, 0L);

        long readInc = (long)(ioReadBps / 4096 * 15);  // 5초간 읽기 횟수
        long writeInc = (long)(ioWriteBps / 4096 * 15);

        long newReadCount = prevReadCount + readInc;
        long newWriteCount = prevWriteCount + writeInc;

        cumulativeIoReads.put(key, newReadCount);
        cumulativeIoWrites.put(key, newWriteCount);

        metric.setIoReadCount(newReadCount);
        metric.setIoWriteCount(newWriteCount);

        // ===== inode (그래프 4.6) =====
        long totalInodes = 32_000_000L;
        double inodeUsagePercent = 15 + rand.nextDouble() * 30;

        long usedInodes = (long)(totalInodes * inodeUsagePercent / 100);
        long freeInodes = totalInodes - usedInodes;

        metric.setTotalInodes(totalInodes);
        metric.setUsedInodes(usedInodes);
        metric.setFreeInodes(freeInodes);
        metric.setUsedInodePercentage(inodeUsagePercent);

        return metric;
    }

    /**
     * 네트워크 메트릭 생성 - 모든 그래프 지원
     */
    private NetworkMetric generateNetworkMetric(Long equipmentId, String nicName, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        NetworkMetric metric = NetworkMetric.builder()
                .equipmentId(equipmentId)
                .nicName(nicName)
                .generateTime(time)
                .build();

        double bandwidthBps = 1_000_000_000.0;  // 1Gbps

        // ===== RX/TX 사용률 (그래프 3.1, 3.2) =====
        double baseRxUsage = 5 + rand.nextDouble() * 15;
        double baseTxUsage = 3 + rand.nextDouble() * 12;

        double rxUsage = state.hasNetworkAnomaly ?
                Math.min(95, baseRxUsage + 50 + rand.nextDouble() * 25) : baseRxUsage;

        double txUsage = state.hasNetworkAnomaly ?
                Math.min(95, baseTxUsage + 50 + rand.nextDouble() * 25) : baseTxUsage;

        metric.setRxUsage(rxUsage);
        metric.setTxUsage(txUsage);

        // ===== 초당 전송량 (그래프 3.7) =====
        double inBytesPerSec = (bandwidthBps / 8) * (rxUsage / 100.0);
        double outBytesPerSec = (bandwidthBps / 8) * (txUsage / 100.0);

        metric.setInBytesPerSec(inBytesPerSec);
        metric.setOutBytesPerSec(outBytesPerSec);

        double inPktsPerSec = inBytesPerSec / 1500;  // 평균 패킷 크기 1500 bytes
        double outPktsPerSec = outBytesPerSec / 1500;

        metric.setInPktsPerSec(inPktsPerSec);
        metric.setOutPktsPerSec(outPktsPerSec);

        // ===== 누적 카운터 업데이트 =====
        String key = equipmentId + "_" + nicName;

        long prevInPackets = cumulativeInPackets.getOrDefault(key, 0L);
        long prevOutPackets = cumulativeOutPackets.getOrDefault(key, 0L);
        long prevInBytes = cumulativeInBytes.getOrDefault(key, 0L);
        long prevOutBytes = cumulativeOutBytes.getOrDefault(key, 0L);

        long inPacketsInc = (long)(inPktsPerSec * 15);  // 5초간 증가량
        long outPacketsInc = (long)(outPktsPerSec * 15);
        long inBytesInc = (long)(inBytesPerSec * 15);
        long outBytesInc = (long)(outBytesPerSec * 15);

        long newInPackets = prevInPackets + inPacketsInc;
        long newOutPackets = prevOutPackets + outPacketsInc;
        long newInBytes = prevInBytes + inBytesInc;
        long newOutBytes = prevOutBytes + outBytesInc;

        cumulativeInPackets.put(key, newInPackets);
        cumulativeOutPackets.put(key, newOutPackets);
        cumulativeInBytes.put(key, newInBytes);
        cumulativeOutBytes.put(key, newOutBytes);

        // ===== 패킷/바이트 누적 (그래프 3.3, 3.4, 3.5, 3.6) =====
        metric.setInPktsTot(newInPackets);
        metric.setOutPktsTot(newOutPackets);
        metric.setInBytesTot(newInBytes);
        metric.setOutBytesTot(newOutBytes);

        // ===== 에러/드롭 패킷 (그래프 3.8) =====
        long inErrorInc = state.hasNetworkAnomaly ? rand.nextLong(100) : rand.nextLong(5);
        long outErrorInc = state.hasNetworkAnomaly ? rand.nextLong(100) : rand.nextLong(5);
        long inDiscardInc = state.hasNetworkAnomaly ? rand.nextLong(50) : rand.nextLong(2);
        long outDiscardInc = state.hasNetworkAnomaly ? rand.nextLong(50) : rand.nextLong(2);

        long prevInErrors = cumulativeInErrors.getOrDefault(key, 0L);
        long prevOutErrors = cumulativeOutErrors.getOrDefault(key, 0L);
        long prevInDiscards = cumulativeInDiscards.getOrDefault(key, 0L);
        long prevOutDiscards = cumulativeOutDiscards.getOrDefault(key, 0L);

        long newInErrors = prevInErrors + inErrorInc;
        long newOutErrors = prevOutErrors + outErrorInc;
        long newInDiscards = prevInDiscards + inDiscardInc;
        long newOutDiscards = prevOutDiscards + outDiscardInc;

        cumulativeInErrors.put(key, newInErrors);
        cumulativeOutErrors.put(key, newOutErrors);
        cumulativeInDiscards.put(key, newInDiscards);
        cumulativeOutDiscards.put(key, newOutDiscards);

        metric.setInErrorPktsTot(newInErrors);
        metric.setOutErrorPktsTot(newOutErrors);
        metric.setInDiscardPktsTot(newInDiscards);
        metric.setOutDiscardPktsTot(newOutDiscards);

        // ===== 인터페이스 상태 (그래프 3.9) =====
        metric.setOperStatus(1);  // 1=UP, 0=DOWN

        return metric;
    }

    /**
     * 환경 메트릭 생성 (온도/습도) - 랙 기준
     */
    private EnvironmentMetric generateEnvironmentMetric(Long rackId, LocalDateTime time) {
        AnomalyState state = rackAnomalyStates.get(rackId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        EnvironmentMetric metric = EnvironmentMetric.builder()
                .rackId(rackId)
                .generateTime(time)
                .build();

        // ===== 온도 생성 (정상 범위: 18°C ~ 26°C, 권장: 20°C ~ 24°C) =====
        double baseTemperature = 20.0 + rand.nextDouble() * 4.0;  // 20~24°C

        double currentTemperature;
        if (state.hasTemperatureAnomaly) {
            // 이상 징후 시 온도 급상승 (28°C ~ 35°C)
            currentTemperature = 28.0 + rand.nextDouble() * 7.0;
        } else {
            // 정상 범위 내 변동
            currentTemperature = baseTemperature + (rand.nextDouble() - 0.5) * 2.0;
        }

        metric.setTemperature(Math.round(currentTemperature * 100.0) / 100.0);

        // 최저/최고 온도 추적
        double currentMin = minTemperatureTracker.get(rackId);
        double currentMax = maxTemperatureTracker.get(rackId);

        if (currentTemperature < currentMin) {
            minTemperatureTracker.put(rackId, currentTemperature);
            currentMin = currentTemperature;
        }
        if (currentTemperature > currentMax) {
            maxTemperatureTracker.put(rackId, currentTemperature);
            currentMax = currentTemperature;
        }

        metric.setMinTemperature(Math.round(currentMin * 100.0) / 100.0);
        metric.setMaxTemperature(Math.round(currentMax * 100.0) / 100.0);

        // 온도 경고 설정 (26°C 이상 경고)
        metric.setTemperatureWarning(currentTemperature >= 26.0);

        // ===== 습도 생성 (정상 범위: 40% ~ 60%, 권장: 45% ~ 55%) =====
        double baseHumidity = 45.0 + rand.nextDouble() * 10.0;  // 45~55%

        double currentHumidity;
        if (state.hasHumidityAnomaly) {
            // 이상 징후 시 습도 급상승 또는 급하강
            if (rand.nextBoolean()) {
                currentHumidity = 65.0 + rand.nextDouble() * 15.0;  // 높음: 65~80%
            } else {
                currentHumidity = 20.0 + rand.nextDouble() * 15.0;  // 낮음: 20~35%
            }
        } else {
            // 정상 범위 내 변동
            currentHumidity = baseHumidity + (rand.nextDouble() - 0.5) * 5.0;
        }

        metric.setHumidity(Math.round(currentHumidity * 100.0) / 100.0);

        // 최저/최고 습도 추적
        double currentMinHumidity = minHumidityTracker.get(rackId);
        double currentMaxHumidity = maxHumidityTracker.get(rackId);

        if (currentHumidity < currentMinHumidity) {
            minHumidityTracker.put(rackId, currentHumidity);
            currentMinHumidity = currentHumidity;
        }
        if (currentHumidity > currentMaxHumidity) {
            maxHumidityTracker.put(rackId, currentHumidity);
            currentMaxHumidity = currentHumidity;
        }

        metric.setMinHumidity(Math.round(currentMinHumidity * 100.0) / 100.0);
        metric.setMaxHumidity(Math.round(currentMaxHumidity * 100.0) / 100.0);

        // 습도 경고 설정 (40% 미만 또는 60% 초과 시 경고)
        metric.setHumidityWarning(currentHumidity < 40.0 || currentHumidity > 60.0);

        return metric;
    }

    /**
     * 이상 징후 시뮬레이션 (실제 서버실 수준으로 감소)
     */
    private void maybeUpdateAnomalies() {
        long currentTime = System.currentTimeMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 장비별 이상 징후 (발생 확률 대폭 감소)
        for (Equipment equipment : activeEquipments) {
            Long equipmentId = equipment.getId();
            AnomalyState state = anomalyStates.get(equipmentId);

            // CPU 이상 징후 (5% -> 0.5% = 1/200 확률)
            if (state.hasCpuAnomaly) {
                if (currentTime - state.cpuAnomalyStartTime > state.cpuAnomalyDuration) {
                    state.hasCpuAnomaly = false;
                    log.warn("✅ [Equipment {}] CPU 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < 0.005) {  // ✅ 5% -> 0.5%
                state.hasCpuAnomaly = true;
                state.cpuAnomalyStartTime = currentTime;
                state.cpuAnomalyDuration = 60_000 + random.nextInt(120_000);  // 1~3분
                log.error("🚨 [Equipment {}] CPU 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.cpuAnomalyDuration / 1000);
            }

            // 메모리 이상 징후 (4% -> 0.3%)
            if (state.hasMemoryAnomaly) {
                if (currentTime - state.memoryAnomalyStartTime > state.memoryAnomalyDuration) {
                    state.hasMemoryAnomaly = false;
                    log.warn("✅ [Equipment {}] 메모리 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < 0.003) {  // ✅ 4% -> 0.3%
                state.hasMemoryAnomaly = true;
                state.memoryAnomalyStartTime = currentTime;
                state.memoryAnomalyDuration = 40_000 + random.nextInt(80_000);
                log.error("🚨 [Equipment {}] 메모리 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.memoryAnomalyDuration / 1000);
            }

            // 디스크 I/O 이상 징후 (3% -> 0.2%)
            if (state.hasDiskAnomaly) {
                if (currentTime - state.diskAnomalyStartTime > state.diskAnomalyDuration) {
                    state.hasDiskAnomaly = false;
                    log.warn("✅ [Equipment {}] 디스크 I/O 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < 0.002) {  // ✅ 3% -> 0.2%
                state.hasDiskAnomaly = true;
                state.diskAnomalyStartTime = currentTime;
                state.diskAnomalyDuration = 30_000 + random.nextInt(90_000);
                log.error("🚨 [Equipment {}] 디스크 I/O 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.diskAnomalyDuration / 1000);
            }

            // 네트워크 이상 징후 (6% -> 0.4%)
            if (state.hasNetworkAnomaly) {
                if (currentTime - state.networkAnomalyStartTime > state.networkAnomalyDuration) {
                    state.hasNetworkAnomaly = false;
                    log.warn("✅ [Equipment {}] 네트워크 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < 0.004) {  // ✅ 6% -> 0.4%
                state.hasNetworkAnomaly = true;
                state.networkAnomalyStartTime = currentTime;
                state.networkAnomalyDuration = 40_000 + random.nextInt(100_000);
                log.error("🚨 [Equipment {}] 네트워크 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.networkAnomalyDuration / 1000);
            }
        }

        // 랙별 환경 이상 징후 (발생 확률 대폭 감소)
        for (Rack rack : activeRacks) {
            Long rackId = rack.getId();
            AnomalyState state = rackAnomalyStates.get(rackId);

            // 온도 이상 징후 (4% -> 0.3%)
            if (state.hasTemperatureAnomaly) {
                if (currentTime - state.temperatureAnomalyStartTime > state.temperatureAnomalyDuration) {
                    state.hasTemperatureAnomaly = false;
                    log.warn("✅ [Rack {}] 온도 이상 징후 해소!", rackId);
                }
            } else if (random.nextDouble() < 0.003) {  // ✅ 4% -> 0.3%
                state.hasTemperatureAnomaly = true;
                state.temperatureAnomalyStartTime = currentTime;
                state.temperatureAnomalyDuration = 50_000 + random.nextInt(150_000);
                log.error("🚨 [Rack {}] 온도 이상 징후 발생! (지속: {}초)",
                        rackId, state.temperatureAnomalyDuration / 1000);
            }

            // 습도 이상 징후 (3% -> 0.2%)
            if (state.hasHumidityAnomaly) {
                if (currentTime - state.humidityAnomalyStartTime > state.humidityAnomalyDuration) {
                    state.hasHumidityAnomaly = false;
                    log.warn("✅ [Rack {}] 습도 이상 징후 해소!", rackId);
                }
            } else if (random.nextDouble() < 0.002) {  // ✅ 3% -> 0.2%
                state.hasHumidityAnomaly = true;
                state.humidityAnomalyStartTime = currentTime;
                state.humidityAnomalyDuration = 45_000 + random.nextInt(135_000);
                log.error("🚨 [Rack {}] 습도 이상 징후 발생! (지속: {}초)",
                        rackId, state.humidityAnomalyDuration / 1000);
            }
        }
    }


    private static class AnomalyState {
        boolean hasCpuAnomaly = false;
        long cpuAnomalyStartTime = 0;
        long cpuAnomalyDuration = 0;

        boolean hasMemoryAnomaly = false;
        long memoryAnomalyStartTime = 0;
        long memoryAnomalyDuration = 0;

        boolean hasDiskAnomaly = false;
        long diskAnomalyStartTime = 0;
        long diskAnomalyDuration = 0;

        boolean hasNetworkAnomaly = false;
        long networkAnomalyStartTime = 0;
        long networkAnomalyDuration = 0;

        // 온도/습도 이상 징후
        boolean hasTemperatureAnomaly = false;
        long temperatureAnomalyStartTime = 0;
        long temperatureAnomalyDuration = 0;

        boolean hasHumidityAnomaly = false;
        long humidityAnomalyStartTime = 0;
        long humidityAnomalyDuration = 0;
    }

    /**
     * 외부에서 새 장비가 등록되었을 때 시뮬레이터에 즉시 반영하는 메서드
     */
    public void addEquipment(Equipment newEquipment) {
        // 1. 메모리 리스트에 새 장비 추가
        this.activeEquipments.add(newEquipment);

        // 2. NIC 정보 등 초기화
        Long equipmentId = newEquipment.getId();
        EquipmentType type = newEquipment.getType();

        if (hasNetworkMetric(type)) {
            EQUIPMENT_NICS.put(equipmentId, generateDefaultNics(type));
        }

        // 3. 이상 징후 상태 초기화
        anomalyStates.put(equipmentId, new AnomalyState());

        log.info("🆕 새 장비 시뮬레이터 등록 완료: ID={}, Name={}", equipmentId, newEquipment.getName());
    }
}
