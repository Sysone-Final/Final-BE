package org.example.finalbe.domains.prometheus.domain.disk;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "node_filesystem_size_bytes", schema = "prom_metric")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeFilesystemSizeBytes {

    @Id
    @Column(name = "series_id")
    private Long seriesId;

    @Column(name = "time")
    private Instant time;

    @Column(name = "value")
    private Double value;

    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "fstype_id")
    private Integer fstypeId;

    @Column(name = "instance_id")
    private Integer instanceId;

    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "mountpoint_id")
    private Integer mountpointId;
}