package org.example.finalbe.domains.history.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.history.dto.HistoryResponse;
import org.example.finalbe.domains.history.dto.HistorySearchRequest;
import org.example.finalbe.domains.history.dto.HistoryStatisticsResponse;

import org.example.finalbe.domains.history.service.HistoryService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 히스토리 컨트롤러
 * 변경 이력 조회 및 통계 API 제공
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 서버실 히스토리 조회 (기본)
     * GET /api/history/datacenter/{dataCenterId}
     *
     * @param dataCenterId 서버실 ID
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20)
     * @return 히스토리 목록
     */
    @GetMapping("/datacenter/{dataCenterId}")
    public ResponseEntity<CommonResDto> getDataCenterHistory(
            @PathVariable Long dataCenterId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Fetching history for datacenter: {}", dataCenterId);

        HistorySearchRequest request = HistorySearchRequest.builder()
                .dataCenterId(dataCenterId)
                .page(page)
                .size(size)
                .build();

        Page<HistoryResponse> history = historyService.searchHistory(request);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "히스토리 조회 성공", history));
    }

    /**
     * 서버실 히스토리 검색 (복합 조건)
     * GET /api/history/datacenter/{dataCenterId}/search
     *
     * @param dataCenterId 서버실 ID (필수)
     * @param entityType 엔티티 타입 (선택)
     * @param action 작업 타입 (선택)
     * @param changedBy 변경자 ID (선택)
     * @param startDate 시작 날짜 (선택)
     * @param endDate 종료 날짜 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 필터링된 히스토리 목록
     */
    @GetMapping("/datacenter/{dataCenterId}/search")
    public ResponseEntity<CommonResDto> searchHistory(
            @PathVariable Long dataCenterId,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) HistoryAction action,
            @RequestParam(required = false) Long changedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Searching history for datacenter: {} with filters", dataCenterId);

        HistorySearchRequest request = HistorySearchRequest.builder()
                .dataCenterId(dataCenterId)
                .entityType(entityType)
                .action(action)
                .changedBy(changedBy)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .build();

        Page<HistoryResponse> history = historyService.searchHistory(request);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "히스토리 검색 성공", history));
    }

    /**
     * 특정 엔티티 히스토리 조회
     * GET /api/history/entity
     *
     * @param dataCenterId 서버실 ID
     * @param entityType 엔티티 타입 (RACK, EQUIPMENT, DEVICE)
     * @param entityId 엔티티 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 특정 엔티티의 변경 이력
     */
    @GetMapping("/entity")
    public ResponseEntity<CommonResDto> getEntityHistory(
            @RequestParam Long dataCenterId,
            @RequestParam EntityType entityType,
            @RequestParam Long entityId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Fetching history for entity: {} {} in datacenter: {}",
                entityType, entityId, dataCenterId);

        HistorySearchRequest request = HistorySearchRequest.ofEntity(dataCenterId, entityType, entityId);
        request = HistorySearchRequest.builder()
                .dataCenterId(request.dataCenterId())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .page(page)
                .size(size)
                .build();

        Page<HistoryResponse> history = historyService.searchHistory(request);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "엔티티 히스토리 조회 성공", history));
    }

    /**
     * 서버실 히스토리 통계
     * GET /api/history/datacenter/{dataCenterId}/statistics
     *
     * @param dataCenterId 서버실 ID
     * @param startDate 시작 날짜 (기본: 30일 전)
     * @param endDate 종료 날짜 (기본: 현재)
     * @return 서버실 변경 통계
     */
    @GetMapping("/datacenter/{dataCenterId}/statistics")
    public ResponseEntity<CommonResDto> getStatistics(
            @PathVariable Long dataCenterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Fetching statistics for datacenter: {}", dataCenterId);

        HistoryStatisticsResponse statistics = historyService.getStatistics(dataCenterId, startDate, endDate);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "통계 조회 성공", statistics));
    }

    /**
     * 사용자별 히스토리 조회
     * GET /api/history/user/{userId}
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자의 변경 이력
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<CommonResDto> getUserHistory(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Fetching history for user: {}", userId);

        Page<HistoryResponse> history = historyService.getUserHistory(userId, page, size);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "사용자 히스토리 조회 성공", history));
    }


    /**
     * 내 히스토리 조회 (현재 로그인한 사용자)
     */
    @GetMapping("/my")
    public ResponseEntity<CommonResDto> getMyHistory(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Fetching history for current user");

        Page<HistoryResponse> history = historyService.getMyHistory(page, size);  // 변경!

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "내 히스토리 조회 성공", history));
    }
}