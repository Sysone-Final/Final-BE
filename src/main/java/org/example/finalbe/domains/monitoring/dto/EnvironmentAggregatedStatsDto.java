package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 환경 집계 통계 DTO
 * 시간대별 온도/습도 통계 (1분/5분/1시간 단위 집계)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentAggregatedStatsDto {

    /**
     * 집계 시간 버킷 (time_bucket)
     */
    private LocalDateTime timestamp;

    /**
     * 평균 온도 (°C)
     */
    private Double avgTemperature;

    /**
     * 최대 온도 (°C)
     */
    private Double maxTemperature;

    /**
     * 최소 온도 (°C)
     */
    private Double minTemperature;

    /**
     * 평균 습도 (%)
     */
    private Double avgHumidity;

    /**
     * 집계된 샘플 개수
     */
    private Integer sampleCount;
}