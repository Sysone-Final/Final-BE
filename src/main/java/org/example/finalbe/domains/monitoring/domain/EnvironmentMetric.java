package org.example.finalbe.domains.monitoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "environment_metrics", indexes = {
        @Index(name = "idx_rack_env_time", columnList = "rackId,generateTime"),
        @Index(name = "idx_generate_time_env", columnList = "generateTime")
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

    // ==================== 온도 관련 ====================
    @Column(nullable = false)
    private Double temperature;           // 현재 온도 (°C)

    private Double minTemperature;        // 최저 온도 (°C)

    private Double maxTemperature;        // 최고 온도 (°C)

    // ==================== 습도 관련 ====================
    @Column(nullable = false)
    private Double humidity;              // 현재 습도 (%)

    private Double minHumidity;           // 최저 습도 (%)

    private Double maxHumidity;           // 최고 습도 (%)

    // ==================== 알람 상태 ====================
    private Boolean temperatureWarning;   // 온도 경고 여부

    private Boolean humidityWarning;      // 습도 경고 여부
}