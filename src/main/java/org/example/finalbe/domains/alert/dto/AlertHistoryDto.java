package org.example.finalbe.domains.alert.dto;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.AlertStatus;
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
        AlertStatus status,
        Double measuredValue,
        Double thresholdValue,

        // 시간 정보
        LocalDateTime triggeredAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt,

        // 사용자 정보
        Long acknowledgedBy,
        Long resolvedBy,

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
                alert.getStatus(),
                alert.getMeasuredValue(),
                alert.getThresholdValue(),
                alert.getTriggeredAt(),
                alert.getAcknowledgedAt(),
                alert.getResolvedAt(),
                alert.getAcknowledgedBy(),
                alert.getResolvedBy(),
                alert.getMessage(),
                alert.getAdditionalInfo(),
                alert.getCreatedAt()
        );
    }
}