package org.example.finalbe.domains.monitoring.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.example.finalbe.domains.monitoring.dto.DataCenterStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.RackStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.ServerRoomStatisticsDto;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {

    private final MonitoringMetricCache monitoringMetricCache;

    // 구독자 관리 맵 (ConcurrentHashMap: 스레드 안전)
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000; // 1시간
    private static final long HEARTBEAT_INTERVAL_MS = 30_000;

    private final SystemMetricRepository systemMetricRepository;
    private final DiskMetricRepository diskMetricRepository;
    private final NetworkMetricRepository networkMetricRepository;
    private final EnvironmentMetricRepository environmentMetricRepository;
    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;

    private final ServerRoomMonitoringService serverRoomMonitoringService;
    private final DataCenterMonitoringService dataCenterMonitoringService;
    private final RackMonitoringService rackMonitoringService;

    /**
     * 장비 메트릭 구독 (equipmentId 기준)
     */
    public SseEmitter subscribeEquipment(Long equipmentId) {
        String topic = "equipment-" + equipmentId;
        SseEmitter emitter = createEmitter(topic);

        // 비동기로 초기 데이터 전송
        asyncSendInitialData(equipmentId, emitter);

        return emitter;
    }

    /**
     * 비동기로 초기 데이터 전송
     * ✅ DB 조회와 SSE 전송을 분리하여 커넥션 누수 방지
     */
    @Async("taskExecutor")
    void asyncSendInitialData(Long equipmentId, SseEmitter emitter) {
        try {
            Equipment equipment = equipmentRepository.findByIdWithRackAndServerRoom(equipmentId)
                    .orElse(null);

            Long rackId = null;
            if (equipment != null && equipment.getRack() != null) {
                rackId = equipment.getRack().getId();
            }

            boolean sentFromCache = sendFromCache(equipmentId, rackId, emitter);
            if (!sentFromCache) {
                // ✅ DB 조회를 별도 메서드로 분리 (트랜잭션 범위 축소)
                InitialMetricData data = loadInitialDataFromDatabase(equipmentId, rackId);
                sendInitialData(emitter, data);
            }
            log.info("🚀 [Equipment-{}] 초기 데이터 전송 완료 (RackID: {})", equipmentId, rackId);
        } catch (Exception e) {
            log.error("❌ [Equipment-{}] 초기 데이터 전송 실패", equipmentId, e);
        }
    }

    /**
     * ✅ DB에서 데이터 조회만 수행 (트랜잭션 범위 최소화)
     */
    @Transactional(readOnly = true)
    InitialMetricData loadInitialDataFromDatabase(Long equipmentId, Long rackId) {
        InitialMetricData data = new InitialMetricData();

        // System
        systemMetricRepository.findLatestByEquipmentId(equipmentId)
                .ifPresent(data::setSystemMetric);

        // Disk
        diskMetricRepository.findLatestByEquipmentId(equipmentId)
                .ifPresent(data::setDiskMetric);

        // Network
        List<NetworkMetric> networks = networkMetricRepository.findLatestByEquipmentId(equipmentId);
        data.setNetworkMetrics(networks);

        // Environment
        if (rackId != null) {
            environmentMetricRepository.findLatestByRackId(rackId)
                    .ifPresent(data::setEnvironmentMetric);
        }

        return data;
    }

    /**
     * ✅ 조회된 데이터를 SSE로 전송 (DB 커넥션 없이 수행)
     */
    private void sendInitialData(SseEmitter emitter, InitialMetricData data) {
        if (data.getSystemMetric() != null) {
            emitSafely(emitter, "system", data.getSystemMetric());
        }
        if (data.getDiskMetric() != null) {
            emitSafely(emitter, "disk", data.getDiskMetric());
        }
        if (data.getNetworkMetrics() != null && !data.getNetworkMetrics().isEmpty()) {
            emitSafely(emitter, "network", data.getNetworkMetrics());
        }
        if (data.getEnvironmentMetric() != null) {
            emitSafely(emitter, "environment", data.getEnvironmentMetric());
        }
    }

    /**
     * ✅ 초기 데이터 DTO (DB 조회 결과를 담는 객체)
     */
    @Data
    private static class InitialMetricData {
        private SystemMetric systemMetric;
        private DiskMetric diskMetric;
        private List<NetworkMetric> networkMetrics;
        private EnvironmentMetric environmentMetric;
    }

    /**
     * Cache에서 데이터 전송
     * emitSafely가 boolean을 반환하므로 '|=' 연산 사용 가능
     */
    private boolean sendFromCache(Long equipmentId, Long rackId, SseEmitter emitter) {
        boolean sent = false;
        // System
        if (monitoringMetricCache.getSystemMetric(equipmentId).isPresent()) {
            sent |= emitSafely(emitter, "system", monitoringMetricCache.getSystemMetric(equipmentId).get());
        }
        // Disk
        if (monitoringMetricCache.getDiskMetric(equipmentId).isPresent()) {
            sent |= emitSafely(emitter, "disk", monitoringMetricCache.getDiskMetric(equipmentId).get());
        }
        // Network: 리스트 전체를 한 번에 전송
        List<NetworkMetric> networks = monitoringMetricCache.getNetworkMetrics(equipmentId);
        if (!networks.isEmpty()) {
            sent |= emitSafely(emitter, "network", networks);
        }
        // Environment: Rack ID가 있으면 환경 정보도 전송
        if (rackId != null && monitoringMetricCache.getEnvironmentMetric(rackId).isPresent()) {
            sent |= emitSafely(emitter, "environment", monitoringMetricCache.getEnvironmentMetric(rackId).get());
        }
        return sent;
    }

    /**
     * [중요] void 버전을 삭제하고 boolean 반환 버전만 남김
     * 성공 시 true, 실패 시 false 반환
     */
    private boolean emitSafely(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            return true; // 전송 성공
        } catch (IOException e) {
            log.warn("SSE 초기 데이터 전송 실패: {}", eventName, e);
            return false; // 전송 실패
        }
    }

    /**
     * 랙 환경 메트릭 및 통계 구독 (rackId 기준)
     * ✅ 초기 데이터: rack-statistics만 전송 (environment 포함)
     * ✅ 실시간: rack-statistics만 전송 (environment 중복 제거)
     */
    public SseEmitter subscribeRack(Long rackId) {
        String topic = "rack-" + rackId;
        SseEmitter emitter = createEmitter(topic);
        asyncSendRackInitialData(rackId, emitter);
        return emitter;
    }

    @Async("taskExecutor")
    void asyncSendRackInitialData(Long rackId, SseEmitter emitter) {
        try {
            // ✅ 변경: rack-statistics만 전송 (environment 정보 포함)
            monitoringMetricCache.getRackStatistics(rackId)
                    .ifPresent(data -> emitSafely(emitter, "rack-statistics", data));

            // ✅ 캐시에 없으면 새로 계산
            if (monitoringMetricCache.getRackStatistics(rackId).isEmpty()) {
                try {
                    Rack rack = rackRepository.findById(rackId).orElse(null);
                    if (rack != null) {
                        RackStatisticsDto statistics = calculateRackStatisticsForInitialData(rackId);
                        emitSafely(emitter, "rack-statistics", statistics);
                        log.info("🚀 [Rack-{}] 초기 통계 데이터 계산 및 전송 완료", rackId);
                    }
                } catch (Exception e) {
                    log.error("❌ [Rack-{}] 초기 통계 계산 실패", rackId, e);
                }
            } else {
                log.info("🚀 [Rack-{}] 초기 통계 데이터 전송 완료 (캐시에서)", rackId);
            }
        } catch (Exception e) {
            log.error("❌ [Rack-{}] 초기 데이터 전송 실패", rackId, e);
        }
    }

    /**
     * 초기 데이터 전송용 랙 통계 계산
     * RackMonitoringService를 직접 호출하여 계산
     */
    private RackStatisticsDto calculateRackStatisticsForInitialData(Long rackId) {
        // RackMonitoringService를 직접 사용하여 전체 통계 계산
        return rackMonitoringService.calculateRackStatistics(rackId);
    }

    /**
     * 공통 Emitter 생성 및 등록 로직
     */
    private SseEmitter createEmitter(String topic) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        this.emitters.putIfAbsent(topic, new CopyOnWriteArrayList<>());
        this.emitters.get(topic).add(emitter);

        log.info("✅ SSE 구독 시작: [{}], 현재 구독자 수: {}", topic, this.emitters.get(topic).size());

        emitter.onTimeout(() -> {
            log.warn("⚠️ SSE 타임아웃: [{}]", topic);
            this.emitters.get(topic).remove(emitter);
        });
        emitter.onCompletion(() -> {
            log.info("🔌 SSE 연결 종료: [{}]", topic);
            this.emitters.get(topic).remove(emitter);
        });

        try {
            // ✅ 즉시 comment를 보내서 연결 수립 (데이터 없이 연결만 열림)
            emitter.send(SseEmitter.event()
                    .comment("connected")
                    .reconnectTime(5000));
            log.debug("📡 SSE 연결 수립 완료: [{}]", topic);
        } catch (IOException e) {
            log.error("❌ SSE 초기 연결 오류: [{}]", topic, e);
            this.emitters.get(topic).remove(emitter);
            throw new RuntimeException("SSE 연결 실패: " + topic, e);
        }

        return emitter;
    }

    public void sendToEquipment(Long equipmentId, String eventName, Object data) {
        String topic = "equipment-" + equipmentId;
        if (!hasSubscribers(topic)) {
            return;
        }
        asyncSend(topic, eventName, data);
    }

    /**
     * 랙에 통계 데이터 전송
     */
    public void sendToRack(Long rackId, String eventName, Object data) {
        String topic = "rack-" + rackId;
        if (!hasSubscribers(topic)) {
            return;
        }
        asyncSend(topic, eventName, data);
    }

    @Async("taskExecutor")
    void asyncSend(String topic, String eventName, Object data) {
        sendData(topic, eventName, data);
    }

    /**
     * ✅ public 메서드로 변경 - 스케줄러에서 사용
     */
    public boolean hasSubscribers(String topic) {
        List<SseEmitter> topicEmitters = this.emitters.get(topic);
        return topicEmitters != null && !topicEmitters.isEmpty();
    }

    private void sendData(String topic, String eventName, Object data) {
        List<SseEmitter> topicEmitters = this.emitters.get(topic);

        if (topicEmitters == null || topicEmitters.isEmpty()) {
            return;
        }

        topicEmitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                return false;
            } catch (IOException e) {
                log.warn("❌ SSE 데이터 전송 실패: [{}], Emitter 제거", topic);
                return true;
            }
        });

        // 빈 리스트가 된 경우 topic 자체를 제거하여 메모리 누수 방지
        if (topicEmitters.isEmpty()) {
            this.emitters.remove(topic);
            log.debug("🗑️ 구독자가 없어 topic [{}] 제거", topic);
        }
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeats() {
        // ✅ 구독자가 없으면 스킵하여 CPU 사용률 감소
        if (emitters.isEmpty()) {
            return;
        }

        emitters.forEach((topic, topicEmitters) -> {
            int removed = topicEmitters.size();
            topicEmitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .comment("heartbeat")
                            .reconnectTime(5000));
                    return false;
                } catch (IOException e) {
                    log.debug("⚠️ Heartbeat 실패: {}", topic);
                    return true;
                }
            });
            removed -= topicEmitters.size();

            if (removed > 0) {
                log.warn("⚠️ Heartbeat 실패: {} - {}개 구독자 제거됨", topic, removed);
            }

            if (topicEmitters.isEmpty()) {
                emitters.remove(topic);
            }
        });
    }

    /**
     * 서버실 통계 구독 (serverRoomId 기준)
     */
    public SseEmitter subscribeServerRoom(Long serverRoomId) {
        String topic = "serverroom-" + serverRoomId;
        SseEmitter emitter = createEmitter(topic);

        // 비동기로 초기 데이터 전송
        asyncSendServerRoomInitialData(serverRoomId, emitter);

        return emitter;
    }

    @Async("taskExecutor")
    void asyncSendServerRoomInitialData(Long serverRoomId, SseEmitter emitter) {
        try {
            // ServerRoomMonitoringService를 통해 초기 통계 계산
            ServerRoomStatisticsDto initialStats = serverRoomMonitoringService.calculateServerRoomStatistics(serverRoomId);
            emitSafely(emitter, "serverroom-statistics", initialStats);
            log.info("🚀 [ServerRoom-{}] 초기 통계 데이터 전송 완료", serverRoomId);
        } catch (Exception e) {
            log.error("❌ [ServerRoom-{}] 초기 통계 데이터 전송 실패", serverRoomId, e);
        }
    }

    /**
     * 데이터센터 통계 구독 (dataCenterId 기준)
     */
    public SseEmitter subscribeDataCenter(Long dataCenterId) {
        String topic = "datacenter-" + dataCenterId;
        SseEmitter emitter = createEmitter(topic);

        // 비동기로 초기 데이터 전송
        asyncSendDataCenterInitialData(dataCenterId, emitter);

        return emitter;
    }

    @Async("taskExecutor")
    void asyncSendDataCenterInitialData(Long dataCenterId, SseEmitter emitter) {
        try {
            // DataCenterMonitoringService를 통해 초기 통계 계산
            DataCenterStatisticsDto initialStats = dataCenterMonitoringService.calculateDataCenterStatistics(dataCenterId);
            emitSafely(emitter, "datacenter-statistics", initialStats);
            log.info("🚀 [DataCenter-{}] 초기 통계 데이터 전송 완료", dataCenterId);
        } catch (Exception e) {
            log.error("❌ [DataCenter-{}] 초기 통계 데이터 전송 실패", dataCenterId, e);
        }
    }

    /**
     * 서버실에 통계 데이터 전송
     */
    public void sendToServerRoom(Long serverRoomId, String eventName, Object data) {
        String topic = "serverroom-" + serverRoomId;
        if (!hasSubscribers(topic)) {
            return;
        }
        asyncSend(topic, eventName, data);
    }

    /**
     * 데이터센터에 통계 데이터 전송
     */
    public void sendToDataCenter(Long dataCenterId, String eventName, Object data) {
        String topic = "datacenter-" + dataCenterId;
        if (!hasSubscribers(topic)) {
            return;
        }
        asyncSend(topic, eventName, data);
    }
}