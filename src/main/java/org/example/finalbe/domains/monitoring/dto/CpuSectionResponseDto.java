package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CPU 섹션 전체 응답 DTO
 * CPU 대시보드 섹션의 모든 그래프 데이터 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuSectionResponseDto {

    /**
     * 현재 CPU 상태 (게이지 및 요약 정보)
     */
    private CpuCurrentStatsDto currentStats;

    /**
     * 그래프 1.1: CPU 사용률 추이
     */
    private List<CpuUsagePointDto> cpuUsageTrend;

    /**
     * 그래프 1.2: CPU 모드별 분포 (적층 영역 차트)
     */
    private List<CpuModeDistributionDto> cpuModeDistribution;

    /**
     * 그래프 1.3: 시스템 부하 추이
     */
    private List<LoadAveragePointDto> loadAverageTrend;

    /**
     * 그래프 1.4: 컨텍스트 스위치 추이
     */
    private List<ContextSwitchPointDto> contextSwitchTrend;
}