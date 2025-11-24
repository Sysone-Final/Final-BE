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
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.TargetType;
import org.example.finalbe.domains.common.exception.AlertNotFoundException;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository companyServerRoomRepository;

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
     * DataCenter 알림 구독 (더 이상 사용하지 않지만 하위 호환성을 위해 유지)
     */
    @GetMapping(value = "/subscribe/datacenter/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeDataCenter(@PathVariable Long id) {
        log.info("DataCenter 알림 구독 요청 (사용 안 함): dataCenterId={}", id);
        return notificationService.subscribeDataCenter(id);
    }

    // ========== 알림 조회 ==========

    /**
     * 로그인한 사용자의 회사에 매핑된 서버실의 활성 알림 조회
     * (데이터센터 알림은 제외)
     */
    @GetMapping
    public ResponseEntity<List<AlertHistoryDto>> getAllActiveAlerts() {
        Long userId = extractUserId();
        Member currentMember = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        List<AlertHistory> alerts;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 모든 알림 조회 (데이터센터 알림 제외)
            alerts = alertHistoryRepository
                    .findByStatusOrderByTriggeredAtDesc(AlertStatus.TRIGGERED)
                    .stream()
                    .filter(alert -> alert.getTargetType() != TargetType.DATA_CENTER)
                    .collect(Collectors.toList());

            log.info("ADMIN 사용자 {}의 알림 조회: {}개 (데이터센터 알림 제외)", userId, alerts.size());
        } else {
            // 일반 사용자: 회사에 매핑된 서버실의 알림만 조회
            Long companyId = currentMember.getCompany().getId();

            // 회사에 매핑된 서버실 ID 목록 조회
            List<Long> serverRoomIds = companyServerRoomRepository
                    .findByCompanyId(companyId)
                    .stream()
                    .map(mapping -> mapping.getServerRoom().getId())
                    .collect(Collectors.toList());

            if (serverRoomIds.isEmpty()) {
                // 매핑된 서버실이 없으면 빈 리스트 반환
                log.info("사용자 {}의 회사 {}에 매핑된 서버실이 없습니다.", userId, companyId);
                return ResponseEntity.ok(List.of());
            }

            // 서버실 단위 알림 필터링
            alerts = alertHistoryRepository
                    .findByStatusOrderByTriggeredAtDesc(AlertStatus.TRIGGERED)
                    .stream()
                    .filter(alert -> {
                        // 데이터센터 알림은 제외
                        if (alert.getTargetType() == TargetType.DATA_CENTER) {
                            return false;
                        }
                        // 서버실 ID가 회사에 매핑된 서버실에 포함되는지 확인
                        return alert.getServerRoomId() != null &&
                                serverRoomIds.contains(alert.getServerRoomId());
                    })
                    .collect(Collectors.toList());

            log.info("사용자 {}의 알림 조회 완료: {}개 (서버실 ID: {}, 서버실 단위 필터링)",
                    userId, alerts.size(), serverRoomIds);
        }

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
     * ServerRoom 알림 조회
     */
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

    /**
     * DataCenter 알림 조회 (더 이상 사용하지 않지만 하위 호환성을 위해 유지)
     */
    @GetMapping("/datacenter/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getDataCenterAlerts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "TRIGGERED") AlertStatus status) {

        log.warn("DataCenter 알림은 더 이상 지원되지 않습니다. dataCenterId: {}", id);
        // 빈 리스트 반환
        return ResponseEntity.ok(List.of());
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
     * 알림 통계 조회 (데이터센터 알림 제외)
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
        // 데이터센터 알림은 제외
        long dataCenterAlerts = 0;

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