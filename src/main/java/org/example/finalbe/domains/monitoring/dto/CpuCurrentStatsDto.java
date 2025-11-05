package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 현재 CPU 상태 DTO
 * 게이지 및 요약 정보 표시용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuCurrentStatsDto {

    /**
     * 현재 CPU 사용률 (%)
     */
    private Double currentCpuUsage;

    /**
     * 평균 CPU 사용률 (%) - 조회 기간 동안
     */
    private Double avgCpuUsage;

    /**
     * 최대 CPU 사용률 (%) - 조회 기간 동안
     */
    private Double maxCpuUsage;

    /**
     * 최소 CPU 사용률 (%) - 조회 기간 동안
     */
    private Double minCpuUsage;

    /**
     * 현재 1분 평균 부하
     */
    private Double currentLoadAvg1;

    /**
     * 현재 5분 평균 부하
     */
    private Double currentLoadAvg5;

    /**
     * 현재 15분 평균 부하
     */
    private Double currentLoadAvg15;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime lastUpdated;
}