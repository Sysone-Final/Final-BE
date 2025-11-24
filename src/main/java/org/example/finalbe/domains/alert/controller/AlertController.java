// src/main/java/org/example/finalbe/domains/alert/controller/AlertController.java
package org.example.finalbe.domains.alert.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.alert.dto.AlertHistoryDto;
import org.example.finalbe.domains.alert.dto.AlertStatisticsDto;
import org.example.finalbe.domains.alert.dto.MarkAsReadRequest;
import org.example.finalbe.domains.alert.dto.DeleteAlertsRequest;
import org.example.finalbe.domains.alert.repository.AlertHistoryRepository;
import org.example.finalbe.domains.alert.service.AlertNotificationService;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
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
public class AlertController {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertNotificationService alertNotificationService;
    private final CompanyServerRoomRepository companyServerRoomRepository;
    private final MemberRepository memberRepository;

    // ========== SSE 구독 API ==========

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeAlerts() {
        return alertNotificationService.subscribeAll();
    }

    @GetMapping(value = "/equipment/{id}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeEquipmentAlerts(@PathVariable Long id) {
        return alertNotificationService.subscribeEquipment(id);
    }

    @GetMapping(value = "/rack/{id}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeRackAlerts(@PathVariable Long id) {
        return alertNotificationService.subscribeRack(id);
    }

    @GetMapping(value = "/serverroom/{id}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeServerRoomAlerts(@PathVariable Long id) {
        return alertNotificationService.subscribeServerRoom(id);
    }

    @GetMapping(value = "/datacenter/{id}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeDataCenterAlerts(@PathVariable Long id) {
        return alertNotificationService.subscribeDataCenter(id);
    }

