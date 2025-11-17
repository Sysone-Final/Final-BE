package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_temperature_realtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusTemperatureRealtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(name = "instance_id")
    private Integer instanceId;

    @Column(name = "chip_id")
    private Integer chipId;

    @Column(name = "sensor_id")
    private Integer sensorId;

    @Column(name = "celsius")
    private Double celsius;
}