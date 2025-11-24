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
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AlertController {

    private final AlertNotificationService notificationService;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository companyServerRoomRepository;

    // ========== SSE 구독 ==========

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeAll() {
        log.info("전체 알림 구독 요청");
        return notificationService.subscribeAll();
    }

    @GetMapping(value = "/subscribe/equipment/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeEquipment(@PathVariable Long id) {
        log.info("Equipment 알림 구독 요청: equipmentId={}", id);
        return notificationService.subscribeEquipment(id);
    }

    @GetMapping(value = "/subscribe/rack/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeRack(@PathVariable Long id) {
        log.info("Rack 알림 구독 요청: rackId={}", id);
        return notificationService.subscribeRack(id);
    }

    @GetMapping(value = "/subscribe/serverroom/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeServerRoom(@PathVariable Long id) {
        log.info("ServerRoom 알림 구독 요청: serverRoomId={}", id);
        return notificationService.subscribeServerRoom(id);
    }

    @GetMapping(value = "/subscribe/datacenter/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeDataCenter(@PathVariable Long id) {
        log.info("DataCenter 알림 구독 요청 (사용 안 함): dataCenterId={}", id);
        return notificationService.subscribeDataCenter(id);
    }

    // ========== 알림 조회 ==========

    /**
     * 로그인한 사용자의 회사에 매핑된 서버실의 활성 알림 조회
     * ✅ 페이지네이션 + DB 레벨 필터링 + 시간 범위 제한
     * ✅ 임계치 기반 자동 알림만 표시
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllActiveAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) AlertLevel level) {

        if (size > 100) size = 100;
        if (size < 1) size = 20;

        Long userId = extractUserId();
        Member currentMember = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            log.info("사용자 {}({})의 회사 {}({})에 매핑된 서버실이 없습니다.",
                    userId, currentMember.getName(), companyId, currentMember.getCompany().getName());

            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("content", List.of());
            emptyResponse.put("totalElements", 0);
            emptyResponse.put("totalPages", 0);
            emptyResponse.put("currentPage", page);
            emptyResponse.put("pageSize", size);
            emptyResponse.put("message", "매핑된 서버실이 없습니다.");

            return ResponseEntity.ok(emptyResponse);
        }

        LocalDateTime startTime = days > 0
                ? LocalDateTime.now().minusDays(days)
                : LocalDateTime.now().minusYears(10);

        Pageable pageable = PageRequest.of(page, size, Sort.by("triggeredAt").descending());

        Page<AlertHistory> alertPage;

        if (level != null) {
            alertPage = alertHistoryRepository
                    .findByServerRoomIdInAndStatusAndLevelAndTriggeredAtAfterAndTargetTypeNot(
                            serverRoomIds,
                            AlertStatus.TRIGGERED,
                            level,
                            startTime,
                            TargetType.DATA_CENTER,
                            pageable
                    );
        } else {
            alertPage = alertHistoryRepository
                    .findByServerRoomIdInAndStatusAndTriggeredAtAfterAndTargetTypeNot(
                            serverRoomIds,
                            AlertStatus.TRIGGERED,
                            startTime,
                            TargetType.DATA_CENTER,
                            pageable
                    );
        }

        List<AlertHistoryDto> dtos = alertPage.getContent().stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", dtos);
        response.put("totalElements", alertPage.getTotalElements());
        response.put("totalPages", alertPage.getTotalPages());
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("hasNext", alertPage.hasNext());
        response.put("hasPrevious", alertPage.hasPrevious());

        log.info("사용자 {}({}, {})의 알림 조회: 페이지 {}/{}, 총 {}개 (필터: {}일, 레벨: {})",
                userId, currentMember.getName(), currentMember.getRole(),
                page + 1, alertPage.getTotalPages(), alertPage.getTotalElements(),
                days > 0 ? days : "전체", level != null ? level : "전체");

        return ResponseEntity.ok(response);
    }

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

    @GetMapping("/serverroom/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getServerRoomAlerts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "TRIGGERED") AlertStatus status) {

        List<AlertHistory> alerts = alertHistoryRepository
                .findByServerRoomIdAndStatusOrderByTriggeredAtDesc(id, status);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/datacenter/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getDataCenterAlerts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "TRIGGERED") AlertStatus status) {

        log.warn("DataCenter 알림은 더 이상 지원되지 않습니다. dataCenterId: {}", id);
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertHistoryDto> getAlertDetail(@PathVariable Long id) {
        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));

        return ResponseEntity.ok(AlertHistoryDto.from(alert));
    }

    @GetMapping("/statistics")
    public ResponseEntity<AlertStatisticsDto> getStatistics() {
        Long userId = extractUserId();
        Member currentMember = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(new AlertStatisticsDto(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        }

        long totalAlerts = alertHistoryRepository.countByServerRoomIdIn(serverRoomIds);
        long triggeredAlerts = alertHistoryRepository.countByServerRoomIdInAndStatus(serverRoomIds, AlertStatus.TRIGGERED);
        long acknowledgedAlerts = alertHistoryRepository.countByServerRoomIdInAndStatus(serverRoomIds, AlertStatus.ACKNOWLEDGED);
        long resolvedAlerts = alertHistoryRepository.countByServerRoomIdInAndStatus(serverRoomIds, AlertStatus.RESOLVED);

        long criticalAlerts = alertHistoryRepository.countByServerRoomIdInAndLevel(serverRoomIds, AlertLevel.CRITICAL);
        long warningAlerts = alertHistoryRepository.countByServerRoomIdInAndLevel(serverRoomIds, AlertLevel.WARNING);

        long equipmentAlerts = alertHistoryRepository.countByServerRoomIdInAndTargetType(serverRoomIds, TargetType.EQUIPMENT);
        long rackAlerts = alertHistoryRepository.countByServerRoomIdInAndTargetType(serverRoomIds, TargetType.RACK);
        long serverRoomAlerts = alertHistoryRepository.countByServerRoomIdInAndTargetType(serverRoomIds, TargetType.SERVER_ROOM);

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
                0L
        );

        return ResponseEntity.ok(stats);
    }

    // ========== 알림 액션 ==========

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