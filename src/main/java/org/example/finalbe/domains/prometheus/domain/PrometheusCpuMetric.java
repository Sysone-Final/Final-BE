package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_cpu_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusCpuMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(nullable = false)
    private String instance;

    // CPU 사용률
    private Double cpuUsagePercent;

    // CPU 모드별 사용률
    private Double userPercent;
    private Double systemPercent;
    private Double iowaitPercent;
    private Double idlePercent;
    private Double nicePercent;
    private Double irqPercent;
    private Double softirqPercent;
    private Double stealPercent;

    // 시스템 부하
    private Double loadAvg1;
    private Double loadAvg5;
    private Double loadAvg15;

    // 컨텍스트 스위치
    private Double contextSwitchesPerSec;

    private Instant createdAt;
}