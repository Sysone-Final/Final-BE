// 작성자: 황요한
// 환경 메트릭 엔티티 (랙 기준 온도/습도 + 경고 상태 저장)

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

    // 온도 정보
    private Double temperature;
    private Double minTemperature;
    private Double maxTemperature;

    // 습도 정보
    private Double humidity;
    private Double minHumidity;
    private Double maxHumidity;

    // 경고 상태
    private Boolean temperatureWarning;
    private Boolean humidityWarning;

    // 계산 필드: 온도 또는 습도 경고 여부
    @Transient
    public Boolean getIsWarning() {
        return Boolean.TRUE.equals(temperatureWarning) ||
                Boolean.TRUE.equals(humidityWarning);
    }

}
