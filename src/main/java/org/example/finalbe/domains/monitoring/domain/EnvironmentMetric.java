package org.example.finalbe.domains.monitoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "environment_metrics", indexes = {
        @Index(name = "idx_env_rack_time", columnList = "rack_id, generate_time"),
        @Index(name = "idx_env_generate_time", columnList = "generate_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rackId;

    @Column(nullable = false)
    private LocalDateTime generateTime;

    // ==================== 온도 관련 (nullable로 변경) ====================
    @Column(nullable = true)
    private Double temperature;

    @Column(nullable = true)
    private Double minTemperature;

    @Column(nullable = true)
    private Double maxTemperature;

    // ==================== 습도 관련 (이미 nullable) ====================
    @Column(nullable = true)
    private Double humidity;

    @Column(nullable = true)
    private Double minHumidity;

    @Column(nullable = true)
    private Double maxHumidity;

    // ==================== 알람 상태 ====================
    @Column(nullable = true)
    private Boolean temperatureWarning;

    @Column(nullable = true)
    private Boolean humidityWarning;
}