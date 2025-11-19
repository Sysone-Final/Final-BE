package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_disk_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusDiskMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(nullable = false)
    private String instance;

    @Column(nullable = false)
    private String device;

    @Column(nullable = false, length = 512)
    private String mountpoint;

    // 디스크 용량
    private Long totalBytes;
    private Long usedBytes;
    private Long freeBytes;
    private Double usagePercent;

    // I/O 속도
    private Double readBytesPerSec;
    private Double writeBytesPerSec;
    private Double totalIoBytesPerSec;

    // IOPS
    private Double readIops;
    private Double writeIops;

    // I/O 사용률
    private Double ioUtilizationPercent;
    private Double readTimePercent;
    private Double writeTimePercent;

    // inode
    private Long totalInodes;
    private Long usedInodes;
    private Long freeInodes;
    private Double inodeUsagePercent;

    private Instant createdAt;
}