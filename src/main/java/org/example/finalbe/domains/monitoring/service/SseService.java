package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
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

    // 초기 데이터 전송을 위해 Repository 주입
    private final SystemMetricRepository systemMetricRepository;
    private final DiskMetricRepository diskMetricRepository;
    private final NetworkMetricRepository networkMetricRepository;
    private final EnvironmentMetricRepository environmentMetricRepository;

    /**
     * 장비 메트릭 구독 (equipmentId 기준)
     */
    public SseEmitter subscribeEquipment(Long equipmentId) {
        String topic = "equipment-" + equipmentId;
        SseEmitter emitter = createEmitter(topic); // Emitter 생성

        // 구독 즉시 최신 데이터 1건 전송 (빈 화면 방지)
        sendInitialData(equipmentId, emitter);

        return emitter;
    }

    /**
     * 연결된 클라이언트에게 최신 데이터 1건 즉시 전송
     */
    private void sendInitialData(Long equipmentId, SseEmitter emitter) {
        boolean sentFromCache = sendFromCache(equipmentId, emitter);
        if (!sentFromCache) {
            sendFromDatabase(equipmentId, emitter);
        }
        log.info("🚀 [Equipment-{}] 초기 데이터 전송 완료 (cache={} )", equipmentId, sentFromCache);
    }

    private boolean sendFromCache(Long equipmentId, SseEmitter emitter) {
        boolean sent = false;
        if (monitoringMetricCache.getSystemMetric(equipmentId).isPresent()) {
            sent |= emitSafely(emitter, "system", monitoringMetricCache.getSystemMetric(equipmentId).get());
        }
        if (monitoringMetricCache.getDiskMetric(equipmentId).isPresent()) {
            sent |= emitSafely(emitter, "disk", monitoringMetricCache.getDiskMetric(equipmentId).get());
        }
        List<NetworkMetric> networks = monitoringMetricCache.getNetworkMetrics(equipmentId);
        for (NetworkMetric net : networks) {
            sent |= emitSafely(emitter, "network", net);
        }
        return sent;
    }

    private void sendFromDatabase(Long equipmentId, SseEmitter emitter) {
        systemMetricRepository.findLatestByEquipmentId(equipmentId)
                .ifPresent(data -> emitSafely(emitter, "system", data));
        diskMetricRepository.findLatestByEquipmentId(equipmentId)
                .ifPresent(data -> emitSafely(emitter, "disk", data));
        List<NetworkMetric> networks = networkMetricRepository.findLatestByEquipmentId(equipmentId);
        for (NetworkMetric net : networks) {
            emitSafely(emitter, "network", net);
        }
    }

    private boolean emitSafely(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            return true;
        } catch (IOException e) {
            log.warn("초기 {} 데이터 전송 실패", eventName, e);
            return false;
        }
    }

    /**
     * 랙 환경 메트릭 구독 (rackId 기준)
     */
    public SseEmitter subscribeRack(Long rackId) {
        String topic = "rack-" + rackId;
        SseEmitter emitter = createEmitter(topic);
        sendRackInitialData(rackId, emitter);
        return emitter;
    }

    private void sendRackInitialData(Long rackId, SseEmitter emitter) {
        monitoringMetricCache.getEnvironmentMetric(rackId)
                .ifPresent(data -> emitSafely(emitter, "environment", data));
        environmentMetricRepository.findLatestByRackId(rackId)
                .ifPresent(data -> emitSafely(emitter, "environment", data));
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
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE connection established for topic: " + topic));
        } catch (IOException e) {
            log.error("❌ SSE 초기 연결 오류: [{}]", topic, e);
        }

        return emitter;
    }

    /**
     * 장비 구독자들에게 데이터 전송
     */
    @Async
    public void sendToEquipment(Long equipmentId, String eventName, Object data) {
        String topic = "equipment-" + equipmentId;
        sendData(topic, eventName, data);
    }

    /**
     * 랙 구독자들에게 데이터 전송
     */
    @Async
    public void sendToRack(Long rackId, String eventName, Object data) {
        String topic = "rack-" + rackId;
        sendData(topic, eventName, data);
    }

    /**
     * 공통 데이터 전송 로직
     */

    private void sendData(String topic, String eventName, Object data) {
        List<SseEmitter> topicEmitters = this.emitters.get(topic);

        if (topicEmitters == null || topicEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : topicEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.warn("❌ SSE 데이터 전송 실패: [{}], Emitter 제거", topic);
                topicEmitters.remove(emitter);
            }
        }
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    public void sendHeartbeats() {
        emitters.forEach((topic, topicEmitters) -> {
            for (SseEmitter emitter : topicEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .comment("heartbeat")
                            .reconnectTime(5000));
                } catch (IOException e) {
                    log.warn("Heartbeat 실패: {}, emitter 제거", topic);
                    topicEmitters.remove(emitter);
                }
            }
        });
    }
}