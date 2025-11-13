package org.example.finalbe.domains.prometheus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SSEBroadcastService {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private static final long SSE_TIMEOUT = 3600000L; // 1시간
    private static final long HEARTBEAT_INTERVAL = 30000L; // 30초

    /**
     * SSE 연결 생성
     */
    public SseEmitter createEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        log.info("SSE 연결 생성 - clientId: {}, 현재 연결 수: {}", clientId, emitters.get(clientId).size());

        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료 - clientId: {}", clientId);
            removeEmitter(clientId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 타임아웃 - clientId: {}", clientId);
            removeEmitter(clientId, emitter);
        });

        emitter.onError((ex) -> {
            log.error("SSE 에러 - clientId: {}, error: {}", clientId, ex.getMessage());
            removeEmitter(clientId, emitter);
        });

        // 초기 연결 확인 메시지
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 연결 성공"));
        } catch (IOException e) {
            log.error("초기 메시지 전송 실패 - clientId: {}", clientId, e);
            removeEmitter(clientId, emitter);
        }

        return emitter;
    }

    /**
     * 특정 클라이언트에게 메시지 전송
     */
    public void sendToClient(String clientId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);

        if (clientEmitters == null || clientEmitters.isEmpty()) {
            log.debug("전송 대상 클라이언트 없음 - clientId: {}", clientId);
            return;
        }

        clientEmitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                return false;
            } catch (IOException e) {
                log.error("메시지 전송 실패 - clientId: {}, event: {}", clientId, eventName, e);
                return true;
            }
        });
    }

    /**
     * 모든 클라이언트에게 브로드캐스트
     */
    public void broadcast(String eventName, Object data) {
        log.debug("브로드캐스트 시작 - event: {}, 총 클라이언트 수: {}", eventName, emitters.size());

        emitters.forEach((clientId, clientEmitters) -> {
            clientEmitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                    return false;
                } catch (IOException e) {
                    log.error("브로드캐스트 실패 - clientId: {}, event: {}", clientId, eventName, e);
                    return true;
                }
            });

            if (clientEmitters.isEmpty()) {
                emitters.remove(clientId);
            }
        });
    }

    /**
     * Heartbeat 전송 (연결 유지)
     */
    public void sendHeartbeat() {
        emitters.forEach((clientId, clientEmitters) -> {
            clientEmitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("ping"));
                    return false;
                } catch (IOException e) {
                    log.error("Heartbeat 전송 실패 - clientId: {}", clientId);
                    return true;
                }
            });

            if (clientEmitters.isEmpty()) {
                emitters.remove(clientId);
            }
        });
    }

    /**
     * Emitter 제거
     */
    private void removeEmitter(String clientId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters != null) {
            clientEmitters.remove(emitter);
            if (clientEmitters.isEmpty()) {
                emitters.remove(clientId);
            }
            log.info("Emitter 제거 - clientId: {}, 남은 연결 수: {}", clientId, clientEmitters.size());
        }
    }

    /**
     * 특정 클라이언트의 모든 연결 종료
     */
    public void closeClient(String clientId) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.remove(clientId);
        if (clientEmitters != null) {
            clientEmitters.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.error("Emitter 종료 실패 - clientId: {}", clientId, e);
                }
            });
            log.info("클라이언트 연결 종료 - clientId: {}", clientId);
        }
    }

    /**
     * 모든 연결 종료
     */
    public void closeAll() {
        emitters.forEach((clientId, clientEmitters) -> {
            clientEmitters.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.error("Emitter 종료 실패 - clientId: {}", clientId, e);
                }
            });
        });
        emitters.clear();
        log.info("모든 SSE 연결 종료");
    }

    /**
     * 현재 연결 상태 조회
     */
    public Map<String, Integer> getConnectionStatus() {
        Map<String, Integer> status = new ConcurrentHashMap<>();
        emitters.forEach((clientId, clientEmitters) ->
                status.put(clientId, clientEmitters.size())
        );
        return status;
    }

    /**
     * 전체 연결 수
     */
    public int getTotalConnections() {
        return emitters.values().stream()
                .mapToInt(CopyOnWriteArrayList::size)
                .sum();
    }
}