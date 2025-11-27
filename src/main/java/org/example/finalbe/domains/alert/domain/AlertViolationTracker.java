/**
 * 작성자: 황요한
 * 알림 연속 위반 상태를 추적하는 엔티티
 */
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

    private Long equipmentId;
    private Long rackId;
    private Long serverRoomId;
    private Long dataCenterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MetricType metricType;

    @Column(nullable = false, length = 100)
    private String metricName;

    // 연속 위반 횟수
    @Builder.Default
    @Column(nullable = false)
    private Integer consecutiveViolations = 0;

    // 마지막 위반 발생 시간
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime lastViolationTime = LocalDateTime.now();

    // 가장 최근 측정값
    private Double lastMeasuredValue;

    // 마지막으로 알림 전송된 시간
    private LocalDateTime lastAlertSentAt;

    // 생성 및 수정 시간
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
