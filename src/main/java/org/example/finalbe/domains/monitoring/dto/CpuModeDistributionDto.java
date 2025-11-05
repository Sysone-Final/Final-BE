package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CPU 모드별 분포 DTO
 * 그래프 1.2: CPU 사용 모드별 분포 (적층 영역 차트)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuModeDistributionDto {

    private LocalDateTime timestamp;

    /**
     * 사용자 모드 CPU 사용률 (%)
     */
    private Double userPercent;

    /**
     * 시스템 모드 CPU 사용률 (%)
     */
    private Double systemPercent;

    /**
     * I/O 대기 시간 (%)
     */
    private Double iowaitPercent;

    /**
     * 하드웨어 인터럽트 시간 (%)
     */
    private Double irqPercent;

    /**
     * 소프트웨어 인터럽트 시간 (%)
     */
    private Double softirqPercent;

    /**
     * Nice 프로세스 CPU 시간 (%)
     */
    private Double nicePercent;

    /**
     * Steal 시간 - 가상화 환경에서의 CPU 도용 (%)
     */
    private Double stealPercent;

    /**
     * 유휴 시간 (%)
     */
    private Double idlePercent;
}