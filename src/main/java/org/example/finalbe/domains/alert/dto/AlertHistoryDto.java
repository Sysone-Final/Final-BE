package org.example.finalbe.domains.alert.dto;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;

import java.time.LocalDateTime;

public record AlertHistoryDto(
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
        Long readBy,

        // 메시지
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