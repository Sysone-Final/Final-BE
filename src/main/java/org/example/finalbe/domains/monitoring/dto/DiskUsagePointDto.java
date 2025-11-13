package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 디스크 사용률 포인트 DTO
 * 그래프 4.1: 디스크 사용률 시계열 그래프용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskUsagePointDto {

    private LocalDateTime timestamp;

    /**
     * 디스크 전체 사용률 (%)
     */
    private Double usagePercent;
}