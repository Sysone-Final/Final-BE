package org.example.finalbe.domains.monitoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_metrics", indexes = {
        @Index(name = "idx_device_nic_time", columnList = "deviceId,nicName,generateTime"),
        @Index(name = "idx_network_generate_time", columnList = "generateTime")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer deviceId;

    @Column(nullable = false, length = 100)
    private String nicName;

    @Column(nullable = false)
    private LocalDateTime generateTime;

    // ==================== 사용률 (그래프 3.1, 3.2) ====================
    private Double rxUsage;           // 수신(RX) 사용률 (%)
    private Double txUsage;           // 송신(TX) 사용률 (%)

    // ==================== 패킷 누적 (그래프 3.3, 3.4) ====================
    private Long inPktsTot;           // 총 수신 패킷 수 (누적)
    private Long outPktsTot;          // 총 송신 패킷 수 (누적)

    // ==================== 바이트 누적 (그래프 3.5, 3.6) ====================
    private Long inBytesTot;          // 총 수신 바이트 (누적)
    private Long outBytesTot;         // 총 송신 바이트 (누적)

    // ==================== 초당 전송량 (그래프 3.7) ====================
    private Double inBytesPerSec;     // 초당 수신 바이트
    private Double outBytesPerSec;    // 초당 송신 바이트
    private Double inPktsPerSec;      // 초당 수신 패킷
    private Double outPktsPerSec;     // 초당 송신 패킷

    // ==================== 에러/드롭 (그래프 3.8) ====================
    private Long inErrorPktsTot;      // 총 수신 에러 패킷
    private Long outErrorPktsTot;     // 총 송신 에러 패킷
    private Long inDiscardPktsTot;    // 총 수신 드롭 패킷
    private Long outDiscardPktsTot;   // 총 송신 드롭 패킷

    // ==================== 인터페이스 상태 (그래프 3.9) ====================
    private Integer operStatus;       // 작동 상태 (1=UP, 0=DOWN)
}