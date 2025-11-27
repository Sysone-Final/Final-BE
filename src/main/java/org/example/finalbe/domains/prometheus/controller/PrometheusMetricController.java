// 작성자: 황요한
// Prometheus SSE 컨트롤러

package org.example.finalbe.domains.prometheus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.service.SseEmitterService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/prometheus")
@RequiredArgsConstructor
@Slf4j
public class PrometheusMetricController {

    private final SseEmitterService sseEmitterService;

    // 실시간 메트릭 스트림 (SSE)
    @GetMapping(value = "/metrics/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics() {
        log.info("SSE 연결 요청");
        return sseEmitterService.createEmitter();
    }

    // 현재 활성 SSE 연결 수
    @GetMapping("/metrics/stream/connections")
    public ConnectionStatusResponse getConnectionStatus() {
        return new ConnectionStatusResponse(sseEmitterService.getActiveConnectionCount());
    }

    public record ConnectionStatusResponse(int activeConnections) {}
}
