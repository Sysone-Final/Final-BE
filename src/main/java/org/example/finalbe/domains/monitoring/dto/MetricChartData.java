// 작성자: 황요한
// 메트릭 차트 데이터 DTO (그래프 렌더링용)

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricChartData {

    // 시간 값
    private LocalDateTime time;

    // 평균 CPU User 사용률
    private Double avgCpuUser;

    // 평균 CPU System 사용률
    private Double avgCpuSystem;

    // 평균 메모리 사용률
    private Double avgMemoryUsage;

    // 평균 Load Average
    private Double avgLoad;

    // Native Query 매핑용 생성자 (Timestamp → LocalDateTime 변환)
    public MetricChartData(Timestamp timestamp, Double avgCpuUser, Double avgCpuSystem,
                           Double avgMemoryUsage, Double avgLoad) {
        this.time = timestamp != null ? timestamp.toLocalDateTime() : null;
        this.avgCpuUser = avgCpuUser;
        this.avgCpuSystem = avgCpuSystem;
        this.avgMemoryUsage = avgMemoryUsage;
        this.avgLoad = avgLoad;
    }
}
