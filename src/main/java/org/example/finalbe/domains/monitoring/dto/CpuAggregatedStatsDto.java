// 작성자: 황요한
// CPU 집계 통계를 담는 DTO

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
public class CpuAggregatedStatsDto {

    // 집계 기준 시간
    private LocalDateTime timestamp;

    // 평균 CPU 사용률
    private Double avgCpuUsage;

    // 최대 CPU 사용률
    private Double maxCpuUsage;

    // 최소 CPU 사용률
    private Double minCpuUsage;

    // 평균 1분 부하
    private Double avgLoadAvg1;

    // 집계 기간 동안의 총 컨텍스트 스위치 수
    private Long totalContextSwitches;

    // 집계된 샘플 개수
    private Integer sampleCount;
}
