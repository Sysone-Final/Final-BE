package org.example.finalbe.domains.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 60L * 60 * 1000; // 1시간

    /**
     * 전체 알림 구독
     */
    public SseEmitter subscribeAll() {
        return createEmitter("alerts-all");
    }

    /**
     * Equipment 알림 구독
     */
    public SseEmitter subscribeEquipment(Long equipmentId) {
        return createEmitter("alerts-equipment-" + equipmentId);
    }

    /**
     * Rack 알림 구독
     */
    public SseEmitter subscribeRack(Long rackId) {
        return createEmitter("alerts-rack-" + rackId);
    }

    /**
     * ServerRoom 알림 구독
     */
    public SseEmitter subscribeServerRoom(Long serverRoomId) {
        return createEmitter("alerts-serverroom-" + serverRoomId);
    }

    /**
     * DataCenter 알림 구독
     */
    public SseEmitter subscribeDataCenter(Long dataCenterId) {
        return createEmitter("alerts-datacenter-" + dataCenterId);
    }

    /**
     * Emitter 생성
     */
    private SseEmitter createEmitter(String topic) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitters.putIfAbsent(topic, new CopyOnWriteArrayList<>());
        emitters.get(topic).add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE 연결 완료: topic={}", topic);
            emitters.get(topic).remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE 연결 타임아웃: topic={}", topic);
            emitters.get(topic).remove(emitter);
        });

        emitter.onError((error) -> {
            log.error("SSE 연결 오류: topic={}", topic, error);
            emitters.get(topic).remove(emitter);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to " + topic));
            log.info("✅ SSE 연결 성공: topic={}", topic);
        } catch (IOException e) {
            log.error("SSE 초기 연결 실패: topic={}", topic, e);
            emitters.get(topic).remove(emitter);
        }

        return emitter;
    }

    /**
     * 알림 발생 전송
     */
    @Async("alertExecutor")
    public void sendAlert(AlertHistory alert) {
        AlertNotificationDto dto = AlertNotificationDto.from(alert);

        // 전체 구독자에게 전송
        sendToTopic("alerts-all", "alert-triggered", dto);

        // 대상별 구독자에게 전송
        if (alert.getEquipmentId() != null) {
            sendToTopic("alerts-equipment-" + alert.getEquipmentId(), "alert-triggered", dto);
        }
        if (alert.getRackId() != null) {
            sendToTopic("alerts-rack-" + alert.getRackId(), "alert-triggered", dto);
        }
        if (alert.getServerRoomId() != null) {
            sendToTopic("alerts-serverroom-" + alert.getServerRoomId(), "alert-triggered", dto);
        }
        if (alert.getDataCenterId() != null) {
            sendToTopic("alerts-datacenter-" + alert.getDataCenterId(), "alert-triggered", dto);
        }
    }

    /**
     * 알림 확인 전송
     */
    @Async("alertExecutor")
    public void sendAlertAcknowledged(AlertHistory alert) {
        AlertNotificationDto dto = AlertNotificationDto.from(alert);

        sendToTopic("alerts-all", "alert-acknowledged", dto);

        if (alert.getEquipmentId() != null) {
            sendToTopic("alerts-equipment-" + alert.getEquipmentId(), "alert-acknowledged", dto);
        }
        if (alert.getRackId() != null) {
            sendToTopic("alerts-rack-" + alert.getRackId(), "alert-acknowledged", dto);
        }
        if (alert.getServerRoomId() != null) {
            sendToTopic("alerts-serverroom-" + alert.getServerRoomId(), "alert-acknowledged", dto);
        }
        if (alert.getDataCenterId() != null) {
            sendToTopic("alerts-datacenter-" + alert.getDataCenterId(), "alert-acknowledged", dto);
        }
    }

    /**
     * 알림 해결 전송
     */
    @Async("alertExecutor")
    public void sendAlertResolved(AlertHistory alert) {
        AlertNotificationDto dto = AlertNotificationDto.from(alert);

        sendToTopic("alerts-all", "alert-resolved", dto);

        if (alert.getEquipmentId() != null) {
            sendToTopic("alerts-equipment-" + alert.getEquipmentId(), "alert-resolved", dto);
        }
        if (alert.getRackId() != null) {
            sendToTopic("alerts-rack-" + alert.getRackId(), "alert-resolved", dto);
        }
        if (alert.getServerRoomId() != null) {
            sendToTopic("alerts-serverroom-" + alert.getServerRoomId(), "alert-resolved", dto);
        }
        if (alert.getDataCenterId() != null) {
            sendToTopic("alerts-datacenter-" + alert.getDataCenterId(), "alert-resolved", dto);
        }
    }

    /**
     * 특정 토픽으로 메시지 전송
     */
    private void sendToTopic(String topic, String eventName, Object data) {
        List<SseEmitter> topicEmitters = emitters.get(topic);

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
                log.debug("SSE 전송 실패, Emitter 제거: topic={}", topic);
                return true;
            }
        });
    }

    /**
     * 연결된 구독자 수 조회
     */
    public int getSubscriberCount(String topic) {
        List<SseEmitter> topicEmitters = emitters.get(topic);
        return topicEmitters != null ? topicEmitters.size() : 0;
    }

    /**
     * 전체 구독자 수 조회
     */
    public int getTotalSubscriberCount() {
        return emitters.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}