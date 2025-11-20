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

    @Column(name = "default_consecutive_count", nullable = false)
    @Builder.Default
    private Integer defaultConsecutiveCount = 3;

    @Column(name = "default_cooldown_minutes", nullable = false)
    @Builder.Default
    private Integer defaultCooldownMinutes = 10;

    @Column(name = "network_error_rate_warning")
    @Builder.Default
    private Double networkErrorRateWarning = 0.1;

    @Column(name = "network_error_rate_critical")
    @Builder.Default
    private Double networkErrorRateCritical = 1.0;

    @Column(name = "network_drop_rate_warning")
    @Builder.Default
    private Double networkDropRateWarning = 0.1;

    @Column(name = "network_drop_rate_critical")
    @Builder.Default
    private Double networkDropRateCritical = 1.0;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}