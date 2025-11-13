package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 네트워크 집계 통계 DTO
 * 시간대별 네트워크 통계 (1분/5분/1시간 단위 집계)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkAggregatedStatsDto {

    /**
     * 집계 시간 버킷 (time_bucket)
     */
    private LocalDateTime timestamp;

    /**
     * 총 수신 속도 (모든 NIC 합산, bytes/sec)
     */
    private Double totalInBps;

    /**
     * 총 송신 속도 (모든 NIC 합산, bytes/sec)
     */
    private Double totalOutBps;

    /**
     * 평균 수신 사용률 (모든 NIC 평균, %)
     */
    private Double avgRxUsage;

    /**
     * 평균 송신 사용률 (모든 NIC 평균, %)
     */
    private Double avgTxUsage;

    /**
     * 집계된 샘플 개수
     */
    private Integer sampleCount;
}