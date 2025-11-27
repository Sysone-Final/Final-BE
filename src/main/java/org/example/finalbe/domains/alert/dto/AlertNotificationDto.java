/**
 * 작성자: 황요한
 * 실시간 알림(SSE) 전송용 DTO
 */
package org.example.finalbe.domains.alert.dto;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;

import java.time.LocalDateTime;

public record AlertNotificationDto(
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

        String message
) {
    public static AlertNotificationDto from(AlertHistory alert) {
        return new AlertNotificationDto(
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
                alert.getMessage()
        );
    }
}
