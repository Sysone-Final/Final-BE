package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 네트워크 에러 패킷 포인트 DTO
 * 그래프 3.8: 에러/드롭 패킷 (라인 차트)
 * (모든 NIC의 누적 에러 패킷을 합산한 값)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkErrorPointDto {

    private LocalDateTime timestamp;

    /**
     * 총 수신 에러 패킷 (누적)
     */
    private Long inErrors;

    /**
     * 총 송신 에러 패킷 (누적)
     */
    private Long outErrors;

    /**
     * 총 수신 드롭 패킷 (누적)
     */
    private Long inDiscards;

    /**
     * 총 송신 드롭 패킷 (누적)
     */
    private Long outDiscards;
}