package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시스템 부하 포인트 DTO
 * 그래프 1.3: 시스템 부하 추이 (라인 차트)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadAveragePointDto {

    private LocalDateTime timestamp;

    /**
     * 1분 평균 시스템 부하
     */
    private Double loadAvg1;

    /**
     * 5분 평균 시스템 부하
     */
    private Double loadAvg5;

    /**
     * 15분 평균 시스템 부하
     */
    private Double loadAvg15;
}