    // ========== 알림 조회 API ==========

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AlertLevel level,
            @RequestParam(defaultValue = "0") int days) {

        Long userId = extractUserId();

        // ✅ Fetch Join으로 Company를 함께 조회
        Member currentMember = memberRepository.findByIdWithCompany(userId)
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
                    .findByServerRoomIdInAndLevelAndTriggeredAtAfterAndTargetTypeNot(
                            serverRoomIds,
                            level,
                            startTime,
                            TargetType.DATA_CENTER,
                            pageable
                    );
        } else {
            alertPage = alertHistoryRepository
                    .findByServerRoomIdInAndTriggeredAtAfterAndTargetTypeNot(
                            serverRoomIds,
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
    public ResponseEntity<List<AlertHistoryDto>> getEquipmentAlerts(@PathVariable Long id) {
        List<AlertHistory> alerts = alertHistoryRepository
                .findByEquipmentIdOrderByTriggeredAtDesc(id);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/rack/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getRackAlerts(@PathVariable Long id) {
        List<AlertHistory> alerts = alertHistoryRepository
                .findByRackIdOrderByTriggeredAtDesc(id);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/serverroom/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getServerRoomAlerts(@PathVariable Long id) {
        List<AlertHistory> alerts = alertHistoryRepository
                .findByServerRoomIdOrderByTriggeredAtDesc(id);

        List<AlertHistoryDto> dtos = alerts.stream()
                .map(AlertHistoryDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
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

        // ✅ Fetch Join으로 Company를 함께 조회
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(AlertStatisticsDto.empty());
        }

        long totalAlerts = alertHistoryRepository.countByServerRoomIdIn(serverRoomIds);
        long criticalAlerts = alertHistoryRepository.countByServerRoomIdInAndLevel(
                serverRoomIds, AlertLevel.CRITICAL);
        long warningAlerts = alertHistoryRepository.countByServerRoomIdInAndLevel(
                serverRoomIds, AlertLevel.WARNING);
        long equipmentAlerts = alertHistoryRepository.countByServerRoomIdInAndTargetType(
                serverRoomIds, TargetType.EQUIPMENT);
        long rackAlerts = alertHistoryRepository.countByServerRoomIdInAndTargetType(
                serverRoomIds, TargetType.RACK);
        long serverRoomAlerts = alertHistoryRepository.countByServerRoomIdInAndTargetType(
                serverRoomIds, TargetType.SERVER_ROOM);

        AlertStatisticsDto stats = new AlertStatisticsDto(
                totalAlerts,
                totalAlerts,
                criticalAlerts,
                warningAlerts,
                equipmentAlerts,
                rackAlerts,
                serverRoomAlerts
        );

        return ResponseEntity.ok(stats);
    }

    // ========== 읽음 처리 API ==========

    /**
     * 전체 알림 읽음 처리
     */
    @PostMapping("/mark-all-as-read")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAllAlertsAsRead() {
        Long userId = extractUserId();

        // ✅ Fetch Join으로 Company를 함께 조회
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "매핑된 서버실이 없습니다.",
                    "updatedCount", 0
            ));
        }

        int updatedCount = alertHistoryRepository.markAllAsReadByServerRoomIds(
                serverRoomIds,
                LocalDateTime.now(),
                userId
        );

        log.info("전체 알림 읽음 처리 완료: userId={}, count={}", userId, updatedCount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "전체 알림을 읽음 처리했습니다.",
                "updatedCount", updatedCount
        ));
    }

    /**
     * 선택한 알림 읽음 처리
     */
    @PostMapping("/mark-as-read")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAlertsAsRead(
            @Valid @RequestBody MarkAsReadRequest request) {

        Long userId = extractUserId();

        // ✅ Fetch Join으로 Company를 함께 조회
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "매핑된 서버실이 없습니다.",
                    "updatedCount", 0
            ));
        }

        // 권한 확인: 해당 알림들이 사용자의 회사 서버실에 속하는지 확인
        List<AlertHistory> alerts = alertHistoryRepository.findAllById(request.alertIds());

        boolean hasUnauthorizedAlert = alerts.stream()
                .anyMatch(alert -> !serverRoomIds.contains(alert.getServerRoomId()));

        if (hasUnauthorizedAlert) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "접근 권한이 없는 알림이 포함되어 있습니다.",
                    "updatedCount", 0
            ));
        }

        int updatedCount = alertHistoryRepository.markAsReadByIds(
                request.alertIds(),
                LocalDateTime.now(),
                userId
        );

        log.info("선택 알림 읽음 처리 완료: userId={}, count={}", userId, updatedCount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "선택한 알림을 읽음 처리했습니다.",
                "updatedCount", updatedCount
        ));
    }

    // ========== 삭제 API ==========

    /**
     * 전체 알림 삭제
     */
    @DeleteMapping("/delete-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAllAlerts() {
        Long userId = extractUserId();

        // ✅ Fetch Join으로 Company를 함께 조회
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "매핑된 서버실이 없습니다.",
                    "deletedCount", 0
            ));
        }

        int deletedCount = alertHistoryRepository.deleteAllByServerRoomIds(serverRoomIds);

        log.info("전체 알림 삭제 완료: userId={}, count={}", userId, deletedCount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "전체 알림을 삭제했습니다.",
                "deletedCount", deletedCount
        ));
    }

    /**
     * 선택한 알림 삭제
     */
    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAlerts(
            @Valid @RequestBody DeleteAlertsRequest request) {

        Long userId = extractUserId();

        // ✅ Fetch Join으로 Company를 함께 조회
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "매핑된 서버실이 없습니다.",
                    "deletedCount", 0
            ));
        }

        int deletedCount = alertHistoryRepository.deleteByIdsAndServerRoomIds(
                request.alertIds(),
                serverRoomIds
        );

        log.info("선택 알림 삭제 완료: userId={}, count={}", userId, deletedCount);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "선택한 알림을 삭제했습니다.",
                "deletedCount", deletedCount
        ));
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Long userId = extractUserId();

        // ✅ Fetch Join으로 Company를 함께 조회
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "unreadCount", 0L
            ));
        }

        long unreadCount = alertHistoryRepository.countUnreadByServerRoomIds(serverRoomIds);

        return ResponseEntity.ok(Map.of(
                "unreadCount", unreadCount
        ));
    }

    // ========== Private Methods ==========

    private Long extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        String userId = authentication.getName();

        if (userId == null || userId.equals("anonymousUser")) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        return Long.parseLong(userId);
    }
}