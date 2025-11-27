/**
 * 작성자: 최산하
 * 스왑 사용률 포인트 DTO
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
public class SwapUsagePointDto {

    private LocalDateTime timestamp;
    private Double swapUsagePercent;
}
