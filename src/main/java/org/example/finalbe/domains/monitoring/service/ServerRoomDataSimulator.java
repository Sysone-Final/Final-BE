package org.example.finalbe.domains.monitoring.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.service.AlertEvaluationService;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerRoomDataSimulator {

    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SseService sseService;
    private final MonitoringMetricCache monitoringMetricCache;
    private final AlertEvaluationService alertEvaluationService;

    private static final Map<Long, List<String>> EQUIPMENT_NICS = new HashMap<>();

    private final Map<Long, AnomalyState> anomalyStates = new HashMap<>();
    private final Map<Long, AnomalyState> rackAnomalyStates = new HashMap<>();
    private final Random random = new Random();

    @Value("${monitoring.simulator.excluded-equipment-ids:256,257,258,259}")
    private String excludedEquipmentIdsStr;

    private Set<Long> excludedEquipmentIds = new HashSet<>();

    // 누적 카운터
    private final Map<String, Long> cumulativeInPackets = new HashMap<>();
    private final Map<String, Long> cumulativeOutPackets = new HashMap<>();
    private final Map<String, Long> cumulativeInErrors = new HashMap<>();
    private final Map<String, Long> cumulativeOutErrors = new HashMap<>();
    private final Map<String, Long> cumulativeInDiscards = new HashMap<>();
    private final Map<String, Long> cumulativeOutDiscards = new HashMap<>();
    private final Map<String, Long> cumulativeIoReads = new HashMap<>();
    private final Map<String, Long> cumulativeIoWrites = new HashMap<>();
    private final Map<String, Long> cumulativeInBytes = new ConcurrentHashMap<>();
    private final Map<String, Long> cumulativeOutBytes = new ConcurrentHashMap<>();

    private final Map<Long, Double> minTemperatureTracker = new HashMap<>();
    private final Map<Long, Double> maxTemperatureTracker = new HashMap<>();
    private final Map<Long, Double> minHumidityTracker = new HashMap<>();
    private final Map<Long, Double> maxHumidityTracker = new HashMap<>();

    private List<Equipment> activeEquipments = new CopyOnWriteArrayList<>();
    private List<Rack> activeRacks = new CopyOnWriteArrayList<>();

    private static final double HOURLY_PROBABILITY = 1.0 / 720.0;

    @PostConstruct
    public void init() {
        log.info("🚀 서버실 데이터 시뮬레이터 초기화 시작...");

        // Excluded Equipment IDs 파싱
        if (excludedEquipmentIdsStr != null && !excludedEquipmentIdsStr.trim().isEmpty()) {
            String[] ids = excludedEquipmentIdsStr.split(",");
            for (String id : ids) {
                try {
                    excludedEquipmentIds.add(Long.parseLong(id.trim()));
                } catch (NumberFormatException e) {
                    log.warn("⚠️ 잘못된 Excluded Equipment ID: {}", id);
                }
            }
        }
        log.info("🚫 더미 데이터 생성 제외 장비 ID: {}", excludedEquipmentIds);


        activeEquipments = equipmentRepository.findAll().stream()
                .filter(e -> DelYN.N.equals(e.getDelYn()))
                .filter(e -> e.getRack() != null)
                .collect(java.util.stream.Collectors.toCollection(CopyOnWriteArrayList::new));

        // DB에서 삭제되지 않은 랙만 로드
        activeRacks = rackRepository.findAll().stream()
                .filter(r -> DelYN.N.equals(r.getDelYn()))
                .collect(java.util.stream.Collectors.toCollection(CopyOnWriteArrayList::new));

        log.info("📊 DB에서 로드된 장비 총 개수: {} (랙 배치된 장비만)", activeEquipments.size());
        log.info("📊 DB에서 로드된 랙 총 개수: {}", activeRacks.size());

        if (activeEquipments.isEmpty()) {
            log.warn("⚠️ DB에 랙에 배치된 장비가 없습니다. 시뮬레이터가 동작하지 않습니다.");
            return;
        }

        for (Equipment equipment : activeEquipments) {
            Long equipmentId = equipment.getId();
            EquipmentType type = equipment.getType();

            if (excludedEquipmentIds.contains(equipmentId)) {
                log.info("⏭️ 장비 ID {}는 실제 Prometheus 데이터 사용 - 더미 생성 제외", equipmentId);
                continue;
            }

            if (hasNetworkMetric(type)) {
                EQUIPMENT_NICS.put(equipmentId, generateDefaultNics(type));
            }

            anomalyStates.put(equipmentId, new AnomalyState());
        }

        for (Rack rack : activeRacks) {
            Long rackId = rack.getId();
            rackAnomalyStates.put(rackId, new AnomalyState());
            minTemperatureTracker.put(rackId, 22.0);
            maxTemperatureTracker.put(rackId, 22.0);
            minHumidityTracker.put(rackId, 45.0);
            maxHumidityTracker.put(rackId, 45.0);
        }

        int activeCount = (int) activeEquipments.stream()
                .filter(e -> !excludedEquipmentIds.contains(e.getId()))
                .count();

        log.info("✅ 초기화 완료! {}개 장비(랙 배치 + 더미 생성 대상) + {}개 랙 모니터링 시작",
                activeCount, activeRacks.size());
    }

    @Scheduled(fixedDelayString = "${monitoring.simulator.interval-seconds:5000}", initialDelay = 2000)
    @Transactional
    public void generateRealtimeMetrics() {
        log.info("📊 =================================================");
        log.info("📊 generateRealtimeMetrics 시작");
        log.info("📊 activeEquipments 총 개수: {}", activeEquipments.size());

        if (activeEquipments.isEmpty()) {
            log.warn("⚠️ activeEquipments가 비어있어서 메트릭 생성 중단!");
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        long startTime = System.currentTimeMillis();

        List<SystemMetric> systemMetricsToSave = new ArrayList<>();
        List<DiskMetric> diskMetricsToSave = new ArrayList<>();
        List<NetworkMetric> networkMetricsToSave = new ArrayList<>();
        List<EnvironmentMetric> environmentMetricsToSave = new ArrayList<>();

        int skippedExcluded = 0;
        int skippedDeleted = 0;
        int skippedNoRack = 0;
        int processed = 0;
        int alertEvaluationCount = 0;

        // ✅ 랙 ID를 수집할 Set 추가
        Set<Long> activeRackIds = new HashSet<>();

        try {
            for (Equipment equipment : activeEquipments) {
                Long equipmentId = equipment.getId();
                EquipmentType type = equipment.getType();

                if (equipment.getRack() == null) {
                    log.debug("⏭️ 장비 ID {} 건너뜀 (랙에 배치되지 않음)", equipmentId);
                    skippedNoRack++;
                    continue;
                }

                // ✅ 랙 ID 수집
                activeRackIds.add(equipment.getRack().getId());

                if (excludedEquipmentIds.contains(equipmentId)) {
                    log.debug("⏭️ 장비 ID {} 건너뜀 (excluded)", equipmentId);
                    skippedExcluded++;
                    continue;
                }

                if (DelYN.Y.equals(equipment.getDelYn())) {
                    log.debug("⏭️ 장비 ID {} 건너뜀 (del_yn=Y)", equipmentId);
                    skippedDeleted++;
                    continue;
                }

                log.debug("✅ 장비 ID {} 메트릭 생성 시작 (type={})", equipmentId, type);
                processed++;

                // System 메트릭
                if (hasSystemMetric(type)) {
                    SystemMetric sysMetric = generateSystemMetric(equipmentId, now);
                    systemMetricsToSave.add(sysMetric);
                    monitoringMetricCache.updateSystemMetric(sysMetric);
                    sseService.sendToEquipment(equipmentId, "system", sysMetric);

                    if (needsSystemAlertEvaluation(sysMetric, equipment)) {
                        try {
                            alertEvaluationService.evaluateSystemMetric(sysMetric);
                            alertEvaluationCount++;
                        } catch (Exception e) {
                            log.warn("⚠️ System 알림 평가 실패: equipmentId={}, error={}", equipmentId, e.getMessage());
                        }
                    }

                    log.debug("  → System 메트릭 생성 완료 (equipmentId={})", equipmentId);
                }

                // Disk 메트릭
                if (hasDiskMetric(type)) {
                    DiskMetric diskMetric = generateDiskMetric(equipmentId, now);
                    diskMetricsToSave.add(diskMetric);
                    monitoringMetricCache.updateDiskMetric(diskMetric);
                    sseService.sendToEquipment(equipmentId, "disk", diskMetric);

                    if (needsDiskAlertEvaluation(diskMetric, equipment)) {
                        try {
                            alertEvaluationService.evaluateDiskMetric(diskMetric);
                            alertEvaluationCount++;
                        } catch (Exception e) {
                            log.warn("⚠️ Disk 알림 평가 실패: equipmentId={}, error={}", equipmentId, e.getMessage());
                        }
                    }

                    log.debug("  → Disk 메트릭 생성 완료 (equipmentId={})", equipmentId);
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

                            if (needsNetworkAlertEvaluation(nicMetric, equipment)) {
                                try {
                                    alertEvaluationService.evaluateNetworkMetric(nicMetric);
                                    alertEvaluationCount++;
                                } catch (Exception e) {
                                    log.warn("⚠️ Network 알림 평가 실패: equipmentId={}, nic={}, error={}",
                                            equipmentId, nic, e.getMessage());
                                }
                            }
                        }
                    }
                    log.debug("  → Network 메트릭 생성 완료 (equipmentId={}, NICs={})",
                            equipmentId, nics != null ? nics.size() : 0);
                }

                // ❌ 기존의 ENVIRONMENTAL_SENSOR 타입 체크 로직 삭제
            }

            log.info("🌡️ 환경 메트릭 생성 시작 - 활성 랙 개수: {}", activeRackIds.size());

            for (Long rackId : activeRackIds) {
                try {
                    Rack rack = rackRepository.findById(rackId).orElse(null);

                    if (rack == null) {
                        log.warn("⚠️ 랙 정보를 찾을 수 없음: rackId={}", rackId);
                        continue;
                    }

                    EnvironmentMetric envMetric = generateEnvironmentMetricForRack(rackId, now);
                    if (envMetric != null) {
                        environmentMetricsToSave.add(envMetric);
                        monitoringMetricCache.updateEnvironmentMetric(envMetric);
                        sseService.sendToRack(rackId, "environment", envMetric);

                        if (needsEnvironmentAlertEvaluation(envMetric, rack)) {
                            try {
                                alertEvaluationService.evaluateEnvironmentMetric(envMetric);
                                alertEvaluationCount++;
                            } catch (Exception e) {
                                log.warn("⚠️ Environment 알림 평가 실패: rackId={}, error={}",
                                        rackId, e.getMessage());
                            }
                        }

                        log.debug("  → Environment 메트릭 생성 완료 (rackId={})", rackId);
                    }
                } catch (Exception e) {
                    log.error("❌ 랙 {} 환경 메트릭 생성 실패", rackId, e);
                }
            }

            // DB에 한 번에 저장 (Batch Insert)
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

            log.info("📊 메트릭 생성 완료:");
            log.info("  - 전체 장비: {}", activeEquipments.size());
            log.info("  - Excluded 제외: {}", skippedExcluded);
            log.info("  - 삭제됨 제외: {}", skippedDeleted);
            log.info("  - 랙 미배치 제외: {}", skippedNoRack);
            log.info("  - 실제 처리: {}", processed);
            log.info("  - 활성 랙: {}", activeRackIds.size());
            log.info("  - System 메트릭: {}", systemMetricsToSave.size());
            log.info("  - Disk 메트릭: {}", diskMetricsToSave.size());
            log.info("  - Network 메트릭: {}", networkMetricsToSave.size());
            log.info("  - Environment 메트릭: {}", environmentMetricsToSave.size());

            int totalMetrics = systemMetricsToSave.size() + diskMetricsToSave.size() +
                    networkMetricsToSave.size() + environmentMetricsToSave.size();
            if (totalMetrics > 0) {
                log.info("  ✅ 알림 평가 실행: {} 건 (전체 메트릭의 {}%)",
                        alertEvaluationCount,
                        String.format("%.1f", alertEvaluationCount * 100.0 / totalMetrics));
            }

            log.info("🚀 SSE 전송 완료 & DB 작업 할당 끝: {}ms 소요", duration);
            log.info("📊 =================================================");

        } catch (Exception e) {
            log.error("❌ 메트릭 생성 중 오류 발생", e);
        }
    }

    public void addEquipment(Equipment equipment) {
        if (equipment == null) {
            log.warn("⚠️ addEquipment: equipment가 null입니다.");
            return;
        }

        // ✅ 랙 배치 여부 체크
        if (equipment.getRack() == null) {
            log.info("⊘ 장비 ID {}는 랙에 배치되지 않아 시뮬레이터에 등록하지 않습니다.", equipment.getId());
            return;
        }

        if (excludedEquipmentIds.contains(equipment.getId())) {
            log.info("⏭️ 장비 ID {}는 제외 목록에 있어 시뮬레이터에 등록하지 않습니다.", equipment.getId());
            return;
        }

        // 이미 존재하는지 확인
        boolean exists = activeEquipments.stream()
                .anyMatch(e -> e.getId().equals(equipment.getId()));

        if (!exists) {
            activeEquipments.add(equipment);

            // 네트워크 타입이면 NIC 정보 추가
            if (hasNetworkMetric(equipment.getType())) {
                EQUIPMENT_NICS.put(equipment.getId(), generateDefaultNics(equipment.getType()));
            }

            // Anomaly 상태 초기화
            anomalyStates.put(equipment.getId(), new AnomalyState());

            log.info("시뮬레이터에 장비 추가: ID={}, Type={}, Rack={}",
                    equipment.getId(), equipment.getType(), equipment.getRack().getId());
        } else {
            log.debug("이미 등록된 장비입니다: ID={}", equipment.getId());
        }
    }

    public void removeEquipment(Long equipmentId) {
        if (equipmentId == null) {
            log.warn("⚠️ removeEquipment: equipmentId가 null입니다.");
            return;
        }

        boolean removed = activeEquipments.removeIf(e -> e.getId().equals(equipmentId));

        if (removed) {
            EQUIPMENT_NICS.remove(equipmentId);
            anomalyStates.remove(equipmentId);

            log.info("✅ 시뮬레이터에서 장비 제거: ID={}", equipmentId);
        } else {
            log.debug("제거할 장비가 없습니다: ID={}", equipmentId);
        }
    }

    private List<String> generateDefaultNics(EquipmentType type) {
        switch (type) {
            case SERVER:
                return Arrays.asList("eth0", "eth1");
            case STORAGE:
                return Arrays.asList("mgmt0", "data0");
            case SWITCH:
                return Arrays.asList("GigabitEthernet1/0/1", "GigabitEthernet1/0/2",
                        "GigabitEthernet1/0/3", "GigabitEthernet1/0/4");
            case ROUTER:
                return Arrays.asList("GigabitEthernet0/0", "GigabitEthernet0/1", "GigabitEthernet0/2");
            case FIREWALL:
                return Arrays.asList("port1", "port2", "port3", "port4");
            case LOAD_BALANCER:
                return Arrays.asList("nic1", "nic2");
            case PDU:
                return Arrays.asList("mgmt0");
            case ENVIRONMENTAL_SENSOR:
                return Arrays.asList("sensor0");
            case KVM:
                return Collections.emptyList();
            default:
                throw new IllegalArgumentException("지원하지 않는 장비 타입: " + type);
        }
    }

    private boolean hasSystemMetric(EquipmentType type) {
        return type == EquipmentType.SERVER ||
                type == EquipmentType.STORAGE ||
                type == EquipmentType.FIREWALL ||
                type == EquipmentType.LOAD_BALANCER;
    }


    private boolean hasDiskMetric(EquipmentType type) {
        return type == EquipmentType.SERVER || type == EquipmentType.STORAGE;
    }

    private boolean hasNetworkMetric(EquipmentType type) {
        return type == EquipmentType.SERVER ||
                type == EquipmentType.STORAGE ||
                type == EquipmentType.SWITCH ||
                type == EquipmentType.ROUTER ||
                type == EquipmentType.FIREWALL ||
                type == EquipmentType.LOAD_BALANCER ||
                type == EquipmentType.PDU ||
                type == EquipmentType.ENVIRONMENTAL_SENSOR;
    }

    private SystemMetric generateSystemMetric(Long equipmentId, LocalDateTime time) {
        Equipment equipment = activeEquipments.stream()
                .filter(e -> e.getId().equals(equipmentId))
                .findFirst()
                .orElse(null);

        if (equipment == null) {
            return null;
        }

        EquipmentType type = equipment.getType();
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        SystemMetric metric = SystemMetric.builder()
                .equipmentId(equipmentId)
                .generateTime(time)
                .build();

        // ==================== 장비 유형별 CPU 설정 ====================
        double baseCpu, cpuUsage;

        switch (type) {
            case SERVER:
                baseCpu = 30 + rand.nextDouble() * 35;  // 30~65%
                break;
            case STORAGE:
                baseCpu = 10 + rand.nextDouble() * 20;  // 10~30% (낮음)
                break;
            case FIREWALL:
                baseCpu = 20 + rand.nextDouble() * 30;  // 20~50% (패킷 처리)
                break;
            case LOAD_BALANCER:
                baseCpu = 15 + rand.nextDouble() * 25;  // 15~40%
                break;
            default:
                baseCpu = 15 + rand.nextDouble() * 20;
        }

        cpuUsage = state.hasCpuAnomaly ?
                Math.min(95, baseCpu + 50 + rand.nextDouble() * 20) : baseCpu;

        double cpuIdle = 100.0 - cpuUsage;
        metric.setCpuIdle(cpuIdle);
        metric.setCpuUser(cpuUsage * 0.6);
        metric.setCpuSystem(cpuUsage * 0.25);
        metric.setCpuWait(cpuUsage * 0.08);
        metric.setCpuNice(cpuUsage * 0.03);
        metric.setCpuIrq(cpuUsage * 0.02);
        metric.setCpuSoftirq(cpuUsage * 0.015);
        metric.setCpuSteal(cpuUsage * 0.005);

        // ==================== Load Average (SERVER, STORAGE만) ====================
        if (type == EquipmentType.SERVER || type == EquipmentType.STORAGE) {
            double loadAvg = cpuUsage / 100.0 * 4;
            metric.setLoadAvg1(loadAvg + rand.nextDouble() * 0.5);
            metric.setLoadAvg5(loadAvg + rand.nextDouble() * 0.3);
            metric.setLoadAvg15(loadAvg + rand.nextDouble() * 0.2);

            long contextSwitches = (long) (1000 + rand.nextDouble() * 9000);
            metric.setContextSwitches(contextSwitches);
        } else {
            // FIREWALL, LOAD_BALANCER는 NULL
            metric.setLoadAvg1(null);
            metric.setLoadAvg5(null);
            metric.setLoadAvg15(null);
            metric.setContextSwitches(null);
        }

        // ==================== 장비 유형별 메모리 설정 ====================
        long totalMemory = 16L * 1024 * 1024 * 1024;  // 16GB
        double baseMemUsage, memUsagePercent;

        switch (type) {
            case SERVER:
                baseMemUsage = 40 + rand.nextDouble() * 30;  // 40~70%
                break;
            case STORAGE:
                baseMemUsage = 30 + rand.nextDouble() * 20;  // 30~50%
                break;
            case FIREWALL:
                baseMemUsage = 30 + rand.nextDouble() * 30;  // 30~60% (세션 테이블)
                break;
            case LOAD_BALANCER:
                baseMemUsage = 25 + rand.nextDouble() * 25;  // 25~50%
                break;
            default:
                baseMemUsage = 40 + rand.nextDouble() * 20;
        }

        memUsagePercent = state.hasMemoryAnomaly ?
                Math.min(95, baseMemUsage + 30 + rand.nextDouble() * 15) : baseMemUsage;

        long usedMemory = (long) (totalMemory * memUsagePercent / 100);
        long freeMemory = totalMemory - usedMemory;

        metric.setTotalMemory(totalMemory);
        metric.setUsedMemory(usedMemory);
        metric.setFreeMemory(freeMemory);
        metric.setUsedMemoryPercentage(memUsagePercent);

        long buffers = (long) (totalMemory * 0.05);
        long cached = (long) (totalMemory * 0.15);
        long active = (long) (usedMemory * 0.6);
        long inactive = (long) (usedMemory * 0.4);

        metric.setMemoryBuffers(buffers);
        metric.setMemoryCached(cached);
        metric.setMemoryActive(active);
        metric.setMemoryInactive(inactive);

        // ==================== Swap (SERVER, STORAGE만) ====================
        if (type == EquipmentType.SERVER || type == EquipmentType.STORAGE) {
            long totalSwap = 8L * 1024 * 1024 * 1024;
            double swapUsagePercent = state.hasMemoryAnomaly ?
                    Math.min(50, rand.nextDouble() * 30) : rand.nextDouble() * 10;

            long usedSwap = (long) (totalSwap * swapUsagePercent / 100);

            metric.setTotalSwap(totalSwap);
            metric.setUsedSwap(usedSwap);
            metric.setUsedSwapPercentage(swapUsagePercent);
        } else {
            // FIREWALL, LOAD_BALANCER는 NULL
            metric.setTotalSwap(null);
            metric.setUsedSwap(null);
            metric.setUsedSwapPercentage(null);
        }

        return metric;
    }

    private NetworkMetric generateNetworkMetric(Long equipmentId, String nicName, LocalDateTime time) {
        Equipment equipment = activeEquipments.stream()
                .filter(e -> e.getId().equals(equipmentId))
                .findFirst()
                .orElse(null);

        if (equipment == null) {
            return null;
        }

        EquipmentType type = equipment.getType();
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        NetworkMetric metric = NetworkMetric.builder()
                .equipmentId(equipmentId)
                .nicName(nicName)
                .generateTime(time)
                .build();

        double bandwidthBps = 1_000_000_000.0;  // 1Gbps

        // ==================== 장비 유형별 사용률 차별화 ====================
        double baseRxUsage, baseTxUsage;

        switch (type) {
            case SERVER:
                baseRxUsage = 10 + rand.nextDouble() * 30;  // 10~40%
                baseTxUsage = 5 + rand.nextDouble() * 25;   // 5~30%
                break;
            case STORAGE:
                baseRxUsage = 15 + rand.nextDouble() * 35;  // 15~50% (높음)
                baseTxUsage = 20 + rand.nextDouble() * 40;  // 20~60% (높음)
                break;
            case SWITCH:
            case ROUTER:
                baseRxUsage = 10 + rand.nextDouble() * 50;  // 10~60% (변동 큼)
                baseTxUsage = 10 + rand.nextDouble() * 50;  // 10~60%
                break;
            case FIREWALL:
                baseRxUsage = 15 + rand.nextDouble() * 35;  // 15~50%
                baseTxUsage = 10 + rand.nextDouble() * 30;  // 10~40%
                break;
            case LOAD_BALANCER:
                baseRxUsage = 20 + rand.nextDouble() * 40;  // 20~60% (높음)
                baseTxUsage = 20 + rand.nextDouble() * 40;  // 20~60%
                break;
            case PDU:
            case ENVIRONMENTAL_SENSOR:
                baseRxUsage = 0.1 + rand.nextDouble() * 0.5;  // 0.1~0.6% (매우 낮음)
                baseTxUsage = 0.1 + rand.nextDouble() * 0.5;  // 0.1~0.6%
                break;
            default:
                baseRxUsage = 5 + rand.nextDouble() * 15;
                baseTxUsage = 3 + rand.nextDouble() * 12;
        }

        double rxUsage = state.hasNetworkAnomaly ?
                Math.min(95, baseRxUsage + 50 + rand.nextDouble() * 25) : baseRxUsage;

        double txUsage = state.hasNetworkAnomaly ?
                Math.min(95, baseTxUsage + 45 + rand.nextDouble() * 25) : baseTxUsage;

        metric.setRxUsage(rxUsage);
        metric.setTxUsage(txUsage);

        String key = "network_" + equipmentId + "_" + nicName;

        // ==================== 패킷 처리 ====================
        long prevInPackets = cumulativeInPackets.getOrDefault(key, 0L);
        long prevOutPackets = cumulativeOutPackets.getOrDefault(key, 0L);

        long inPacketsInc = (long) (bandwidthBps * rxUsage / 100.0 / 1500 * 15);  // 15초 간격
        long outPacketsInc = (long) (bandwidthBps * txUsage / 100.0 / 1500 * 15);

        long newInPackets = prevInPackets + inPacketsInc;
        long newOutPackets = prevOutPackets + outPacketsInc;

        cumulativeInPackets.put(key, newInPackets);
        cumulativeOutPackets.put(key, newOutPackets);

        metric.setInPktsTot(newInPackets);
        metric.setOutPktsTot(newOutPackets);

        // ✅ 초당 패킷 수
        metric.setInPktsPerSec((double) inPacketsInc / 15.0);
        metric.setOutPktsPerSec((double) outPacketsInc / 15.0);

        // ==================== 바이트 처리 (추가) ====================
        long prevInBytes = cumulativeInBytes.getOrDefault(key, 0L);
        long prevOutBytes = cumulativeOutBytes.getOrDefault(key, 0L);

        // 평균 패킷 크기 1500바이트 가정
        long inBytesInc = inPacketsInc * 1500;
        long outBytesInc = outPacketsInc * 1500;

        long newInBytes = prevInBytes + inBytesInc;
        long newOutBytes = prevOutBytes + outBytesInc;

        cumulativeInBytes.put(key, newInBytes);
        cumulativeOutBytes.put(key, newOutBytes);

        metric.setInBytesTot(newInBytes);
        metric.setOutBytesTot(newOutBytes);

        metric.setInBytesPerSec((double) inBytesInc / 15.0);
        metric.setOutBytesPerSec((double) outBytesInc / 15.0);

        // ==================== 에러/드롭 처리 ====================
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

        metric.setOperStatus(1);

        return metric;
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
                "io_read_bps, io_write_bps, io_time_percentage, " +
                "io_read_count, io_write_count, " +
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
                "rx_usage, tx_usage, " +
                "in_pkts_tot, out_pkts_tot, " +
                "in_bytes_tot, out_bytes_tot, " +
                "in_bytes_per_sec, out_bytes_per_sec, " +
                "in_pkts_per_sec, out_pkts_per_sec, " +
                "in_error_pkts_tot, out_error_pkts_tot, " +
                "in_discard_pkts_tot, out_discard_pkts_tot, " +
                "oper_status) " +
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

    private boolean needsSystemAlertEvaluation(SystemMetric metric, Equipment equipment) {
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }
        return equipment.getCpuThresholdWarning() != null ||
                equipment.getMemoryThresholdWarning() != null;
    }

    private boolean needsDiskAlertEvaluation(DiskMetric metric, Equipment equipment) {
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }
        return equipment.getDiskThresholdWarning() != null;
    }

    private boolean needsNetworkAlertEvaluation(NetworkMetric metric, Equipment equipment) {
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }
        return true;
    }

    private boolean needsEnvironmentAlertEvaluation(EnvironmentMetric metric, Rack rack) {
        if (!Boolean.TRUE.equals(rack.getMonitoringEnabled())) {
            return false;
        }

        if (rack.getTemperatureThresholdWarning() != null &&
                metric.getTemperature() != null) {
            double threshold = rack.getTemperatureThresholdWarning().doubleValue();
            if (metric.getTemperature() >= threshold * 0.9) {
                return true;
            }
        }

        if (rack.getHumidityThresholdMinWarning() != null &&
                metric.getHumidity() != null) {
            double threshold = rack.getHumidityThresholdMinWarning().doubleValue();
            if (metric.getHumidity() <= threshold * 1.1) {
                return true;
            }
        }

        if (rack.getHumidityThresholdMaxWarning() != null &&
                metric.getHumidity() != null) {
            double threshold = rack.getHumidityThresholdMaxWarning().doubleValue();
            if (metric.getHumidity() >= threshold * 0.9) {
                return true;
            }
        }

        return false;
    }

    private void maybeUpdateAnomalies() {
        long currentTime = System.currentTimeMillis();

        for (Equipment equipment : activeEquipments) {
            Long equipmentId = equipment.getId();

            if (excludedEquipmentIds.contains(equipmentId)) {
                continue;
            }

            if (DelYN.Y.equals(equipment.getDelYn())) {
                continue;
            }

            AnomalyState state = anomalyStates.get(equipmentId);

            if (state.hasCpuAnomaly) {
                if (currentTime - state.cpuAnomalyStartTime > state.cpuAnomalyDuration) {
                    state.hasCpuAnomaly = false;
                    log.warn("✅ [Equipment {}] CPU 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
                state.hasCpuAnomaly = true;
                state.cpuAnomalyStartTime = currentTime;
                state.cpuAnomalyDuration = 30_000 + random.nextInt(120_000);
                log.error("🚨 [Equipment {}] CPU 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.cpuAnomalyDuration / 1000);
            }

            if (state.hasMemoryAnomaly) {
                if (currentTime - state.memoryAnomalyStartTime > state.memoryAnomalyDuration) {
                    state.hasMemoryAnomaly = false;
                    log.warn("✅ [Equipment {}] 메모리 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
                state.hasMemoryAnomaly = true;
                state.memoryAnomalyStartTime = currentTime;
                state.memoryAnomalyDuration = 35_000 + random.nextInt(125_000);
                log.error("🚨 [Equipment {}] 메모리 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.memoryAnomalyDuration / 1000);
            }

            if (state.hasDiskAnomaly) {
                if (currentTime - state.diskAnomalyStartTime > state.diskAnomalyDuration) {
                    state.hasDiskAnomaly = false;
                    log.warn("✅ [Equipment {}] 디스크 I/O 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
                state.hasDiskAnomaly = true;
                state.diskAnomalyStartTime = currentTime;
                state.diskAnomalyDuration = 25_000 + random.nextInt(75_000);
                log.error("🚨 [Equipment {}] 디스크 I/O 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.diskAnomalyDuration / 1000);
            }

            if (state.hasNetworkAnomaly) {
                if (currentTime - state.networkAnomalyStartTime > state.networkAnomalyDuration) {
                    state.hasNetworkAnomaly = false;
                    log.warn("✅ [Equipment {}] 네트워크 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
                state.hasNetworkAnomaly = true;
                state.networkAnomalyStartTime = currentTime;
                state.networkAnomalyDuration = 40_000 + random.nextInt(100_000);
                log.error("🚨 [Equipment {}] 네트워크 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.networkAnomalyDuration / 1000);
            }
        }

        for (Rack rack : activeRacks) {
            Long rackId = rack.getId();
            AnomalyState state = rackAnomalyStates.get(rackId);

            if (state.hasTemperatureAnomaly) {
                if (currentTime - state.temperatureAnomalyStartTime > state.temperatureAnomalyDuration) {
                    state.hasTemperatureAnomaly = false;
                    log.warn("✅ [Rack {}] 온도 이상 징후 해소!", rackId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
                state.hasTemperatureAnomaly = true;
                state.temperatureAnomalyStartTime = currentTime;
                state.temperatureAnomalyDuration = 50_000 + random.nextInt(150_000);
                log.error("🚨 [Rack {}] 온도 이상 징후 발생! (지속: {}초)",
                        rackId, state.temperatureAnomalyDuration / 1000);
            }

            if (state.hasHumidityAnomaly) {
                if (currentTime - state.humidityAnomalyStartTime > state.humidityAnomalyDuration) {
                    state.hasHumidityAnomaly = false;
                    log.warn("✅ [Rack {}] 습도 이상 징후 해소!", rackId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
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

        boolean hasTemperatureAnomaly = false;
        long temperatureAnomalyStartTime = 0;
        long temperatureAnomalyDuration = 0;

        boolean hasHumidityAnomaly = false;
        long humidityAnomalyStartTime = 0;
        long humidityAnomalyDuration = 0;
    }

    private DiskMetric generateDiskMetric(Long equipmentId, LocalDateTime time) {
        Equipment equipment = activeEquipments.stream()
                .filter(e -> e.getId().equals(equipmentId))
                .findFirst()
                .orElse(null);

        if (equipment == null) {
            return null;
        }

        EquipmentType type = equipment.getType();
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        DiskMetric metric = DiskMetric.builder()
                .equipmentId(equipmentId)
                .generateTime(time)
                .build();

        // ==================== 디스크 용량 ====================
        long totalBytes = 500L * 1024 * 1024 * 1024;  // 500GB
        double baseUsage = 30 + rand.nextDouble() * 40;  // 30~70%
        double usedPercent = Math.min(95, baseUsage);

        long usedBytes = (long) (totalBytes * usedPercent / 100);
        long freeBytes = totalBytes - usedBytes;

        metric.setTotalBytes(totalBytes);
        metric.setUsedBytes(usedBytes);
        metric.setFreeBytes(freeBytes);
        metric.setUsedPercentage(usedPercent);

        // ==================== 장비 유형별 I/O 속도 차별화 ====================
        double baseReadBps, baseWriteBps;

        if (type == EquipmentType.STORAGE) {
            // STORAGE는 I/O가 매우 높음
            baseReadBps = 50_000_000 + rand.nextDouble() * 100_000_000;   // 50~150 MB/s
            baseWriteBps = 30_000_000 + rand.nextDouble() * 70_000_000;   // 30~100 MB/s
        } else {
            // SERVER는 일반적인 수준
            baseReadBps = 5_000_000 + rand.nextDouble() * 10_000_000;     // 5~15 MB/s
            baseWriteBps = 3_000_000 + rand.nextDouble() * 7_000_000;     // 3~10 MB/s
        }

        double ioReadBps = state.hasDiskAnomaly ?
                baseReadBps * (2 + rand.nextDouble() * 3) : baseReadBps;

        double ioWriteBps = state.hasDiskAnomaly ?
                baseWriteBps * (2 + rand.nextDouble() * 3) : baseWriteBps;

        metric.setIoReadBps(ioReadBps);
        metric.setIoWriteBps(ioWriteBps);

        // ==================== I/O 사용률 ====================
        double ioTimePercentage = state.hasDiskAnomaly ?
                Math.min(95, 30 + rand.nextDouble() * 50) : 5 + rand.nextDouble() * 20;

        metric.setIoTimePercentage(ioTimePercentage);

        // ==================== I/O 카운트 (누적) ====================
        String key = "disk_" + equipmentId;

        long prevReadCount = cumulativeIoReads.getOrDefault(key, 0L);
        long prevWriteCount = cumulativeIoWrites.getOrDefault(key, 0L);

        // 15초 간격 동안의 I/O 작업 수
        long readInc = (long) (ioReadBps / 4096 * 15);  // 4KB 블록 가정
        long writeInc = (long) (ioWriteBps / 4096 * 15);

        long newReadCount = prevReadCount + readInc;
        long newWriteCount = prevWriteCount + writeInc;

        cumulativeIoReads.put(key, newReadCount);
        cumulativeIoWrites.put(key, newWriteCount);

        metric.setIoReadCount(newReadCount);
        metric.setIoWriteCount(newWriteCount);

        // ==================== inode ====================
        long totalInodes = 32_000_000L;
        double inodeUsagePercent = 15 + rand.nextDouble() * 30;  // 15~45%

        long usedInodes = (long) (totalInodes * inodeUsagePercent / 100);
        long freeInodes = totalInodes - usedInodes;

        metric.setTotalInodes(totalInodes);
        metric.setUsedInodes(usedInodes);
        metric.setFreeInodes(freeInodes);
        metric.setUsedInodePercentage(inodeUsagePercent);

        return metric;
    }

    /**
     * 랙 기반 환경 메트릭 생성 (수정됨)
     *
     * @param rackId 랙 ID
     * @param time   생성 시간
     * @return EnvironmentMetric
     */
    private EnvironmentMetric generateEnvironmentMetricForRack(Long rackId, LocalDateTime time) {
        if (rackId == null) {
            log.warn("⚠️ rackId가 null입니다.");
            return null;
        }

        AnomalyState state = rackAnomalyStates.get(rackId);
        if (state == null) {
            // 해당 랙에 대한 anomaly 상태가 없으면 생성
            state = new AnomalyState();
            rackAnomalyStates.put(rackId, state);
        }

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        EnvironmentMetric metric = EnvironmentMetric.builder()
                .rackId(rackId)
                .generateTime(time)
                .build();

        // 온도
        double baseTemp = 22 + rand.nextDouble() * 4;  // 22~26°C
        double temperature = state.hasTemperatureAnomaly ?
                Math.min(35, baseTemp + 8 + rand.nextDouble() * 5) : baseTemp;

        metric.setTemperature(temperature);
        metric.setMinTemperature(temperature - rand.nextDouble() * 2);
        metric.setMaxTemperature(temperature + rand.nextDouble() * 2);

        // 습도
        double baseHumidity = 45 + rand.nextDouble() * 10;  // 45~55%
        double humidity = state.hasHumidityAnomaly ?
                Math.min(75, baseHumidity + 15 + rand.nextDouble() * 10) : baseHumidity;

        metric.setHumidity(humidity);
        metric.setMinHumidity(humidity - rand.nextDouble() * 3);
        metric.setMaxHumidity(humidity + rand.nextDouble() * 3);

        log.trace("📊 랙 {} 환경 메트릭: 온도={}, 습도={}", rackId, temperature, humidity);

        return metric;
    }
}