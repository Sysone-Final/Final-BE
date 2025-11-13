package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 현재 메모리/스왑 상태 DTO
 * 게이지 및 요약 정보 표시용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryCurrentStatsDto {

    /**
     * 현재 메모리 사용률 (%)
     */
    private Double currentMemoryUsage;

    /**
     * 평균 메모리 사용률 (%) - 조회 기간 동안
     */
    private Double avgMemoryUsage;

    /**
     * 최대 메모리 사용률 (%) - 조회 기간 동안
     */
    private Double maxMemoryUsage;

    /**
     * 최소 메모리 사용률 (%) - 조회 기간 동안
     */
    private Double minMemoryUsage;

    /**
     * 현재 스왑 사용률 (%)
     */
    private Double currentSwapUsage;

    /**
     * 사용 중인 메모리 (bytes)
     */
    private Long usedMemoryBytes;

    /**
     * 전체 메모리 (bytes)
     */
    private Long totalMemoryBytes;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime lastUpdated;
}