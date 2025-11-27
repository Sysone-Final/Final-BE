// 작성자: 최산하
// 환경 섹션 전체 응답 DTO

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentSectionResponseDto {

    // 현재 환경 상태
    private EnvironmentCurrentStatsDto currentStats;

    // 온도 추이
    private List<TemperaturePointDto> temperatureTrend;

    // 습도 추이
    private List<HumidityPointDto> humidityTrend;
}
