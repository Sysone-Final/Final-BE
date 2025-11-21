package org.example.finalbe.domains.alert.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.AlertStatus;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long id;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "rack_id")
    private Long rackId;

    @Column(name = "server_room_id")
    private Long serverRoomId;

    @Column(name = "data_center_id")
    private Long dataCenterId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", nullable = false, length = 20)
    private AlertLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.TRIGGERED;

    @Column(name = "measured_value", nullable = false)
    private Double measuredValue;

    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void acknowledge(Long userId) {
        if (this.status == AlertStatus.TRIGGERED) {
            this.status = AlertStatus.ACKNOWLEDGED;
            this.acknowledgedAt = LocalDateTime.now();
            this.acknowledgedBy = userId;
        }
    }

    public void resolve(Long userId) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = userId;

        if (this.acknowledgedAt == null) {
            this.acknowledgedAt = LocalDateTime.now();
            this.acknowledgedBy = userId;
        }
    }

    public boolean isActive() {
        return status != AlertStatus.RESOLVED;
    }
}
