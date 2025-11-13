package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 네트워크 트래픽 포인트 DTO
 * 그래프 3.7: 초당 전송량 (bytes/sec) (라인 차트)
 * (모든 NIC의 트래픽을 합산한 값)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkTrafficPointDto {

    private LocalDateTime timestamp;

    /**
     * 총 수신 속도 (bytes/sec)
     */
    private Double inBps;

    /**
     * 총 송신 속도 (bytes/sec)
     */
    private Double outBps;
}