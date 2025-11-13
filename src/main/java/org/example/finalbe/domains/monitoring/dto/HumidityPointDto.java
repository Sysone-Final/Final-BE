package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 습도 포인트 DTO
 * 습도 시계열 그래프용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumidityPointDto {

    private LocalDateTime timestamp;

    /**
     * 습도 (%)
     */
    private Double humidity;
}