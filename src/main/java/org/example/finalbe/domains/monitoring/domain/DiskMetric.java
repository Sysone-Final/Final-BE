package org.example.finalbe.domains.monitoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "disk_metrics", indexes = {
        @Index(name = "idx_device_partition_time", columnList = "deviceId,partitionPath,generateTime"),
        @Index(name = "idx_disk_generate_time", columnList = "generateTime")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer deviceId;

    @Column(nullable = false, length = 100)
    private String partitionPath;

    @Column(nullable = false)
    private LocalDateTime generateTime;

    // ==================== 디스크 용량 (그래프 4.1, 4.5) ====================≥
    private Long totalBytes;          // 총 디스크 용량
    private Long usedBytes;           // 사용 중인 디스크
    private Long freeBytes;           // 여유 디스크 공간
    private Double usedPercentage;    // 디스크 사용률

    // ==================== 디스크 I/O (그래프 4.2, 4.3, 4.4) ====================
    private Double ioReadBps;         // 디스크 읽기 속도 (bytes/sec)
    private Double ioWriteBps;        // 디스크 쓰기 속도 (bytes/sec)
    private Double ioTimePercentage;  // I/O 사용률 (%)
    private Long ioReadCount;         // 읽기 작업 횟수
    private Long ioWriteCount;        // 쓰기 작업 횟수

    // ==================== inode (그래프 4.6) ====================
    private Long totalInodes;         // 총 inode 수
    private Long usedInodes;          // 사용 중인 inode
    private Long freeInodes;          // 여유 inode
    private Double usedInodePercentage;  // inode 사용률




}