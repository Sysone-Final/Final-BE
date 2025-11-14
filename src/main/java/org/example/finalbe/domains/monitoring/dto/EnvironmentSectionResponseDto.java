package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 환경 섹션 전체 응답 DTO
 * 환경 대시보드 섹션의 모든 그래프 데이터 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentSectionResponseDto {

    /**
     * 현재 환경 상태 (게이지 및 요약 정보)
     */
    private EnvironmentCurrentStatsDto currentStats;

    /**
     * 온도 추이 그래프
     */
    private List<TemperaturePointDto> temperatureTrend;

    /**
     * 습도 추이 그래프
     */
    private List<HumidityPointDto> humidityTrend;
}