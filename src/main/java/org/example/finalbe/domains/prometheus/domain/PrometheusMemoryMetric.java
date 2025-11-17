package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_memory_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusMemoryMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(nullable = false)
    private String instance;

    // 총 메모리 및 사용률
    private Long totalBytes;
    private Long usedBytes;
    private Long freeBytes;
    private Long availableBytes;
    private Double usagePercent;

    // 메모리 구성
    private Long buffersBytes;
    private Long cachedBytes;
    private Long activeBytes;
    private Long inactiveBytes;

    // 스왑 메모리
    private Long swapTotalBytes;
    private Long swapUsedBytes;
    private Long swapFreeBytes;
    private Double swapUsagePercent;

    private Instant createdAt;
}