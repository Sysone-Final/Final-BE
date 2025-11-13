package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메모리 집계 통계 DTO
 * 시간대별 메모리/스왑 통계 (1분/5분/1시간 단위 집계)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryAggregatedStatsDto {

    /**
     * 집계 시간 버킷 (time_bucket)
     */
    private LocalDateTime timestamp;

    /**
     * 평균 메모리 사용률 (%)
     */
    private Double avgMemoryUsage;

    /**
     * 최대 메모리 사용률 (%)
     */
    private Double maxMemoryUsage;

    /**
     * 최소 메모리 사용률 (%)
     */
    private Double minMemoryUsage;

    /**
     * 평균 스왑 사용률 (%)
     */
    private Double avgSwapUsage;

    /**
     * 집계된 샘플 개수
     */
    private Integer sampleCount;
}