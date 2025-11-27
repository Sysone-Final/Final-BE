/**
 * 작성자: 최산하
 * 네트워크 집계 통계 DTO
 */
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
public class NetworkAggregatedStatsDto {

    // 집계 시간
    private LocalDateTime timestamp;

    // 총 수신 속도 (bytes/sec)
    private Double totalInBps;

    // 총 송신 속도 (bytes/sec)
    private Double totalOutBps;

    // 평균 수신 사용률 (%)
    private Double avgRxUsage;

    // 평균 송신 사용률 (%)
    private Double avgTxUsage;

    // 집계된 샘플 수
    private Integer sampleCount;
}
