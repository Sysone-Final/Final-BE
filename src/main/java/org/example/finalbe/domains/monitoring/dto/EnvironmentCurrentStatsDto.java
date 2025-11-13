package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 현재 환경 상태 DTO
 * 게이지 및 요약 정보 표시용 (랙 기준)rackId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentCurrentStatsDto {

    /**
     * 현재 온도 (°C)
     */
    private Double currentTemperature;

    /**
     * 평균 온도 (°C) - 조회 기간 동안
     */
    private Double avgTemperature;

    /**
     * 최대 온도 (°C) - 조회 기간 동안
     */
    private Double maxTemperature;

    /**
     * 최소 온도 (°C) - 조회 기간 동안
     */
    private Double minTemperature;

    /**
     * 현재 습도 (%)
     */
    private Double currentHumidity;

    /**
     * 온도 경고 여부
     */
    private Boolean temperatureWarning;

    /**
     * 습도 경고 여부
     */
    private Boolean humidityWarning;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime lastUpdated;
}