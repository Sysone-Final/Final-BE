// 작성자: 최산하
// 환경 집계 통계 DTO (시간대별 온도/습도 집계 데이터)

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentAggregatedStatsDto {

    // 집계 시간
    private LocalDateTime timestamp;

    // 평균 온도
    private Double avgTemperature;

    // 최대 온도
    private Double maxTemperature;

    // 최소 온도
    private Double minTemperature;

    // 평균 습도
    private Double avgHumidity;

    // 집계된 샘플 수
    private Integer sampleCount;
}
