package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.dto.DataCenterStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.ServerRoomStatisticsDto;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {

    private final MonitoringMetricCache monitoringMetricCache;

    // 1. 구독자 관리 맵 (ConcurrentHashMap: 스레드 안전)
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000; // 1시간
    private static final long HEARTBEAT_INTERVAL_MS = 30_000;

    private final SystemMetricRepository systemMetricRepository;
    private final DiskMetricRepository diskMetricRepository;
    private final NetworkMetricRepository networkMetricRepository;
    private final EnvironmentMetricRepository environmentMetricRepository;
    private final EquipmentRepository equipmentRepository;

    private final ServerRoomMonitoringService serverRoomMonitoringService;
    private final DataCenterMonitoringService dataCenterMonitoringService;

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
                sendFromDatabase(equipmentId, rackId, emitter);
            }
            log.info("🚀 [Equipment-{}] 초기 데이터 전송 완료 (RackID: {})", equipmentId, rackId);
        } catch (Exception e) {
            log.error("❌ [Equipment-{}] 초기 데이터 전송 실패", equipmentId, e);
        }
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
     * DB에서 데이터 전송
     */
    private void sendFromDatabase(Long equipmentId, Long rackId, SseEmitter emitter) {
        // System
        systemMetricRepository.findLatestByEquipmentId(equipmentId)
                .ifPresent(data -> emitSafely(emitter, "system", data));
        // Disk
        diskMetricRepository.findLatestByEquipmentId(equipmentId)
                .ifPresent(data -> emitSafely(emitter, "disk", data));

        // Network: 리스트 전체를 한 번에 전송
        List<NetworkMetric> networks = networkMetricRepository.findLatestByEquipmentId(equipmentId);
        if (!networks.isEmpty()) {
            emitSafely(emitter, "network", networks);
        }

        // Environment: Rack ID로 조회하여 전송
        if (rackId != null) {
            environmentMetricRepository.findLatestByRackId(rackId)
                    .ifPresent(data -> emitSafely(emitter, "environment", data));
        }
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
     * 랙 환경 메트릭 구독 (rackId 기준)
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
            monitoringMetricCache.getEnvironmentMetric(rackId)
                    .ifPresent(data -> emitSafely(emitter, "environment", data));
            if (monitoringMetricCache.getEnvironmentMetric(rackId).isEmpty()) {
                environmentMetricRepository.findLatestByRackId(rackId)
                        .ifPresent(data -> emitSafely(emitter, "environment", data));
            }
            log.info("🚀 [Rack-{}] 초기 데이터 전송 완료", rackId);
        } catch (Exception e) {
            log.error("❌ [Rack-{}] 초기 데이터 전송 실패", rackId, e);
        }
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

    private boolean hasSubscribers(String topic) {
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
        emitters.forEach((topic, topicEmitters) -> {
            int removed = topicEmitters.size();
            topicEmitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .comment("heartbeat")
                            .reconnectTime(5000));
                    return false;
                } catch (IOException e) {
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