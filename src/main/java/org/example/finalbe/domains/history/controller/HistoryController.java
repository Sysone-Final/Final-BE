package org.example.finalbe.domains.history.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.history.dto.*;
import org.example.finalbe.domains.history.service.HistoryService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
     * GET /api/history/serverroom/{serverRoomId}
     *
     * @param serverRoomId 서버실 ID
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20)
     * @return 히스토리 목록
     */
    @GetMapping("/serverroom/{serverRoomId}")
    public ResponseEntity<CommonResDto> getServerRoomHistory(
            @PathVariable Long serverRoomId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Fetching history for server room: {}", serverRoomId);

        HistorySearchRequest request = HistorySearchRequest.builder()
                .serverRoomId(serverRoomId)
                .page(page)
                .size(size)
                .build();

        Page<HistoryResponse> history = historyService.searchHistory(request);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "히스토리 조회 성공", history));
    }

    /**
     * 서버실 히스토리 검색 (복합 조건)
     * GET /api/history/serverroom/{serverRoomId}/search
     *
     * @param serverRoomId 서버실 ID (필수)
     * @param entityType 엔티티 타입 (선택)
     * @param action 작업 타입 (선택)
     * @param changedBy 변경자 ID (선택)
     * @param startDate 시작 날짜 (선택)
     * @param endDate 종료 날짜 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 필터링된 히스토리 목록
     */
    @GetMapping("/serverroom/{serverRoomId}/search")
    public ResponseEntity<CommonResDto> searchHistory(
            @PathVariable Long serverRoomId,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) HistoryAction action,
            @RequestParam(required = false) Long changedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Searching history for server room: {} with filters", serverRoomId);

        HistorySearchRequest request = HistorySearchRequest.builder()
                .serverRoomId(serverRoomId)
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
     * @param serverRoomId 서버실 ID
     * @param entityType 엔티티 타입 (RACK, EQUIPMENT, DEVICE)
     * @param entityId 엔티티 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 특정 엔티티의 변경 이력
     */
    @GetMapping("/entity")
    public ResponseEntity<CommonResDto> getEntityHistory(
            @RequestParam Long serverRoomId,
            @RequestParam EntityType entityType,
            @RequestParam Long entityId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        log.info("Fetching history for entity: {} {} in server room: {}",
                entityType, entityId, serverRoomId);

        HistorySearchRequest request = HistorySearchRequest.ofEntity(serverRoomId, entityType, entityId);
        request = HistorySearchRequest.builder()
                .serverRoomId(request.serverRoomId())
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
     * GET /api/history/serverroom/{serverRoomId}/statistics
     *
     * @param serverRoomId 서버실 ID
     * @param startDate 시작 날짜 (기본: 30일 전)
     * @param endDate 종료 날짜 (기본: 현재)
     * @return 서버실 변경 통계
     */
    @GetMapping("/serverroom/{serverRoomId}/statistics")
    public ResponseEntity<CommonResDto> getStatistics(
            @PathVariable Long serverRoomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Fetching statistics for server room: {}", serverRoomId);

        HistoryStatisticsResponse statistics = historyService.getHistoryStatistics(
                serverRoomId, startDate, endDate);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "히스토리 통계 조회 성공", statistics));
    }

    /**
     * 히스토리 상세 조회
     * GET /api/history/{id}
     *
     * @param id 히스토리 ID
     * @return 히스토리 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getHistoryDetail(
            @PathVariable @Min(value = 1, message = "유효하지 않은 히스토리 ID입니다.") Long id) {

        HistoryDetailResponse history = historyService.getHistoryDetail(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "히스토리 상세 조회 완료", history));
    }

    /**
     * 엔티티별 히스토리 목록 조회
     * GET /api/history/entity/details
     *
     * 특정 엔티티(예: 특정 랙)의 모든 히스토리를 조회
     */
    @GetMapping("/entity/details")
    public ResponseEntity<CommonResDto> getHistoryByEntity(
            @RequestParam EntityType entityType,
            @RequestParam Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<HistoryDetailResponse> histories = historyService.getHistoryByEntity(
                entityType, entityId, page, size);

        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "엔티티 히스토리 조회 완료", histories));
    }

    /**
     * 내가 접근 가능한 서버실 목록 조회
     * GET /api/history/my-serverrooms
     *
     * 사용 목적:
     * - 프론트엔드에서 히스토리 조회 시 서버실 선택 드롭다운에 사용
     * - 사용자가 소속된 부서가 담당하는 랙이 있는 서버실만 표시
     *
     * @return 접근 가능한 서버실 목록
     */
    @GetMapping("/my-serverrooms")
    public ResponseEntity<CommonResDto> getMyAccessibleServerRooms() {
        log.info("Fetching accessible server rooms for current user");

        List<ServerRoomAccessResponse> serverRooms = historyService.getMyAccessibleServerRooms();

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "접근 가능한 서버실 목록 조회 성공", serverRooms));
    }
}