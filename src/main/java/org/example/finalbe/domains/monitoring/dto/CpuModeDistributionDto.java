// 작성자: 황요한
// CPU 모드별 사용률 데이터를 담는 DTO

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
public class CpuModeDistributionDto {

    // 측정 시각
    private LocalDateTime timestamp;

    // 사용자 모드 CPU 사용률
    private Double userPercent;

    // 시스템 모드 CPU 사용률
    private Double systemPercent;

    // I/O 대기 시간
    private Double iowaitPercent;

    // 하드웨어 인터럽트 시간
    private Double irqPercent;

    // 소프트웨어 인터럽트 시간
    private Double softirqPercent;

    // Nice 프로세스 CPU 시간
    private Double nicePercent;

    // Steal 시간 (가상화)
    private Double stealPercent;

    // 유휴 시간
    private Double idlePercent;
}
