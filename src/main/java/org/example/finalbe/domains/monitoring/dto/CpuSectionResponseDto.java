// 작성자: 황요한
// CPU 대시보드 섹션 전체 응답 DTO

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
public class CpuSectionResponseDto {

    // 현재 CPU 상태 요약
    private CpuCurrentStatsDto currentStats;

    // CPU 사용률 추이
    private List<CpuUsagePointDto> cpuUsageTrend;

    // CPU 모드별 분포
    private List<CpuModeDistributionDto> cpuModeDistribution;

    // 시스템 부하 추이
    private List<LoadAveragePointDto> loadAverageTrend;

    // 컨텍스트 스위치 추이
    private List<ContextSwitchPointDto> contextSwitchTrend;
}
