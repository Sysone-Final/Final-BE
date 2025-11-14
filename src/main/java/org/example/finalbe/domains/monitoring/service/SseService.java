package org.example.finalbe.domains.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseService {

    // 1. 구독자 관리 맵 (ConcurrentHashMap: 스레드 안전)
    // Key: "equipment-1" 또는 "rack-1"과 같은 구독 주제(Topic)
    // Value: 해당 주제를 구독하는 클라이언트(Emitter) 리스트
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000; // 1시간

    /**
     * 장비 메트릭 구독 (equipmentId 기준)
     */
    public SseEmitter subscribeEquipment(Long equipmentId) {
        String topic = "equipment-" + equipmentId;
        return createEmitter(topic);
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

        // 1. 맵에 Emitter 추가
        // putIfAbsent: 맵에 topic이 없으면 새 리스트를 만들고, 있으면 기존 리스트 반환
        // CopyOnWriteArrayList: 스레드 안전한 리스트 (순회 중 삭제가 일어나도 OK)
        this.emitters.putIfAbsent(topic, new CopyOnWriteArrayList<>());
        this.emitters.get(topic).add(emitter);

        log.info("✅ SSE 구독 시작: [{}], 현재 구독자 수: {}", topic, this.emitters.get(topic).size());

        // 2. 연결 종료 시 (Timeout / Completion) Emitter 자동 제거
        emitter.onTimeout(() -> {
            log.warn("⚠️ SSE 타임아웃: [{}]", topic);
            this.emitters.get(topic).remove(emitter);
        });
        emitter.onCompletion(() -> {
            log.info("🔌 SSE 연결 종료: [{}]", topic);
            this.emitters.get(topic).remove(emitter);
        });

        // 3. 연결 성공 "더미" 데이터 전송 (연결 확인용)
        try {
            emitter.send(SseEmitter.event()
                    .name("connect") // 이벤트 이름
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
            return; // 구독자가 없으면 아무것도 안 함
        }

        // 모든 구독자에게 데이터 전송
        for (SseEmitter emitter : topicEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName) // 이벤트 이름 (예: "system", "disk")
                        .data(data));   // 실제 데이터 (JSON으로 변환됨)
            } catch (IOException e) {
                // 클라이언트 연결이 끊겼을 때 (예: 브라우저 닫음)
                log.warn("❌ SSE 데이터 전송 실패: [{}], Emitter 제거", topic);
                topicEmitters.remove(emitter); // 연결 끊긴 클라이언트 제거
            }
        }
    }
}