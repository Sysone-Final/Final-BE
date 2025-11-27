// 작성자: 황요한
// 히스토리 조회 및 검색을 처리하는 컨트롤러

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

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {

    private final HistoryService historyService;

    // 서버실 기본 히스토리 조회
    @GetMapping("/serverroom/{serverRoomId}")
    public ResponseEntity<CommonResDto> getServerRoomHistory(
            @PathVariable Long serverRoomId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        HistorySearchRequest request = HistorySearchRequest.builder()
                .serverRoomId(serverRoomId)
                .page(page)
                .size(size)
                .build();

        Page<HistoryResponse> history = historyService.searchHistory(request);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "히스토리 조회 성공", history));
    }

    // 서버실 히스토리 조건 검색
    @GetMapping("/serverroom/{serverRoomId}/search")
    public ResponseEntity<CommonResDto> searchHistory(
            @PathVariable Long serverRoomId,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) HistoryAction action,
            @RequestParam(required = false) Long changedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

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

    // 특정 엔티티의 히스토리 조회
    @GetMapping("/entity")
    public ResponseEntity<CommonResDto> getEntityHistory(
            @RequestParam Long serverRoomId,
            @RequestParam EntityType entityType,
            @RequestParam Long entityId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        HistorySearchRequest base = HistorySearchRequest.ofEntity(serverRoomId, entityType, entityId);

        HistorySearchRequest request = HistorySearchRequest.builder()
                .serverRoomId(base.serverRoomId())
                .entityType(base.entityType())
                .entityId(base.entityId())
                .page(page)
                .size(size)
                .build();

        Page<HistoryResponse> history = historyService.searchHistory(request);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "엔티티 히스토리 조회 성공", history));
    }

    // 서버실 히스토리 통계 조회
    @GetMapping("/serverroom/{serverRoomId}/statistics")
    public ResponseEntity<CommonResDto> getStatistics(
            @PathVariable Long serverRoomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        HistoryStatisticsResponse statistics = historyService.getStatistics(
                serverRoomId, startDate, endDate);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "히스토리 통계 조회 성공", statistics));
    }

    // 히스토리 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getHistoryDetail(
            @PathVariable @Min(value = 1, message = "유효하지 않은 히스토리 ID입니다.") Long id) {

        HistoryDetailResponse history = historyService.getHistoryDetail(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "히스토리 상세 조회 완료", history));
    }

    // 특정 엔티티 전체 히스토리 조회
    @GetMapping("/entity/details")
    public ResponseEntity<CommonResDto> getHistoryByEntity(
            @RequestParam EntityType entityType,
            @RequestParam Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<HistoryDetailResponse> histories = historyService.getHistoryByEntity(
                entityType, entityId, page, size);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "엔티티 히스토리 조회 완료", histories));
    }

    // 현재 로그인 사용자가 접근 가능한 서버실 목록 조회
    @GetMapping("/my-serverrooms")
    public ResponseEntity<CommonResDto> getMyAccessibleServerRooms() {

        List<ServerRoomAccessResponse> serverRooms = historyService.getMyAccessibleServerRooms();

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "접근 가능한 서버실 목록 조회 성공", serverRooms));
    }
}
