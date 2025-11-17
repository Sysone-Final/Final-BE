package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_disk_realtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusDiskRealtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(name = "instance_id")
    private Integer instanceId;

    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "mountpoint_id")
    private Integer mountpointId;

    @Column(name = "total_bytes")
    private Long totalBytes;

    @Column(name = "free_bytes")
    private Long freeBytes;

    @Column(name = "usage_percent")
    private Double usagePercent;

    @Column(name = "read_bytes_per_sec")
    private Double readBytesPerSec;

    @Column(name = "write_bytes_per_sec")
    private Double writeBytesPerSec;

    @Column(name = "read_iops")
    private Double readIops;

    @Column(name = "write_iops")
    private Double writeIops;

    @Column(name = "io_utilization_percent")
    private Double ioUtilizationPercent;

    @Column(name = "total_inodes")
    private Long totalInodes;

    @Column(name = "free_inodes")
    private Long freeInodes;

    @Column(name = "inode_usage_percent")
    private Double inodeUsagePercent;
}