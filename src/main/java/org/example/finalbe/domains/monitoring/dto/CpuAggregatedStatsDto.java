package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CPU 집계 통계 DTO
 * 시간대별 CPU 통계 (1분/5분/1시간 단위 집계)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuAggregatedStatsDto {

    /**
     * 집계 시간 버킷 (time_bucket)
     */
    private LocalDateTime timestamp;

    /**
     * 평균 CPU 사용률 (%)
     */
    private Double avgCpuUsage;

    /**
     * 최대 CPU 사용률 (%)
     */
    private Double maxCpuUsage;

    /**
     * 최소 CPU 사용률 (%)
     */
    private Double minCpuUsage;

    /**
     * 평균 1분 부하
     */
    private Double avgLoadAvg1;

    /**
     * 총 컨텍스트 스위치 횟수 (집계 기간 동안)
     */
    private Long totalContextSwitches;

    /**
     * 집계된 샘플 개수
     */
    private Integer sampleCount;
}