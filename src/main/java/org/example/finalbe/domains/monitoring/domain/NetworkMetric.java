// 작성자: 황요한
// 네트워크 메트릭 엔티티

package org.example.finalbe.domains.monitoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_metrics", indexes = {
        @Index(name = "idx_network_equipment_nic_time", columnList = "equipment_id, nic_name, generate_time"),
        @Index(name = "idx_network_generate_time", columnList = "generate_time")
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
    private Long equipmentId;

    @Column(nullable = false, length = 100)
    private String nicName;

    @Column(nullable = false)
    private LocalDateTime generateTime;

    private Double rxUsage;
    private Double txUsage;

    private Long inPktsTot;
    private Long outPktsTot;

    private Long inBytesTot;
    private Long outBytesTot;

    private Double inBytesPerSec;
    private Double outBytesPerSec;
    private Double inPktsPerSec;
    private Double outPktsPerSec;

    private Long inErrorPktsTot;
    private Long outErrorPktsTot;
    private Long inDiscardPktsTot;
    private Long outDiscardPktsTot;

    private Integer operStatus;
}
