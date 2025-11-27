/**
 * 작성자: 황요한
 * SSE 연결을 관리하고 이벤트를 전송하는 서비스
 */
package org.example.finalbe.domains.prometheus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class SseEmitterService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AtomicLong emitterIdGenerator = new AtomicLong(0);

    // SSE 연결 생성
    public SseEmitter createEmitter() {
        Long emitterId = emitterIdGenerator.incrementAndGet();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.put(emitterId, emitter);
        log.info("📡 SSE 연결 생성: emitterId={}, 총 연결 수: {}", emitterId, emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitterId);
            log.info("✅ SSE 연결 완료: emitterId={}, 남은 연결 수: {}", emitterId, emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitterId);
            log.warn("⏱️ SSE 연결 타임아웃: emitterId={}, 남은 연결 수: {}", emitterId, emitters.size());
        });

        emitter.onError(throwable -> {
            emitters.remove(emitterId);
            log.error("❌ SSE 연결 오류: emitterId={}, 남은 연결 수: {}, error: {}",
                    emitterId, emitters.size(), throwable.getMessage());
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException e) {
            log.error("❌ SSE 초기 메시지 전송 실패: emitterId={}", emitterId, e);
            emitters.remove(emitterId);
        }

        return emitter;
    }

    // 모든 클라이언트에게 이벤트 전송
    public void sendToAll(String eventName, Object data) {
        if (emitters.isEmpty()) return;

        log.debug("📤 SSE 메시지 전송: event={}, 대상: {} 개 연결", eventName, emitters.size());

        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.error("❌ SSE 메시지 전송 실패: emitterId={}", id, e);
                emitters.remove(id);
            }
        });
    }

    // 활성 SSE 연결 수 조회
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
