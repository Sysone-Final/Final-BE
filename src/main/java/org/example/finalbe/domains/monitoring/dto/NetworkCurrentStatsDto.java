/**
 * 작성자: 최산하
 * 현재 네트워크 상태 DTO
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
public class NetworkCurrentStatsDto {

    // 현재 총 수신 속도 (bytes/sec)
    private Double currentInBps;

    // 현재 총 송신 속도 (bytes/sec)
    private Double currentOutBps;

    // 평균 수신 사용률 (%)
    private Double avgRxUsage;

    // 최대 수신 사용률 (%)
    private Double maxRxUsage;

    // 최소 수신 사용률 (%)
    private Double minRxUsage;

    // 총 수신 에러 패킷 수
    private Long totalInErrors;

    // 총 송신 에러 패킷 수
    private Long totalOutErrors;

    // 마지막 업데이트 시간
    private LocalDateTime lastUpdated;
}
