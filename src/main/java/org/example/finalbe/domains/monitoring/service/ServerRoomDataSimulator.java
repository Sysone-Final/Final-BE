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

        // DB에서 삭제되지 않은 장비만 로드
        activeEquipments = equipmentRepository.findAll().stream()
                .filter(e -> DelYN.N.equals(e.getDelYn()))
                .collect(java.util.stream.Collectors.toCollection(CopyOnWriteArrayList::new));

        // DB에서 삭제되지 않은 랙만 로드
        activeRacks = rackRepository.findAll().stream()
                .filter(r -> DelYN.N.equals(r.getDelYn()))
                .collect(java.util.stream.Collectors.toCollection(CopyOnWriteArrayList::new));

        log.info("📊 DB에서 로드된 장비 총 개수: {}", activeEquipments.size());
        log.info("📊 DB에서 로드된 랙 총 개수: {}", activeRacks.size());

        if (activeEquipments.isEmpty()) {
            log.warn("⚠️ DB에 장비가 없습니다. 시뮬레이터가 동작하지 않습니다.");
            return;
        }

        for (Equipment equipment : activeEquipments) {
            Long equipmentId = equipment.getId();
            EquipmentType type = equipment.getType();

            if (excludedEquipmentIds.contains(equipmentId)) {
                log.info("⏭️ 장비 ID {}는 실제 Prometheus 데이터 사용 - 더미 생성 제외", equipmentId);
                continue;
            }

            if (DelYN.Y.equals(equipment.getDelYn())) {
                log.info("⏭️ 장비 ID {}는 삭제됨(del_yn=Y) - 더미 생성 제외", equipmentId);
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
                .filter(e -> !DelYN.Y.equals(e.getDelYn()))
                .count();

        log.info("✅ 초기화 완료! {}개 장비(실제 더미 생성 대상) + {}개 랙 모니터링 시작",
                activeCount, activeRacks.size());
    }

    private List<String> generateDefaultNics(EquipmentType type) {
        switch (type) {
            case SERVER:
                return Arrays.asList("eth0", "eth1");
            case SWITCH:
                return Arrays.asList("GigabitEthernet1/0/1", "GigabitEthernet1/0/2",
                        "GigabitEthernet1/0/3", "GigabitEthernet1/0/4");
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

    @Scheduled(fixedDelayString = "${monitoring.simulator.interval-seconds:10}000", initialDelay = 2000)
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
        int processed = 0;
        int alertEvaluationCount = 0;  // ✅ 실제 평가된 알림 수

        try {
            for (Equipment equipment : activeEquipments) {
                Long equipmentId = equipment.getId();
                EquipmentType type = equipment.getType();

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

                    // ✅ 임계값 근처일 때만 알림 평가
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

                    // ✅ 임계값 근처일 때만 알림 평가
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

                            // ✅ 임계값 근처일 때만 알림 평가
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
                        log.debug("  → Network 메트릭 생성 완료 (equipmentId={}, NICs={})",
                                equipmentId, nics.size());
                    }
                }
            }

            // 랙별 환경 메트릭 생성
            for (Rack rack : activeRacks) {
                Long rackId = rack.getId();
                EnvironmentMetric envMetric = generateEnvironmentMetric(rackId, now);
                environmentMetricsToSave.add(envMetric);
                monitoringMetricCache.updateEnvironmentMetric(envMetric);
                sseService.sendToRack(rackId, "environment", envMetric);

                // ✅ 임계값 근처일 때만 알림 평가
                if (needsEnvironmentAlertEvaluation(envMetric, rack)) {
                    try {
                        alertEvaluationService.evaluateEnvironmentMetric(envMetric);
                        alertEvaluationCount++;
                    } catch (Exception e) {
                        log.warn("⚠️ Environment 알림 평가 실패: rackId={}, error={}", rackId, e.getMessage());
                    }
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
            log.info("  - 실제 처리: {}", processed);
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

    // ========== ✅ 새로 추가: 알림 평가 필요 여부 판단 메서드 ==========

    /**
     * System 메트릭 알림 평가 필요 여부 체크
     */
    private boolean needsSystemAlertEvaluation(SystemMetric metric, Equipment equipment) {
        // 모니터링 비활성화면 평가 안 함
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }

        // CPU 체크 (임계값의 80% 이상만 평가)
        if (equipment.getCpuThresholdWarning() != null && metric.getCpuIdle() != null) {
            double cpuUsage = 100.0 - metric.getCpuIdle();
            double threshold = equipment.getCpuThresholdWarning().doubleValue();
            if (cpuUsage >= threshold * 0.8) {
                return true;
            }
        }

        // Memory 체크 (임계값의 80% 이상만 평가)
        if (equipment.getMemoryThresholdWarning() != null &&
                metric.getUsedMemoryPercentage() != null) {
            double threshold = equipment.getMemoryThresholdWarning().doubleValue();
            if (metric.getUsedMemoryPercentage() >= threshold * 0.8) {
                return true;
            }
        }

        return false;
    }

    /**
     * Disk 메트릭 알림 평가 필요 여부 체크
     */
    private boolean needsDiskAlertEvaluation(DiskMetric metric, Equipment equipment) {
        // 모니터링 비활성화면 평가 안 함
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }

        // Disk 사용률 체크 (임계값의 80% 이상만 평가)
        if (equipment.getDiskThresholdWarning() != null &&
                metric.getUsedPercentage() != null) {
            double threshold = equipment.getDiskThresholdWarning().doubleValue();
            if (metric.getUsedPercentage() >= threshold * 0.8) {
                return true;
            }
        }

        return false;
    }

    /**
     * Network 메트릭 알림 평가 필요 여부 체크
     */
    private boolean needsNetworkAlertEvaluation(NetworkMetric metric, Equipment equipment) {
        // 모니터링 비활성화면 평가 안 함
        if (!Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
            return false;
        }

        // 대역폭 사용률 체크 (임계값 80% 기준)
        double bandwidthWarning = 80.0;  // AlertEvaluationService의 기본값

        // RX 사용률 체크
        if (metric.getRxUsage() != null && metric.getRxUsage() >= bandwidthWarning * 0.8) {
            return true;
        }

        // TX 사용률 체크
        if (metric.getTxUsage() != null && metric.getTxUsage() >= bandwidthWarning * 0.8) {
            return true;
        }

        // 에러율 체크 (0.1% 이상이면 평가 필요)
        if (metric.getInErrorPktsTot() != null && metric.getInPktsTot() != null &&
                metric.getInPktsTot() > 0) {
            double errorRate = (metric.getInErrorPktsTot() * 100.0) / metric.getInPktsTot();
            if (errorRate >= 0.08) {  // Warning 임계값(0.1%)의 80%
                return true;
            }
        }

        if (metric.getOutErrorPktsTot() != null && metric.getOutPktsTot() != null &&
                metric.getOutPktsTot() > 0) {
            double errorRate = (metric.getOutErrorPktsTot() * 100.0) / metric.getOutPktsTot();
            if (errorRate >= 0.08) {
                return true;
            }
        }

        // 드롭율 체크 (0.1% 이상이면 평가 필요)
        if (metric.getInDiscardPktsTot() != null && metric.getInPktsTot() != null &&
                metric.getInPktsTot() > 0) {
            double dropRate = (metric.getInDiscardPktsTot() * 100.0) / metric.getInPktsTot();
            if (dropRate >= 0.08) {
                return true;
            }
        }

        if (metric.getOutDiscardPktsTot() != null && metric.getOutPktsTot() != null &&
                metric.getOutPktsTot() > 0) {
            double dropRate = (metric.getOutDiscardPktsTot() * 100.0) / metric.getOutPktsTot();
            if (dropRate >= 0.08) {
                return true;
            }
        }

        return false;
    }

    /**
     * Environment 메트릭 알림 평가 필요 여부 체크
     */
    private boolean needsEnvironmentAlertEvaluation(EnvironmentMetric metric, Rack rack) {
        // 모니터링 비활성화면 평가 안 함
        if (!Boolean.TRUE.equals(rack.getMonitoringEnabled())) {
            return false;
        }

        // 온도 체크 (임계값의 90% 이상만 평가)
        if (rack.getTemperatureThresholdWarning() != null &&
                metric.getTemperature() != null) {
            double threshold = rack.getTemperatureThresholdWarning().doubleValue();
            if (metric.getTemperature() >= threshold * 0.9) {
                return true;
            }
        }

        // 습도 최소값 체크 (임계값의 110% 이하만 평가)
        if (rack.getHumidityThresholdMinWarning() != null &&
                metric.getHumidity() != null) {
            double threshold = rack.getHumidityThresholdMinWarning().doubleValue();
            if (metric.getHumidity() <= threshold * 1.1) {
                return true;
            }
        }

        // 습도 최대값 체크 (임계값의 90% 이상만 평가)
        if (rack.getHumidityThresholdMaxWarning() != null &&
                metric.getHumidity() != null) {
            double threshold = rack.getHumidityThresholdMaxWarning().doubleValue();
            if (metric.getHumidity() >= threshold * 0.9) {
                return true;
            }
        }

        return false;
    }

    // ========== 기존 메서드들 ==========

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
                "rx_usage, tx_usage, in_pkts_tot, out_pkts_tot, " +
                "in_error_pkts_tot, out_error_pkts_tot, in_discard_pkts_tot, out_discard_pkts_tot, " +
                "oper_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, metrics, metrics.size(),
                (ps, metric) -> {
                    ps.setLong(1, metric.getEquipmentId());
                    ps.setString(2, metric.getNicName());
                    ps.setObject(3, metric.getGenerateTime());
                    ps.setObject(4, metric.getRxUsage());
                    ps.setObject(5, metric.getTxUsage());
                    ps.setObject(6, metric.getInPktsTot());
                    ps.setObject(7, metric.getOutPktsTot());
                    ps.setObject(8, metric.getInErrorPktsTot());
                    ps.setObject(9, metric.getOutErrorPktsTot());
                    ps.setObject(10, metric.getInDiscardPktsTot());
                    ps.setObject(11, metric.getOutDiscardPktsTot());
                    ps.setObject(12, metric.getOperStatus());
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

    private SystemMetric generateSystemMetric(Long equipmentId, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        SystemMetric metric = SystemMetric.builder()
                .equipmentId(equipmentId)
                .generateTime(time)
                .build();

        double baseCpu = 15 + rand.nextDouble() * 20;
        double cpuUsage = state.hasCpuAnomaly ?
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

        double loadAvg = cpuUsage / 100.0 * 4;
        metric.setLoadAvg1(loadAvg + rand.nextDouble() * 0.5);
        metric.setLoadAvg5(loadAvg + rand.nextDouble() * 0.3);
        metric.setLoadAvg15(loadAvg + rand.nextDouble() * 0.2);

        long contextSwitches = (long)(1000 + rand.nextDouble() * 9000);
        metric.setContextSwitches(contextSwitches);

        long totalMemory = 16L * 1024 * 1024 * 1024;
        double baseMemUsage = 40 + rand.nextDouble() * 20;
        double memUsagePercent = state.hasMemoryAnomaly ?
                Math.min(95, baseMemUsage + 30 + rand.nextDouble() * 15) : baseMemUsage;

        long usedMemory = (long)(totalMemory * memUsagePercent / 100);
        long freeMemory = totalMemory - usedMemory;

        metric.setTotalMemory(totalMemory);
        metric.setUsedMemory(usedMemory);
        metric.setFreeMemory(freeMemory);
        metric.setUsedMemoryPercentage(memUsagePercent);

        long buffers = (long)(totalMemory * 0.05);
        long cached = (long)(totalMemory * 0.15);
        long active = (long)(usedMemory * 0.6);
        long inactive = (long)(usedMemory * 0.4);

        metric.setMemoryBuffers(buffers);
        metric.setMemoryCached(cached);
        metric.setMemoryActive(active);
        metric.setMemoryInactive(inactive);

        long totalSwap = 8L * 1024 * 1024 * 1024;
        double swapUsagePercent = state.hasMemoryAnomaly ?
                rand.nextDouble() * 50 : rand.nextDouble() * 5;

        long usedSwap = (long)(totalSwap * swapUsagePercent / 100);

        metric.setTotalSwap(totalSwap);
        metric.setUsedSwap(usedSwap);
        metric.setUsedSwapPercentage(swapUsagePercent);

        return metric;
    }

    private DiskMetric generateDiskMetric(Long equipmentId, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        DiskMetric metric = DiskMetric.builder()
                .equipmentId(equipmentId)
                .generateTime(time)
                .build();

        long totalBytes = 500L * 1024 * 1024 * 1024;
        double baseUsage = 30 + rand.nextDouble() * 40;
        double usedPercent = Math.min(95, baseUsage);

        long usedBytes = (long)(totalBytes * usedPercent / 100);
        long freeBytes = totalBytes - usedBytes;

        metric.setTotalBytes(totalBytes);
        metric.setUsedBytes(usedBytes);
        metric.setFreeBytes(freeBytes);
        metric.setUsedPercentage(usedPercent);

        double baseReadBps = 5_000_000 + rand.nextDouble() * 10_000_000;
        double baseWriteBps = 3_000_000 + rand.nextDouble() * 7_000_000;

        double ioReadBps = state.hasDiskAnomaly ?
                baseReadBps * (2 + rand.nextDouble() * 3) : baseReadBps;

        double ioWriteBps = state.hasDiskAnomaly ?
                baseWriteBps * (2 + rand.nextDouble() * 3) : baseWriteBps;

        metric.setIoReadBps(ioReadBps);
        metric.setIoWriteBps(ioWriteBps);

        double ioTimePercentage = state.hasDiskAnomaly ?
                Math.min(95, 30 + rand.nextDouble() * 50) : 5 + rand.nextDouble() * 20;

        metric.setIoTimePercentage(ioTimePercentage);

        String key = "disk_" + equipmentId;

        long prevReadCount = cumulativeIoReads.getOrDefault(key, 0L);
        long prevWriteCount = cumulativeIoWrites.getOrDefault(key, 0L);

        long readInc = (long)(ioReadBps / 4096 * 15);
        long writeInc = (long)(ioWriteBps / 4096 * 15);

        long newReadCount = prevReadCount + readInc;
        long newWriteCount = prevWriteCount + writeInc;

        cumulativeIoReads.put(key, newReadCount);
        cumulativeIoWrites.put(key, newWriteCount);

        metric.setIoReadCount(newReadCount);
        metric.setIoWriteCount(newWriteCount);

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

    private NetworkMetric generateNetworkMetric(Long equipmentId, String nicName, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(equipmentId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        NetworkMetric metric = NetworkMetric.builder()
                .equipmentId(equipmentId)
                .nicName(nicName)
                .generateTime(time)
                .build();

        double bandwidthBps = 1_000_000_000.0;

        double baseRxUsage = 5 + rand.nextDouble() * 15;
        double baseTxUsage = 3 + rand.nextDouble() * 12;

        double rxUsage = state.hasNetworkAnomaly ?
                Math.min(95, baseRxUsage + 50 + rand.nextDouble() * 25) : baseRxUsage;

        double txUsage = state.hasNetworkAnomaly ?
                Math.min(95, baseTxUsage + 45 + rand.nextDouble() * 25) : baseTxUsage;

        metric.setRxUsage(rxUsage);
        metric.setTxUsage(txUsage);

        String key = "network_" + equipmentId + "_" + nicName;

        long prevInPackets = cumulativeInPackets.getOrDefault(key, 0L);
        long prevOutPackets = cumulativeOutPackets.getOrDefault(key, 0L);

        long inPacketsInc = (long)(bandwidthBps * rxUsage / 100.0 / 1500 * 5);
        long outPacketsInc = (long)(bandwidthBps * txUsage / 100.0 / 1500 * 5);

        long newInPackets = prevInPackets + inPacketsInc;
        long newOutPackets = prevOutPackets + outPacketsInc;

        cumulativeInPackets.put(key, newInPackets);
        cumulativeOutPackets.put(key, newOutPackets);

        metric.setInPktsTot(newInPackets);
        metric.setOutPktsTot(newOutPackets);

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

    private EnvironmentMetric generateEnvironmentMetric(Long rackId, LocalDateTime time) {
        AnomalyState state = rackAnomalyStates.get(rackId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        EnvironmentMetric metric = EnvironmentMetric.builder()
                .rackId(rackId)
                .generateTime(time)
                .build();

        double baseTemperature = 20.0 + rand.nextDouble() * 4.0;

        double currentTemperature;
        if (state.hasTemperatureAnomaly) {
            currentTemperature = Math.min(45.0, baseTemperature + 10 + rand.nextDouble() * 8);
        } else {
            currentTemperature = baseTemperature;
        }

        metric.setTemperature(currentTemperature);

        Double prevMinTemp = minTemperatureTracker.get(rackId);
        Double prevMaxTemp = maxTemperatureTracker.get(rackId);

        double minTemp = Math.min(prevMinTemp, currentTemperature);
        double maxTemp = Math.max(prevMaxTemp, currentTemperature);

        minTemperatureTracker.put(rackId, minTemp);
        maxTemperatureTracker.put(rackId, maxTemp);

        metric.setMinTemperature(minTemp);
        metric.setMaxTemperature(maxTemp);

        double baseHumidity = 40.0 + rand.nextDouble() * 15.0;

        double currentHumidity;
        if (state.hasHumidityAnomaly) {
            if (rand.nextBoolean()) {
                currentHumidity = Math.max(20.0, baseHumidity - 15 - rand.nextDouble() * 10);
            } else {
                currentHumidity = Math.min(80.0, baseHumidity + 15 + rand.nextDouble() * 15);
            }
        } else {
            currentHumidity = baseHumidity;
        }

        metric.setHumidity(currentHumidity);

        Double prevMinHumidity = minHumidityTracker.get(rackId);
        Double prevMaxHumidity = maxHumidityTracker.get(rackId);

        double minHumidity = Math.min(prevMinHumidity, currentHumidity);
        double maxHumidity = Math.max(prevMaxHumidity, currentHumidity);

        minHumidityTracker.put(rackId, minHumidity);
        maxHumidityTracker.put(rackId, maxHumidity);

        metric.setMinHumidity(minHumidity);
        metric.setMaxHumidity(maxHumidity);

        boolean tempWarning = currentTemperature > 28.0;
        boolean humidityWarning = currentHumidity < 35.0 || currentHumidity > 65.0;

        metric.setTemperatureWarning(tempWarning);
        metric.setHumidityWarning(humidityWarning);

        return metric;
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

            // CPU 이상 징후 (약 1시간에 한 번)
            if (state.hasCpuAnomaly) {
                if (currentTime - state.cpuAnomalyStartTime > state.cpuAnomalyDuration) {
                    state.hasCpuAnomaly = false;
                    log.warn("✅ [Equipment {}] CPU 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
                state.hasCpuAnomaly = true;
                state.cpuAnomalyStartTime = currentTime;
                state.cpuAnomalyDuration = 60_000 + random.nextInt(120_000);
                log.error("🚨 [Equipment {}] CPU 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.cpuAnomalyDuration / 1000);
            }

            // 메모리 이상 징후 (약 1시간에 한 번)
            if (state.hasMemoryAnomaly) {
                if (currentTime - state.memoryAnomalyStartTime > state.memoryAnomalyDuration) {
                    state.hasMemoryAnomaly = false;
                    log.warn("✅ [Equipment {}] 메모리 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
                state.hasMemoryAnomaly = true;
                state.memoryAnomalyStartTime = currentTime;
                state.memoryAnomalyDuration = 40_000 + random.nextInt(80_000);
                log.error("🚨 [Equipment {}] 메모리 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.memoryAnomalyDuration / 1000);
            }

            // 디스크 I/O 이상 징후 (약 1시간에 한 번)
            if (state.hasDiskAnomaly) {
                if (currentTime - state.diskAnomalyStartTime > state.diskAnomalyDuration) {
                    state.hasDiskAnomaly = false;
                    log.warn("✅ [Equipment {}] 디스크 I/O 이상 징후 해소!", equipmentId);
                }
            } else if (random.nextDouble() < HOURLY_PROBABILITY) {
                state.hasDiskAnomaly = true;
                state.diskAnomalyStartTime = currentTime;
                state.diskAnomalyDuration = 30_000 + random.nextInt(90_000);
                log.error("🚨 [Equipment {}] 디스크 I/O 이상 징후 발생! (지속: {}초)",
                        equipmentId, state.diskAnomalyDuration / 1000);
            }

            // 네트워크 이상 징후 (약 1시간에 한 번)
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

        // 랙별 환경 이상 징후 (약 1시간에 한 번)
        for (Rack rack : activeRacks) {
            Long rackId = rack.getId();
            AnomalyState state = rackAnomalyStates.get(rackId);

            // 온도 이상 징후 (약 1시간에 한 번)
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

            // 습도 이상 징후 (약 1시간에 한 번)
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

    public void addEquipment(Equipment newEquipment) {
        this.activeEquipments.add(newEquipment);

        Long equipmentId = newEquipment.getId();
        EquipmentType type = newEquipment.getType();

        if (excludedEquipmentIds.contains(equipmentId)) {
            log.info("⏭️ 장비 ID {}는 실제 Prometheus 데이터 사용 - 시뮬레이터 등록 제외", equipmentId);
            return;
        }

        if (DelYN.Y.equals(newEquipment.getDelYn())) {
            log.info("⏭️ 장비 ID {}는 삭제됨(del_yn=Y) - 시뮬레이터 등록 제외", equipmentId);
            return;
        }

        if (hasNetworkMetric(type)) {
            EQUIPMENT_NICS.put(equipmentId, generateDefaultNics(type));
        }

        anomalyStates.put(equipmentId, new AnomalyState());

        log.info("🆕 새 장비 시뮬레이터 등록 완료: ID={}, Name={}", equipmentId, newEquipment.getName());
    }
}