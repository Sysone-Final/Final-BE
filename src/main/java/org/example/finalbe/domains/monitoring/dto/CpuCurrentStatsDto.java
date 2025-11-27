// 작성자: 황요한
// CPU 현재 상태를 반환하는 DTO

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
public class CpuCurrentStatsDto {

    // 현재 CPU 사용률 (%)
    private Double currentCpuUsage;

    // 평균 CPU 사용률 (%)
    private Double avgCpuUsage;

    // 최대 CPU 사용률 (%)
    private Double maxCpuUsage;

    // 최소 CPU 사용률 (%)
    private Double minCpuUsage;

    // 현재 1분 부하
    private Double currentLoadAvg1;

    // 현재 5분 부하
    private Double currentLoadAvg5;

    // 현재 15분 부하
    private Double currentLoadAvg15;

    // 마지막 업데이트 시각
    private LocalDateTime lastUpdated;
}
