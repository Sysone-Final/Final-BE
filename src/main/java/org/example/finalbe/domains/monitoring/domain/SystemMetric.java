package org.example.finalbe.domains.monitoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "system_metrics", indexes = {
        @Index(name = "idx_device_time", columnList = "deviceId,generateTime"),
        @Index(name = "idx_system_generate_time", columnList = "generateTime")
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
    private Integer deviceId;

    @Column(nullable = false)
    private LocalDateTime generateTime;

    // ==================== CPU 관련 (그래프 1.1, 1.2) ====================
    private Double cpuIdle;           // CPU 유휴 시간
    private Double cpuUser;           // 사용자 모드 CPU
    private Double cpuSystem;         // 시스템 모드 CPU
    private Double cpuWait;           // I/O 대기 시간
    private Double cpuNice;           // Nice 프로세스 CPU
    private Double cpuIrq;            // 하드웨어 인터럽트
    private Double cpuSoftirq;        // 소프트웨어 인터럽트
    private Double cpuSteal;          // Steal 시간 (가상화)

    // ==================== 시스템 부하 (그래프 1.3) ====================
    private Double loadAvg1;          // 1분 평균 부하
    private Double loadAvg5;          // 5분 평균 부하
    private Double loadAvg15;         // 15분 평균 부하

    // ==================== 컨텍스트 스위치 (그래프 1.4) ====================
    private Long contextSwitches;     // 컨텍스트 스위치 횟수

    // ==================== 메모리 관련 (그래프 2.1, 2.2) ====================
    private Long totalMemory;         // 총 메모리
    private Long usedMemory;          // 사용 중인 메모리
    private Long freeMemory;          // 여유 메모리
    private Double usedMemoryPercentage;  // 메모리 사용률
    private Long memoryBuffers;       // 버퍼 메모리
    private Long memoryCached;        // 캐시 메모리
    private Long memoryActive;        // 활성 메모리
    private Long memoryInactive;      // 비활성 메모리

    // ==================== SWAP 관련 (그래프 2.3) ====================
    private Long totalSwap;           // 총 스왑 메모리
    private Long usedSwap;            // 사용 중인 스왑
    private Double usedSwapPercentage;  // 스왑 사용률
}