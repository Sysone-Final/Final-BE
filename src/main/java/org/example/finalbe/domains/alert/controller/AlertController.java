package org.example.finalbe.domains.alert.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.alert.dto.AlertHistoryDto;
import org.example.finalbe.domains.alert.dto.AlertStatisticsDto;

import org.example.finalbe.domains.alert.repository.AlertHistoryRepository;
import org.example.finalbe.domains.alert.service.AlertNotificationService;
import org.example.finalbe.domains.common.enumdir.AlertStatus;
import org.example.finalbe.domains.common.enumdir.TargetType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertNotificationService notificationService;
    private final AlertHistoryRepository alertHistoryRepository;

    // ========== SSE 구독 ==========

    /**
     * 전체 알림 구독
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeAll() {
        log.info("전체 알림 구독 요청");
        return notificationService.subscribeAll();
    }

    /**
     * Equipment 알림 구독
     */
    @GetMapping(value = "/subscribe/equipment/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeEquipment(@PathVariable Long id) {
        log.info("Equipment 알림 구독 요청: equipmentId={}", id);
        return notificationService.subscribeEquipment(id);
    }

    /**
     * Rack 알림 구독
     */
    @GetMapping(value = "/subscribe/rack/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeRack(@PathVariable Long id) {
        log.info("Rack 알림 구독 요청: rackId={}", id);
        return notificationService.subscribeRack(id);
    }

    /**
     * ServerRoom 알림 구독
     */
    @GetMapping(value = "/subscribe/serverroom/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeServerRoom(@PathVariable Long id) {
        log.info("ServerRoom 알림 구독 요청: serverRoomId={}", id);
        return notificationService.subscribeServerRoom(id);
    }

    /**
     * DataCenter 알림 구독
     */
    @GetMapping(value = "/subscribe/datacenter/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeDataCenter(@PathVariable Long id) {
        log.info("DataCenter 알림 구독 요청: dataCenterId={}", id);
        return notificationService.subscribeDataCenter(id);
    }

    // ========== 알림 조회 ==========

    /**
     * 전체 활성 알림 조회
     */
    @GetMapping
    public ResponseEntity<List<AlertHistoryDto>> getAllActiveAlerts() {
        List<AlertHistory> alerts = alertHistoryRepository
                .findByStatusOrderByTriggeredAtDesc(AlertStatus.TRIGGERED);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Equipment 알림 조회
     */
    @GetMapping("/equipment/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getEquipmentAlerts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "TRIGGERED") AlertStatus status) {

        List<AlertHistory> alerts = alertHistoryRepository
                .findByEquipmentIdAndStatusOrderByTriggeredAtDesc(id, status);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Rack 알림 조회
     */
    @GetMapping("/rack/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getRackAlerts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "TRIGGERED") AlertStatus status) {

        List<AlertHistory> alerts = alertHistoryRepository
                .findByRackIdAndStatusOrderByTriggeredAtDesc(id, status);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * 알림 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertHistoryDto> getAlertDetail(@PathVariable Long id) {
        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + id));

        return ResponseEntity.ok(AlertHistoryDto.from(alert));
    }

    /**
     * 알림 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<AlertStatisticsDto> getStatistics() {
        List<AlertHistory> allAlerts = alertHistoryRepository.findAll();

        AlertStatisticsDto stats = AlertStatisticsDto.builder()
                .totalAlerts((long) allAlerts.size())
                .triggeredAlerts(allAlerts.stream()
                        .filter(a -> a.getStatus() == AlertStatus.TRIGGERED)
                        .count())
                .acknowledgedAlerts(allAlerts.stream()
                        .filter(a -> a.getStatus() == AlertStatus.ACKNOWLEDGED)
                        .count())
                .resolvedAlerts(allAlerts.stream()
                        .filter(a -> a.getStatus() == AlertStatus.RESOLVED)
                        .count())
                .criticalAlerts(allAlerts.stream()
                        .filter(a -> a.getLevel().name().equals("CRITICAL"))
                        .count())
                .warningAlerts(allAlerts.stream()
                        .filter(a -> a.getLevel().name().equals("WARNING"))
                        .count())
                .equipmentAlerts(allAlerts.stream()
                        .filter(a -> a.getTargetType() == TargetType.EQUIPMENT)
                        .count())
                .rackAlerts(allAlerts.stream()
                        .filter(a -> a.getTargetType() == TargetType.RACK)
                        .count())
                .serverRoomAlerts(allAlerts.stream()
                        .filter(a -> a.getTargetType() == TargetType.SERVER_ROOM)
                        .count())
                .dataCenterAlerts(allAlerts.stream()
                        .filter(a -> a.getTargetType() == TargetType.DATA_CENTER)
                        .count())
                .build();

        return ResponseEntity.ok(stats);
    }

    // ========== 알림 액션 ==========

    /**
     * 알림 확인
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertHistoryDto> acknowledgeAlert(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {

        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + id));

        Long userId = body.get("userId");
        alert.acknowledge(userId);
        alertHistoryRepository.save(alert);

        notificationService.sendAlertAcknowledged(alert);

        log.info("알림 확인됨: alertId={}, userId={}", id, userId);

        return ResponseEntity.ok(AlertHistoryDto.from(alert));
    }

    /**
     * 알림 해결
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertHistoryDto> resolveAlert(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {

        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다: " + id));

        Long userId = body.get("userId");
        alert.resolve(userId);
        alertHistoryRepository.save(alert);

        notificationService.sendAlertResolved(alert);

        log.info("알림 해결됨: alertId={}, userId={}", id, userId);

        return ResponseEntity.ok(AlertHistoryDto.from(alert));
    }

    /**
     * 여러 알림 일괄 확인
     */
    @PostMapping("/acknowledge-multiple")
    public ResponseEntity<List<AlertHistoryDto>> acknowledgeMultipleAlerts(
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<Long> alertIds = (List<Long>) body.get("alertIds");
        Long userId = ((Number) body.get("userId")).longValue();

        List<AlertHistory> alerts = alertHistoryRepository.findAllById(alertIds);

        alerts.forEach(alert -> {
            alert.acknowledge(userId);
            notificationService.sendAlertAcknowledged(alert);
        });

        alertHistoryRepository.saveAll(alerts);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        log.info("여러 알림 확인됨: count={}, userId={}", alerts.size(), userId);

        return ResponseEntity.ok(dtos);
    }

    /**
     * 여러 알림 일괄 해결
     */
    @PostMapping("/resolve-multiple")
    public ResponseEntity<List<AlertHistoryDto>> resolveMultipleAlerts(
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<Long> alertIds = (List<Long>) body.get("alertIds");
        Long userId = ((Number) body.get("userId")).longValue();

        List<AlertHistory> alerts = alertHistoryRepository.findAllById(alertIds);

        alerts.forEach(alert -> {
            alert.resolve(userId);
            notificationService.sendAlertResolved(alert);
        });

        alertHistoryRepository.saveAll(alerts);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        log.info("여러 알림 해결됨: count={}, userId={}", alerts.size(), userId);

        return ResponseEntity.ok(dtos);
    }
}