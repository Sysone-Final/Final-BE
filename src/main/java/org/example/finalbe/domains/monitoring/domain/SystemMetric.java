// 작성자: 황요한
// 시스템 메트릭 엔티티

package org.example.finalbe.domains.monitoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_metrics", indexes = {
        @Index(name = "idx_system_equipment_time", columnList = "equipment_id, generate_time"),
        @Index(name = "idx_system_generate_time", columnList = "generate_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long equipmentId;

    @Column(nullable = false)
    private LocalDateTime generateTime;

    private Double cpuIdle;
    private Double cpuUser;
    private Double cpuSystem;
    private Double cpuWait;
    private Double cpuNice;
    private Double cpuIrq;
    private Double cpuSoftirq;
    private Double cpuSteal;

    private Double loadAvg1;
    private Double loadAvg5;
    private Double loadAvg15;

    private Long contextSwitches;

    private Long totalMemory;
    private Long usedMemory;
    private Long freeMemory;
    private Double usedMemoryPercentage;
    private Long memoryBuffers;
    private Long memoryCached;
    private Long memoryActive;
    private Long memoryInactive;

    private Long totalSwap;
    private Long usedSwap;
    private Double usedSwapPercentage;
}
