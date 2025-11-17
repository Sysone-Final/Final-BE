package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_network_realtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusNetworkRealtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(name = "instance_id")
    private Integer instanceId;

    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "rx_bytes_total")
    private Long rxBytesTotal;

    @Column(name = "tx_bytes_total")
    private Long txBytesTotal;

    @Column(name = "rx_packets_total")
    private Long rxPacketsTotal;

    @Column(name = "tx_packets_total")
    private Long txPacketsTotal;

    @Column(name = "rx_errors_total")
    private Long rxErrorsTotal;

    @Column(name = "tx_errors_total")
    private Long txErrorsTotal;

    @Column(name = "rx_dropped_total")
    private Long rxDroppedTotal;

    @Column(name = "tx_dropped_total")
    private Long txDroppedTotal;

    @Column(name = "rx_bps")
    private Double rxBps;

    @Column(name = "tx_bps")
    private Double txBps;
}