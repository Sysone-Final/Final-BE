/**
 * 작성자: 최산하
 * 네트워크 트래픽 시계열 포인트 DTO
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
public class NetworkTrafficPointDto {

    private LocalDateTime timestamp; // 시점
    private Double inBps;            // 총 수신 속도(bytes/sec)
    private Double outBps;           // 총 송신 속도(bytes/sec)
}
