package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor; // [1. 이 부분이 추가되어야 합니다]
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric; // [2. 도메인 import 추가]
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository; // [3. 리포지토리 import 추가]
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository; // [3. 리포지토리 import 추가]
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {

    // 1. 구독자 관리 맵 (ConcurrentHashMap: 스레드 안전)
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000; // 1시간

    // 초기 데이터 전송을 위해 Repository 주입
    private final SystemMetricRepository systemMetricRepository;
    private final DiskMetricRepository diskMetricRepository;
    private final NetworkMetricRepository networkMetricRepository;

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
        try {
            // 1. SystemMetric 최신값 조회 & 전송
            systemMetricRepository.findLatestByEquipmentId(equipmentId)
                    .ifPresent(data -> {
                        try {
                            emitter.send(SseEmitter.event().name("system").data(data));
                        } catch (IOException e) {
                            log.warn("초기 system 데이터 전송 실패");
                        }
                    });

            // 2. DiskMetric 최신값 조회 & 전송
            diskMetricRepository.findLatestByEquipmentId(equipmentId)
                    .ifPresent(data -> {
                        try {
                            emitter.send(SseEmitter.event().name("disk").data(data));
                        } catch (IOException e) {
                            log.warn("초기 disk 데이터 전송 실패");
                        }
                    });

            // 3. NetworkMetric 최신값 조회 & 전송 (리스트)
            List<NetworkMetric> networks = networkMetricRepository.findLatestByEquipmentId(equipmentId);
            for (NetworkMetric net : networks) {
                try {
                    emitter.send(SseEmitter.event().name("network").data(net));
                } catch (IOException e) {
                    log.warn("초기 network 데이터 전송 실패");
                }
            }

            log.info("🚀 [Equipment-{}] 초기 데이터 전송 완료", equipmentId);

        } catch (Exception e) {
            log.warn("⚠️ 초기 데이터 전송 중 오류 (무시 가능): {}", e.getMessage());
        }
    }

    /**
     * 랙 환경 메트릭 구독 (rackId 기준)
     */
    public SseEmitter subscribeRack(Long rackId) {
        String topic = "rack-" + rackId;
        return createEmitter(topic);
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
    public void sendToEquipment(Long equipmentId, String eventName, Object data) {
        String topic = "equipment-" + equipmentId;
        sendData(topic, eventName, data);
    }

    /**
     * 랙 구독자들에게 데이터 전송
     */
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
}