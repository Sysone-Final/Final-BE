// 작성자: 최산하, 황요한
// SSE 기반 실시간 메트릭 구독 API 제공 (장비/랙/서버실/데이터센터)

package org.example.finalbe.domains.monitoring.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/monitoring/subscribe")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * 장비 실시간 메트릭 구독
     */
    @GetMapping(value = "/equipment/{equipmentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEquipment(@PathVariable Long equipmentId, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("🔐 SSE 구독 요청 - Equipment: {}, Auth: {}", equipmentId,
                auth != null ? auth.getName() : "Anonymous");

        applySseHeaders(response);
        return sseService.subscribeEquipment(equipmentId);
    }

    /**
     * 랙 실시간 환경 메트릭 구독
     */
    @GetMapping(value = "/rack/{rackId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToRack(@PathVariable Long rackId, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("🔐 SSE 구독 요청 - Rack: {}, Auth: {}", rackId,
                auth != null ? auth.getName() : "Anonymous");

        applySseHeaders(response);
        return sseService.subscribeRack(rackId);
    }

    /**
     * 서버실 실시간 통계 구독
     */
    @GetMapping(value = "/serverroom/{serverRoomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToServerRoom(@PathVariable Long serverRoomId, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("🔐 SSE 구독 요청 - ServerRoom: {}, Auth: {}", serverRoomId,
                auth != null ? auth.getName() : "Anonymous");

        applySseHeaders(response);
        return sseService.subscribeServerRoom(serverRoomId);
    }

    /**
     * 데이터센터 실시간 통계 구독
     */
    @GetMapping(value = "/datacenter/{dataCenterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToDataCenter(@PathVariable Long dataCenterId, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("🔐 SSE 구독 요청 - DataCenter: {}, Auth: {}", dataCenterId,
                auth != null ? auth.getName() : "Anonymous");

        applySseHeaders(response);
        return sseService.subscribeDataCenter(dataCenterId);
    }

    private void applySseHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Connection", "keep-alive");
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
    }
}
