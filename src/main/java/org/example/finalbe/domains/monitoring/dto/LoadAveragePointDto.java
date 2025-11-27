// 작성자: 황요한
// 시스템 부하 추이 포인트 DTO

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
public class LoadAveragePointDto {

    // 시간값
    private LocalDateTime timestamp;

    // 1분 평균 Load
    private Double loadAvg1;

    // 5분 평균 Load
    private Double loadAvg5;

    // 15분 평균 Load
    private Double loadAvg15;
}
