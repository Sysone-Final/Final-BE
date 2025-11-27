// 작성자: 최산하
// 메모리·스왑 집계 통계 DTO

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
public class MemoryAggregatedStatsDto {

    // 집계 시간
    private LocalDateTime timestamp;

    // 평균 메모리 사용률
    private Double avgMemoryUsage;

    // 최대 메모리 사용률
    private Double maxMemoryUsage;

    // 최소 메모리 사용률
    private Double minMemoryUsage;

    // 평균 스왑 사용률
    private Double avgSwapUsage;

    // 샘플 개수
    private Integer sampleCount;
}
