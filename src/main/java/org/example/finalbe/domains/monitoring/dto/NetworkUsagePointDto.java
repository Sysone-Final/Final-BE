/**
 * 작성자: 최산하
 * 네트워크 RX/TX 사용률 포인트 DTO
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
public class NetworkUsagePointDto {

    private LocalDateTime timestamp; // 시점
    private Double rxUsage;          // 수신 사용률(%)
    private Double txUsage;          // 송신 사용률(%)
}
