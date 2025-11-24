package org.example.finalbe.domains.alert.dto;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;

import java.time.LocalDateTime;

public record AlertNotificationDto(
        Long alertId,

        // 대상 정보
        Long equipmentId,
        Long rackId,
        Long serverRoomId,
        Long dataCenterId,
        String targetName,
        TargetType targetType,

        // 메트릭 정보
        MetricType metricType,
        String metricName,

        // 알림 정보
        AlertLevel level,
        Double measuredValue,
        Double thresholdValue,

        // 시간 정보
        LocalDateTime triggeredAt,

        // 읽음 정보
        Boolean isRead,
        LocalDateTime readAt,

        // 메시지
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