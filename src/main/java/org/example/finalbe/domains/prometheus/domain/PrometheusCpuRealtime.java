package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_cpu_realtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusCpuRealtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(name = "instance_id")
    private Integer instanceId;

    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;

    @Column(name = "load_avg_1")
    private Double loadAvg1;

    @Column(name = "load_avg_5")
    private Double loadAvg5;

    @Column(name = "load_avg_15")
    private Double loadAvg15;

    @Column(name = "context_switches")
    private Long contextSwitches;
}