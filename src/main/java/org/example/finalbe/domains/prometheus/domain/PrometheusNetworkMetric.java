package org.example.finalbe.domains.prometheus.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "prometheus_network_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrometheusNetworkMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant time;

    @Column(nullable = false)
    private String instance;

    @Column(nullable = false)
    private String device;

    // 사용률
    private Double rxUsagePercent;
    private Double txUsagePercent;
    private Double totalUsagePercent;

    // 패킷 수 (누적)
    private Long rxPacketsTotal;
    private Long txPacketsTotal;

    // 바이트 수 (누적)
    private Long rxBytesTotal;
    private Long txBytesTotal;

    // 초당 전송률
    private Double rxBytesPerSec;
    private Double txBytesPerSec;
    private Double rxPacketsPerSec;
    private Double txPacketsPerSec;

    // 에러 및 드롭
    private Long rxErrorsTotal;
    private Long txErrorsTotal;
    private Long rxDroppedTotal;
    private Long txDroppedTotal;

    // 인터페이스 상태
    private Boolean interfaceUp;

    private Instant createdAt;
}