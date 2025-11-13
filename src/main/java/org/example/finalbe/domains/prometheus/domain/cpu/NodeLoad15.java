package org.example.finalbe.domains.prometheus.domain.cpu;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "node_load15", schema = "prom_metric")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeLoad15 {

    @Id
    @Column(name = "series_id")
    private Long seriesId;

    @Column(name = "time")
    private Instant time;

    @Column(name = "value")
    private Double value;

    @Column(name = "instance_id")
    private Integer instanceId;

    @Column(name = "job_id")
    private Integer jobId;
}