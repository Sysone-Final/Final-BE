package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CPU 사용률 포인트 DTO
 * 그래프 1.1: CPU 사용률 시계열 그래프용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuUsagePointDto {

    private LocalDateTime timestamp;

    /**
     * CPU 전체 사용률 (%)
     * 계산식: 100 - cpuIdle
     */
    private Double cpuUsagePercent;
}