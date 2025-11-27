// 작성자: 황요한
// CPU 사용률 포인트 DTO

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
public class CpuUsagePointDto {

    // 시점
    private LocalDateTime timestamp;

    // CPU 사용률 (%)
    private Double cpuUsagePercent;
}
