// 작성자: 최산하
// 디스크 시간대별 집계 통계 DTO

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
public class DiskAggregatedStatsDto {

    // 집계 시간 버킷
    private LocalDateTime timestamp;

    // 평균 디스크 사용률 (%)
    private Double avgUsagePercent;

    // 평균 inode 사용률 (%)
    private Double avgInodeUsagePercent;

    // 평균 읽기 속도 (bytes/sec)
    private Double avgReadBps;

    // 평균 쓰기 속도 (bytes/sec)
    private Double avgWriteBps;

    // 평균 I/O 사용률 (%)
    private Double avgIoTimePercent;

    // 집계 샘플 개수
    private Integer sampleCount;
}
