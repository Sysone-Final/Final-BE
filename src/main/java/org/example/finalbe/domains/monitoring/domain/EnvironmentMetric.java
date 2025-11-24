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

    // ==================== 전체 경고 상태 (계산 필드) ====================
    /**
     * 온도 또는 습도 경고가 있는지 확인
     * DB에 저장되지 않는 계산 필드
     */
    @Transient
    public Boolean getIsWarning() {
        return Boolean.TRUE.equals(temperatureWarning) ||
                Boolean.TRUE.equals(humidityWarning);
    }

    // ==================== 헬퍼 메서드 ====================
    /**
     * 임계치 상수
     */
    private static final double TEMP_MAX_THRESHOLD = 28.0;
    private static final double TEMP_MIN_THRESHOLD = 18.0;
    private static final double HUMIDITY_MAX_THRESHOLD = 70.0;
    private static final double HUMIDITY_MIN_THRESHOLD = 30.0;

    /**
     * 온도와 습도 값을 기반으로 경고 상태를 자동으로 설정
     * 데이터 저장 전에 호출하면 유용
     */
    public void updateWarningFlags() {
        if (temperature != null) {
            this.temperatureWarning = temperature > TEMP_MAX_THRESHOLD ||
                    temperature < TEMP_MIN_THRESHOLD;
        }

        if (humidity != null) {
            this.humidityWarning = humidity > HUMIDITY_MAX_THRESHOLD ||
                    humidity < HUMIDITY_MIN_THRESHOLD;
        }
    }

    /**
     * 빌더 패턴 사용 시 경고 플래그 자동 설정을 위한 정적 메서드
     */
    public static EnvironmentMetric createWithWarnings(
            Long rackId,
            LocalDateTime generateTime,
            Double temperature,
            Double minTemperature,
            Double maxTemperature,
            Double humidity,
            Double minHumidity,
            Double maxHumidity) {

        EnvironmentMetric metric = EnvironmentMetric.builder()
                .rackId(rackId)
                .generateTime(generateTime)
                .temperature(temperature)
                .minTemperature(minTemperature)
                .maxTemperature(maxTemperature)
                .humidity(humidity)
                .minHumidity(minHumidity)
                .maxHumidity(maxHumidity)
                .build();

        metric.updateWarningFlags();
        return metric;
    }
}