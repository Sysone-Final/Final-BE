package org.example.finalbe.domains.alert.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;


import java.time.LocalDateTime;

@Entity
@Table(name = "alert_violation_tracker")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertViolationTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracker_id")
    private Long id;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "rack_id")
    private Long rackId;

    @Column(name = "server_room_id")
    private Long serverRoomId;

    @Column(name = "data_center_id")
    private Long dataCenterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private MetricType metricType;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "consecutive_violations", nullable = false)
    @Builder.Default
    private Integer consecutiveViolations = 0;

    @Column(name = "last_violation_time", nullable = false)
    @Builder.Default
    private LocalDateTime lastViolationTime = LocalDateTime.now();

    @Column(name = "last_measured_value")
    private Double lastMeasuredValue;

    @Column(name = "last_alert_sent_at")
    private LocalDateTime lastAlertSentAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}