package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메모리 사용률 포인트 DTO
 * 그래프 2.1: 메모리 사용률 시계열 그래프용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryUsagePointDto {

    private LocalDateTime timestamp;

    /**
     * 메모리 전체 사용률 (%)
     */
    private Double memoryUsagePercent;
}