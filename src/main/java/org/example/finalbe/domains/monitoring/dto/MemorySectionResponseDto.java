package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 메모리 섹션 전체 응답 DTO
 * 메모리 대시보드 섹션의 모든 그래프 데이터 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorySectionResponseDto {

    /**
     * 현재 메모리/스왑 상태 (게이지 및 요약 정보)
     */
    private MemoryCurrentStatsDto currentStats;

    /**
     * 그래프 2.1: 메모리 사용률 추이
     */
    private List<MemoryUsagePointDto> memoryUsageTrend;

    /**
     * 그래프 2.2: 메모리 구성 요소 (적층 영역 차트)
     */
    private List<MemoryCompositionPointDto> memoryCompositionTrend;

    /**
     * 그래프 2.3: 스왑 사용률 추이
     */
    private List<SwapUsagePointDto> swapUsageTrend;
}