package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 메트릭 차트 데이터 DTO
 * 그래프 렌더링용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricChartData {
    private LocalDateTime time;
    private Double avgCpuUser;
    private Double avgCpuSystem;
    private Double avgMemoryUsage;
    private Double avgLoad;

    /**
     * Native Query 결과를 위한 생성자
     * Timestamp를 LocalDateTime으로 자동 변환
     *
     * @param timestamp SQL Timestamp
     * @param avgCpuUser 평균 CPU User 사용률
     * @param avgCpuSystem 평균 CPU System 사용률
     * @param avgMemoryUsage 평균 메모리 사용률
     * @param avgLoad 평균 Load Average
     */
    public MetricChartData(Timestamp timestamp, Double avgCpuUser, Double avgCpuSystem,
                           Double avgMemoryUsage, Double avgLoad) {
        this.time = timestamp != null ? timestamp.toLocalDateTime() : null;
        this.avgCpuUser = avgCpuUser;
        this.avgCpuSystem = avgCpuSystem;
        this.avgMemoryUsage = avgMemoryUsage;
        this.avgLoad = avgLoad;
    }
}