package org.example.finalbe.domains.alert.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
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

    @Column(name = "measured_value", nullable = false)
    private Double measuredValue;

    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    // 읽음 처리 관련 필드
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "read_by")
    private Long readBy;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 읽음 처리
     */
    public void markAsRead(Long userId) {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        this.readBy = userId;
    }
}