package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 스왑 사용률 포인트 DTO
 * 그래프 2.3: 스왑 사용률 추이 (라인 차트)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwapUsagePointDto {

    private LocalDateTime timestamp;

    /**
     * 스왑 사용률 (%)
     */
    private Double swapUsagePercent;
}