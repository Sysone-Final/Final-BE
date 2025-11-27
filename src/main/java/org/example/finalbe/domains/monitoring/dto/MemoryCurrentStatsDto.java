// 작성자: 최산하
// 현재 메모리 및 스왑 상태 정보를 담는 DTO

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
public class MemoryCurrentStatsDto {

    // 현재 메모리 사용률 (%)
    private Double currentMemoryUsage;

    // 조회 기간 평균 메모리 사용률 (%)
    private Double avgMemoryUsage;

    // 조회 기간 최대 메모리 사용률 (%)
    private Double maxMemoryUsage;

    // 조회 기간 최소 메모리 사용률 (%)
    private Double minMemoryUsage;

    // 현재 스왑 사용률 (%)
    private Double currentSwapUsage;

    // 사용 중인 메모리 (bytes)
    private Long usedMemoryBytes;

    // 전체 메모리 (bytes)
    private Long totalMemoryBytes;

    // 마지막 업데이트 시간
    private LocalDateTime lastUpdated;
}
