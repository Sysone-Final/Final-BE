package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 현재 네트워크 상태 DTO
 * 게이지 및 요약 정보 표시용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkCurrentStatsDto {

    /**
     * 현재 총 수신 속도 (모든 NIC 합산, bytes/sec)
     */
    private Double currentInBps;

    /**
     * 현재 총 송신 속도 (모든 NIC 합산, bytes/sec)
     */
    private Double currentOutBps;

    /**
     * 평균 수신 사용률 (모든 NIC 평균, %) - 조회 기간 동안
     */
    private Double avgRxUsage;

    /**
     * 최대 수신 사용률 (NIC 중 최대, %) - 조회 기간 동안
     */
    private Double maxRxUsage;

    /**
     * 최소 수신 사용률 (NIC 중 최소, %) - 조회 기간 동안
     */
    private Double minRxUsage;

    /**
     * 현재 총 수신 에러 패킷 (누적)
     */
    private Long totalInErrors;

    /**
     * 현재 총 송신 에러 패킷 (누적)
     */
    private Long totalOutErrors;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime lastUpdated;
}