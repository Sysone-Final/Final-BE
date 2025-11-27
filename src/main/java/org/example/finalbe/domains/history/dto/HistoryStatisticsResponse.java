/**
 * 작성자: 황요한
 * 히스토리 통계 응답 DTO
 */
package org.example.finalbe.domains.history.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Builder
public record HistoryStatisticsResponse(
        Long serverRoomId,
        String serverRoomName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Long totalCount,
        Map<HistoryAction, Long> actionCounts,
        Map<EntityType, Long> entityTypeCounts,
        List<TopActiveEntity> topActiveEntities,
        List<TopActiveUser> topActiveUsers
) {
    /**
     * 최근 활동 많은 자산
     */
    @Builder
    public record TopActiveEntity(
            EntityType entityType,
            Long entityId,
            String entityName,
            Long changeCount
    ) {}

    /**
     * 최근 활동 많은 사용자
     */
    @Builder
    public record TopActiveUser(
            Long userId,
            String userName,
            Long changeCount
    ) {}
}