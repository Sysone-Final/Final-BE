/**
 * 작성자: 황요한
 * 알림(SSE 구독, 조회, 통계, 읽음 처리, 삭제) 관련 API 컨트롤러
 */
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

    // SSE 전체/장비/랙/서버실/데이터센터 알림 구독
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

    // 회사의 모든 서버실 기준 알림 조회
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AlertLevel level,
            @RequestParam(defaultValue = "0") int days) {

        Long userId = extractUserId();
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
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

        Page<AlertHistory> alertPage = (level != null)
                ? alertHistoryRepository.findByServerRoomIdInAndLevelAndTriggeredAtAfterAndTargetTypeNot(
                serverRoomIds, level, startTime, TargetType.DATA_CENTER, pageable)
                : alertHistoryRepository.findByServerRoomIdInAndTriggeredAtAfterAndTargetTypeNot(
                serverRoomIds, startTime, TargetType.DATA_CENTER, pageable);

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

        return ResponseEntity.ok(response);
    }

    // 장비 알림 조회
    @GetMapping("/equipment/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getEquipmentAlerts(@PathVariable Long id) {
        List<AlertHistory> alerts = alertHistoryRepository.findByEquipmentIdOrderByTriggeredAtDesc(id);
        return ResponseEntity.ok(alerts.stream().map(AlertHistoryDto::from).toList());
    }

    // 랙 알림 조회
    @GetMapping("/rack/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getRackAlerts(@PathVariable Long id) {
        List<AlertHistory> alerts = alertHistoryRepository.findByRackIdOrderByTriggeredAtDesc(id);
        return ResponseEntity.ok(alerts.stream().map(AlertHistoryDto::from).toList());
    }

    // 서버실 알림 조회
    @GetMapping("/serverroom/{id}")
    public ResponseEntity<List<AlertHistoryDto>> getServerRoomAlerts(@PathVariable Long id) {
        List<AlertHistory> alerts = alertHistoryRepository.findByServerRoomIdOrderByTriggeredAtDesc(id);
        return ResponseEntity.ok(alerts.stream().map(AlertHistoryDto::from).toList());
    }

    // 알림 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<AlertHistoryDto> getAlertDetail(@PathVariable Long id) {
        AlertHistory alert = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));
        return ResponseEntity.ok(AlertHistoryDto.from(alert));
    }

    // 알림 통계 조회
    @GetMapping("/statistics")
    public ResponseEntity<AlertStatisticsDto> getStatistics() {
        Long userId = extractUserId();
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(mapping -> mapping.getServerRoom().getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) return ResponseEntity.ok(AlertStatisticsDto.empty());

        AlertStatisticsDto stats = new AlertStatisticsDto(
                alertHistoryRepository.countByServerRoomIdIn(serverRoomIds),
                alertHistoryRepository.countByServerRoomIdIn(serverRoomIds),
                alertHistoryRepository.countByServerRoomIdInAndLevel(serverRoomIds, AlertLevel.CRITICAL),
                alertHistoryRepository.countByServerRoomIdInAndLevel(serverRoomIds, AlertLevel.WARNING),
                alertHistoryRepository.countByServerRoomIdInAndTargetType(serverRoomIds, TargetType.EQUIPMENT),
                alertHistoryRepository.countByServerRoomIdInAndTargetType(serverRoomIds, TargetType.RACK),
                alertHistoryRepository.countByServerRoomIdInAndTargetType(serverRoomIds, TargetType.SERVER_ROOM)
        );

        return ResponseEntity.ok(stats);
    }

    // 전체 알림 읽음 처리
    @PostMapping("/mark-all-as-read")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAllAlertsAsRead() {
        Long userId = extractUserId();
        Member currentMember = memberRepository.findByIdWithCompany(userId)
                .orElseThrow();

        Long companyId = currentMember.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository.findByCompanyId(companyId)
                .stream().map(m -> m.getServerRoom().getId()).toList();

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "매핑된 서버실이 없습니다.", "updatedCount", 0));
        }

        int updatedCount = alertHistoryRepository.markAllAsReadByServerRoomIds(serverRoomIds, LocalDateTime.now(), userId);

        return ResponseEntity.ok(Map.of("success", true, "message", "전체 알림을 읽음 처리했습니다.", "updatedCount", updatedCount));
    }

    // 선택 알림 읽음 처리
    @PostMapping("/mark-as-read")
    @Transactional
    public ResponseEntity<Map<String, Object>> markAlertsAsRead(@Valid @RequestBody MarkAsReadRequest request) {
        Long userId = extractUserId();
        Member member = memberRepository.findByIdWithCompany(userId).orElseThrow();

        Long companyId = member.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository.findByCompanyId(companyId)
                .stream().map(m -> m.getServerRoom().getId()).toList();

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "매핑된 서버실이 없습니다.", "updatedCount", 0));
        }

        List<AlertHistory> alerts = alertHistoryRepository.findAllById(request.alertIds());

        boolean unauthorized = alerts.stream()
                .anyMatch(alert -> !serverRoomIds.contains(alert.getServerRoomId()));

        if (unauthorized) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "권한 없는 알림 포함", "updatedCount", 0));
        }

        int updatedCount = alertHistoryRepository.markAsReadByIds(request.alertIds(), LocalDateTime.now(), userId);

        return ResponseEntity.ok(Map.of("success", true, "message", "선택한 알림을 읽음 처리했습니다.", "updatedCount", updatedCount));
    }

    // 전체 알림 삭제
    @DeleteMapping("/delete-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAllAlerts() {
        Long userId = extractUserId();
        Member member = memberRepository.findByIdWithCompany(userId).orElseThrow();

        Long companyId = member.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository.findByCompanyId(companyId)
                .stream().map(m -> m.getServerRoom().getId()).toList();

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "매핑된 서버실이 없습니다.", "deletedCount", 0));
        }

        int deletedCount = alertHistoryRepository.deleteAllByServerRoomIds(serverRoomIds);

        return ResponseEntity.ok(Map.of("success", true, "message", "전체 알림을 삭제했습니다.", "deletedCount", deletedCount));
    }

    // 선택 알림 삭제
    @DeleteMapping("/delete")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAlerts(@Valid @RequestBody DeleteAlertsRequest request) {
        Long userId = extractUserId();
        Member member = memberRepository.findByIdWithCompany(userId).orElseThrow();

        Long companyId = member.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository.findByCompanyId(companyId)
                .stream().map(m -> m.getServerRoom().getId()).toList();

        if (serverRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "매핑된 서버실이 없습니다.", "deletedCount", 0));
        }

        int deletedCount = alertHistoryRepository.deleteByIdsAndServerRoomIds(request.alertIds(), serverRoomIds);

        return ResponseEntity.ok(Map.of("success", true, "message", "선택한 알림을 삭제했습니다.", "deletedCount", deletedCount));
    }

    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Long userId = extractUserId();
        Member member = memberRepository.findByIdWithCompany(userId).orElseThrow();

        Long companyId = member.getCompany().getId();

        List<Long> serverRoomIds = companyServerRoomRepository.findByCompanyId(companyId)
                .stream().map(m -> m.getServerRoom().getId()).toList();

        long unreadCount = serverRoomIds.isEmpty()
                ? 0
                : alertHistoryRepository.countUnreadByServerRoomIds(serverRoomIds);

        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    // 인증된 사용자 ID 추출
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
