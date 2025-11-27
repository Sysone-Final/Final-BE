// 작성자: 최산하
// 메모리 대시보드 전체 데이터를 담는 DTO

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
public class MemorySectionResponseDto {

    // 현재 메모리 및 스왑 상태
    private MemoryCurrentStatsDto currentStats;

    // 메모리 사용률 추이 데이터
    private List<MemoryUsagePointDto> memoryUsageTrend;

    // 메모리 구성 요소 추이 데이터
    private List<MemoryCompositionPointDto> memoryCompositionTrend;

    // 스왑 사용률 추이 데이터
    private List<SwapUsagePointDto> swapUsageTrend;
}
