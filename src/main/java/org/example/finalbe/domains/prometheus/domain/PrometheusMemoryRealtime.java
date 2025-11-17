package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_memory_realtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusMemoryRealtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(name = "instance_id")
    private Integer instanceId;

    @Column(name = "total_bytes")
    private Long totalBytes;

    @Column(name = "available_bytes")
    private Long availableBytes;

    @Column(name = "usage_percent")
    private Double usagePercent;

    @Column(name = "active_bytes")
    private Long activeBytes;

    @Column(name = "inactive_bytes")
    private Long inactiveBytes;

    @Column(name = "buffers_bytes")
    private Long buffersBytes;

    @Column(name = "cached_bytes")
    private Long cachedBytes;

    @Column(name = "free_bytes")
    private Long freeBytes;

    @Column(name = "swap_total_bytes")
    private Long swapTotalBytes;

    @Column(name = "swap_free_bytes")
    private Long swapFreeBytes;

    @Column(name = "swap_usage_percent")
    private Double swapUsagePercent;
}