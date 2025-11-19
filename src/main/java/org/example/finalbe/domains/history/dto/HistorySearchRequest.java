package org.example.finalbe.domains.history.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;


import java.time.LocalDateTime;

/**
 * 히스토리 검색 요청 DTO
 */
@Builder
public record HistorySearchRequest(
        Long serverRoomId,      // 필수: 서버실 ID
        EntityType entityType,  // 선택: 엔티티 타입 필터
        Long entityId,          // 선택: 특정 엔티티 ID
        HistoryAction action,   // 선택: 작업 타입 필터
        Long changedBy,         // 선택: 변경자 필터
        LocalDateTime startDate, // 선택: 시작 날짜
        LocalDateTime endDate,   // 선택: 종료 날짜
        Integer page,           // 페이지 번호 (0부터 시작)
        Integer size            // 페이지 크기
) {
    public HistorySearchRequest {
        // 기본값 설정
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100; // 최대 100개로 제한
        }
    }

    /**
     * 기본 생성자 (서버실 ID만 필수)
     */
    public static HistorySearchRequest ofServerRoom(Long serverRoomId) {
        return HistorySearchRequest.builder()
                .serverRoomId(serverRoomId)
                .page(0)
                .size(20)
                .build();
    }

    /**
     * 특정 엔티티 히스토리 조회용
     */
    public static HistorySearchRequest ofEntity(Long serverRoomId, EntityType entityType, Long entityId) {
        return HistorySearchRequest.builder()
                .serverRoomId(serverRoomId)
                .entityType(entityType)
                .entityId(entityId)
                .page(0)
                .size(20)
                .build();
    }
}