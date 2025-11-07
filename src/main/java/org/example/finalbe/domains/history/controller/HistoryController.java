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

    /**
     * 히스토리 상세 조회 (변경 내역 포함)
     * GET /api/history/{id}
     *
     * 사용 예시:
     * GET /api/history/123
     *
     * 응답:
     * {
     *   "id": 123,
     *   "entityName": "랙 A-01",
     *   "action": "UPDATE",
     *   "actionName": "수정",
     *   "changedByName": "홍길동",
     *   "changedAt": "2025-11-06T10:30:00",
     *   "changeDetails": [
     *     {
     *       "fieldLabel": "랙 위치",
     *       "oldValue": "5",
     *       "newValue": "10",
     *       "changeDescription": "랙 위치: 5 → 10"
     *     },
     *     {
     *       "fieldLabel": "랙 이름",
     *       "oldValue": "랙-A",
     *       "newValue": "랙-A-01",
     *       "changeDescription": "랙 이름: 랙-A → 랙-A-01"
     *     }
     *   ]
     * }
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getHistoryDetail(
            @PathVariable @Min(value = 1, message = "유효하지 않은 히스토리 ID입니다.") Long id) {

        HistoryDetailResponse history = historyService.getHistoryDetail(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "히스토리 상세 조회 완료", history));
    }

    /**
     * 엔티티별 히스토리 목록 조회
     * GET /api/history/entity?entityType=RACK&entityId=123
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
     * GET /api/history/my-datacenters
     *
     * 사용 목적:
     * - 프론트엔드에서 히스토리 조회 시 서버실 선택 드롭다운에 사용
     * - 사용자가 소속된 부서가 담당하는 랙이 있는 서버실만 표시
     *
     * 사용 예시:
     * 1. 사용자가 히스토리 화면에 진입
     * 2. 이 API로 접근 가능한 서버실 목록 조회
     * 3. 드롭다운에서 서버실 선택
     * 4. 선택한 서버실의 히스토리 조회
     *
     * @return 접근 가능한 서버실 목록
     */
    @GetMapping("/my-serverRooms")
    public ResponseEntity<CommonResDto> getMyAccessibleDataCenters() {
        log.info("Fetching accessible datacenters for current user");

        List<ServerRoomAccessResponse> datacenters = historyService.getMyAccessibleDataCenters();

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "접근 가능한 서버실 목록 조회 성공", datacenters));
    }
}