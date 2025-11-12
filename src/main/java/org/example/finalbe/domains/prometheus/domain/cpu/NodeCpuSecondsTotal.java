//package org.example.finalbe.domains.prometheus.domain.cpu;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.Instant;
//
//@Entity
//@Table(name = "node_cpu_seconds_total", schema = "prom_metric")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class NodeCpuSecondsTotal {
//
//    @Id
//    @Column(name = "series_id")
//    private Long seriesId;
//
//    @Column(name = "time")
//    private Instant time;
//
//    @Column(name = "value")
//    private Double value;
//
//    @Column(name = "cpu_id")
//    private Integer cpuId;
//
//    @Column(name = "instance_id")
//    private Integer instanceId;
//
//    @Column(name = "job_id")
//    private Integer jobId;
//
//    @Column(name = "mode_id")
//    private Integer modeId;
//}