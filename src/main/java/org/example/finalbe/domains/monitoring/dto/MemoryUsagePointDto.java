// 작성자: 최산하
// 메모리 사용률 시계열 포인트 DTO

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
public class MemoryUsagePointDto {

    // 타임스탬프
    private LocalDateTime timestamp;

    // 메모리 사용률 (%)
    private Double memoryUsagePercent;
}
