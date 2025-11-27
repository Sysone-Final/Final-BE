/**
 * 작성자: 황요한
 * 알림 히스토리 응답 DTO
 */
package org.example.finalbe.domains.alert.dto;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;

import java.time.LocalDateTime;

public record AlertHistoryDto(
        Long alertId,

        Long equipmentId,
        Long rackId,
        Long serverRoomId,
        Long dataCenterId,
        String targetName,
        TargetType targetType,

        MetricType metricType,
        String metricName,

        AlertLevel level,
        Double measuredValue,
        Double thresholdValue,

        LocalDateTime triggeredAt,

        Boolean isRead,
        LocalDateTime readAt,
        Long readBy,

        String message,
        String additionalInfo,

        LocalDateTime createdAt
) {
    public static AlertHistoryDto from(AlertHistory alert) {
        return new AlertHistoryDto(
                alert.getId(),
                alert.getEquipmentId(),
                alert.getRackId(),
                alert.getServerRoomId(),
                alert.getDataCenterId(),
                alert.getTargetName(),
                alert.getTargetType(),
                alert.getMetricType(),
                alert.getMetricName(),
                alert.getLevel(),
                alert.getMeasuredValue(),
                alert.getThresholdValue(),
                alert.getTriggeredAt(),
                alert.getIsRead(),
                alert.getReadAt(),
                alert.getReadBy(),
                alert.getMessage(),
                alert.getAdditionalInfo(),
                alert.getCreatedAt()
        );
    }
}
