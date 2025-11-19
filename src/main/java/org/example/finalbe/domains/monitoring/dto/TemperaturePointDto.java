package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 온도 포인트 DTO
 * 온도 시계열 그래프용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemperaturePointDto {

    private LocalDateTime timestamp;

    /**
     * 온도 (°C)
     */
    private Double temperature;
}