package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 네트워크 사용률 포인트 DTO
 * 그래프 3.1, 3.2: RX/TX 사용률 (%) (라인 차트)
 * (모든 NIC의 사용률을 평균낸 값)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkUsagePointDto {

    private LocalDateTime timestamp;

    /**
     * 평균 수신(RX) 사용률 (%)
     */
    private Double rxUsage;

    /**
     * 평균 송신(TX) 사용률 (%)
     */
    private Double txUsage;
}