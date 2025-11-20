package org.example.finalbe.domains.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.AlertStatus;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;


import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotificationDto {

    private Long alertId;

    // 대상 정보
    private Long equipmentId;
    private Long rackId;
    private Long serverRoomId;
    private Long dataCenterId;
    private String targetName;
    private TargetType targetType;

    // 메트릭 정보
    private MetricType metricType;
    private String metricName;

    // 알림 정보
    private AlertLevel level;
    private AlertStatus status;
    private Double measuredValue;
    private Double thresholdValue;

    // 시간 정보
    private LocalDateTime triggeredAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;

    // 메시지
    private String message;

    public static AlertNotificationDto from(AlertHistory alert) {
        return AlertNotificationDto.builder()
                .alertId(alert.getId())
                .equipmentId(alert.getEquipmentId())
                .rackId(alert.getRackId())
                .serverRoomId(alert.getServerRoomId())
                .dataCenterId(alert.getDataCenterId())
                .targetName(alert.getTargetName())
                .targetType(alert.getTargetType())
                .metricType(alert.getMetricType())
                .metricName(alert.getMetricName())
                .level(alert.getLevel())
                .status(alert.getStatus())
                .measuredValue(alert.getMeasuredValue())
                .thresholdValue(alert.getThresholdValue())
                .triggeredAt(alert.getTriggeredAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .resolvedAt(alert.getResolvedAt())
                .message(alert.getMessage())
                .build();
    }
}