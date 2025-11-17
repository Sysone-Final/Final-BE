package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_temperature_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusTemperatureMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(nullable = false)
    private String instance;

    @Column(length = 100)
    private String chip;

    @Column(length = 100)
    private String sensor;

    @Column(nullable = false)
    private Double tempCelsius;

    private Instant createdAt;
}