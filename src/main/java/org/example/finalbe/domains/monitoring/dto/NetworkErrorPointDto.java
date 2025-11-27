/**
 * 작성자: 최산하
 * 네트워크 에러/드롭 패킷 포인트 DTO
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
public class NetworkErrorPointDto {

    // 측정 시각
    private LocalDateTime timestamp;

    // 총 수신 에러 패킷
    private Long inErrors;

    // 총 송신 에러 패킷
    private Long outErrors;

    // 총 수신 드롭 패킷
    private Long inDiscards;

    // 총 송신 드롭 패킷
    private Long outDiscards;
}
