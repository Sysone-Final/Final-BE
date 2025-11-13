package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 현재 디스크 상태 DTO
 * 게이지 및 요약 정보 표시용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskCurrentStatsDto {

    /**
     * 현재 디스크 사용률 (%)
     */
    private Double currentUsagePercent;

    /**
     * 평균 디스크 사용률 (%) - 조회 기간 동안
     */
    private Double avgUsagePercent;

    /**
     * 최대 디스크 사용률 (%) - 조회 기간 동안
     */
    private Double maxUsagePercent;

    /**
     * 최소 디스크 사용률 (%) - 조회 기간 동안
     */
    private Double minUsagePercent;

    /**
     * 현재 inode 사용률 (%)
     */
    private Double currentInodeUsagePercent;

    /**
     * 현재 디스크 I/O 사용률 (%)
     */
    private Double currentIoTimePercent;

    /**
     * 사용 중인 용량 (bytes)
     */
    private Long usedBytes;

    /**
     * 전체 용량 (bytes)
     */
    private Long totalBytes;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime lastUpdated;
}