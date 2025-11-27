/**
 * 작성자: 황요한
 * 알림 히스토리 엔티티 (알림 기록 및 읽음 처리 기능 포함)
 */
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

    private Long equipmentId;
    private Long rackId;
    private Long serverRoomId;
    private Long dataCenterId;

    @Column(length = 200)
    private String targetName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MetricType metricType;

    @Column(nullable = false, length = 100)
    private String metricName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertLevel level;

    @Column(nullable = false)
    private Double measuredValue;

    @Column(nullable = false)
    private Double thresholdValue;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    // 읽음 처리 필드
    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;

    private LocalDateTime readAt;
    private Long readBy;

    @Column(length = 500)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
