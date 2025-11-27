/**
 * 작성자: 최산하
 * 온도 시계열 그래프 DTO
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
public class TemperaturePointDto {

    private LocalDateTime timestamp;
    private Double temperature;
}
