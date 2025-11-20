package org.example.finalbe.domains.monitoring.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.monitoring.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/monitoring/subscribe")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * 장비 실시간 메트릭 구독
     * @param equipmentId 장비 ID
     * @return SseEmitter
     */
    @GetMapping(value = "/equipment/{equipmentId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEquipment(@PathVariable Long equipmentId, HttpServletResponse response) {
        applySseHeaders(response);
        return sseService.subscribeEquipment(equipmentId);
    }

    /**
     * 랙 실시간 환경 메트릭 구독
     * @param rackId 랙 ID
     * @return SseEmitter
     */
    @GetMapping(value = "/rack/{rackId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToRack(@PathVariable Long rackId, HttpServletResponse response) {
        applySseHeaders(response);
        return sseService.subscribeRack(rackId);
    }

    /**
     * 서버실 실시간 통계 구독
     * @param serverRoomId 서버실 ID
     * @return SseEmitter
     */
    @GetMapping(value = "/serverroom/{serverRoomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToServerRoom(@PathVariable Long serverRoomId, HttpServletResponse response) {
        applySseHeaders(response);
        return sseService.subscribeServerRoom(serverRoomId);
    }

    /**
     * 데이터센터 실시간 통계 구독
     * @param dataCenterId 데이터센터 ID
     * @return SseEmitter
     */
    @GetMapping(value = "/datacenter/{dataCenterId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToDataCenter(@PathVariable Long dataCenterId, HttpServletResponse response) {
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