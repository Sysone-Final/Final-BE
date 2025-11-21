package org.example.finalbe.domains.alert.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.alert.dto.AlertHistoryDto;
import org.example.finalbe.domains.alert.dto.AlertStatisticsDto;
import org.example.finalbe.domains.alert.dto.AcknowledgeMultipleRequest;
import org.example.finalbe.domains.alert.dto.ResolveMultipleRequest;
import org.example.finalbe.domains.alert.repository.AlertHistoryRepository;
import org.example.finalbe.domains.alert.service.AlertNotificationService;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.AlertStatus;
import org.example.finalbe.domains.common.enumdir.TargetType;
import org.example.finalbe.domains.common.exception.AlertNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
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
                .orElseThrow(() -> new AlertNotFoundException(id));

        return ResponseEntity.ok(AlertHistoryDto.from(alert));
    }
    /**
     * 알림 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<AlertStatisticsDto> getStatistics() {
        long totalAlerts = alertHistoryRepository.count();
        long triggeredAlerts = alertHistoryRepository.countByStatus(AlertStatus.TRIGGERED);
        long acknowledgedAlerts = alertHistoryRepository.countByStatus(AlertStatus.ACKNOWLEDGED);
        long resolvedAlerts = alertHistoryRepository.countByStatus(AlertStatus.RESOLVED);

        long criticalAlerts = alertHistoryRepository.countByLevel(AlertLevel.CRITICAL);
        long warningAlerts = alertHistoryRepository.countByLevel(AlertLevel.WARNING);

        long equipmentAlerts = alertHistoryRepository.countByTargetType(TargetType.EQUIPMENT);
        long rackAlerts = alertHistoryRepository.countByTargetType(TargetType.RACK);
        long serverRoomAlerts = alertHistoryRepository.countByTargetType(TargetType.SERVER_ROOM);
        long dataCenterAlerts = alertHistoryRepository.countByTargetType(TargetType.DATA_CENTER);

        AlertStatisticsDto stats = new AlertStatisticsDto(
                totalAlerts,
                triggeredAlerts,
                acknowledgedAlerts,
                resolvedAlerts,
                criticalAlerts,
                warningAlerts,
                equipmentAlerts,
                rackAlerts,
                serverRoomAlerts,
                dataCenterAlerts
        );

        return ResponseEntity.ok(stats);
    }

    // ========== 알림 액션 ==========

    /**
     * 알림 확인
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertHistoryDto> acknowledgeAlert(@PathVariable Long id) {
        Long userId = extractUserId();

        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

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
    public ResponseEntity<AlertHistoryDto> resolveAlert(@PathVariable Long id) {
        Long userId = extractUserId();

        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

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
            @Valid @RequestBody AcknowledgeMultipleRequest request) {

        Long userId = extractUserId();

        List<AlertHistory> alerts = alertHistoryRepository.findAllById(request.alertIds());

        if (alerts.isEmpty()) {
            throw new AlertNotFoundException("요청한 알림을 찾을 수 없습니다.");
        }

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
            @Valid @RequestBody ResolveMultipleRequest request) {

        Long userId = extractUserId();

        List<AlertHistory> alerts = alertHistoryRepository.findAllById(request.alertIds());

        if (alerts.isEmpty()) {
            throw new AlertNotFoundException("요청한 알림을 찾을 수 없습니다.");
        }

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

    // ========== Private Methods ==========

    /**
     * JWT 토큰에서 userId 추출
     */
    private Long extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        String userId = authentication.getName();

        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("유효하지 않은 사용자 ID입니다.", e);
        }
    }
}