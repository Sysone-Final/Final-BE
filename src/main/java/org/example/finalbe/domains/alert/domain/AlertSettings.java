/**
 * 작성자: 황요한
 * 알림 관련 기본 임계치 및 설정값을 저장하는 엔티티
 */
package org.example.finalbe.domains.alert.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long id;

    // 기본 연속 발생 횟수
    @Column(nullable = false)
    @Builder.Default
    private Integer defaultConsecutiveCount = 3;

    // 기본 쿨다운 시간 (분)
    @Column(nullable = false)
    @Builder.Default
    private Integer defaultCooldownMinutes = 10;

    // 네트워크 에러율 기준
    @Builder.Default
    private Double networkErrorRateWarning = 0.1;

    @Builder.Default
    private Double networkErrorRateCritical = 1.0;

    // 네트워크 드롭율 기준
    @Builder.Default
    private Double networkDropRateWarning = 0.1;

    @Builder.Default
    private Double networkDropRateCritical = 1.0;

    // 생성/수정 시간
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
