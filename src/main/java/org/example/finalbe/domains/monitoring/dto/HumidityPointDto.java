// 작성자: 최산하
// 습도 시계열 그래프 포인트 DTO

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
public class HumidityPointDto {

    // 시간값
    private LocalDateTime timestamp;

    // 습도 (%)
    private Double humidity;
}
