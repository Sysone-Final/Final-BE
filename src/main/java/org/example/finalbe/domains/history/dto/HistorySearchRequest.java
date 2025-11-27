/**
 * 작성자: 황요한
 * 히스토리 검색 요청 DTO
 */
package org.example.finalbe.domains.history.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;

import java.time.LocalDateTime;

@Builder
public record HistorySearchRequest(
        Long serverRoomId,
        EntityType entityType,
        Long entityId,
        HistoryAction action,
        Long changedBy,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer page,
        Integer size
) {
    public HistorySearchRequest {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
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