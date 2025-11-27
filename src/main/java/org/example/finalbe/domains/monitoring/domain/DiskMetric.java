// 작성자: 황요한
// 디스크 메트릭 엔티티 (용량, I/O, inode 관련 실시간/통계 데이터 저장)

package org.example.finalbe.domains.monitoring.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "disk_metrics", indexes = {
        @Index(name = "idx_disk_equipment_time", columnList = "equipment_id, generate_time"),
        @Index(name = "idx_disk_generate_time", columnList = "generate_time")
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
    private Long equipmentId;

    @Column(nullable = false)
    private LocalDateTime generateTime;

    // 디스크 용량 정보
    private Long totalBytes;
    private Long usedBytes;
    private Long freeBytes;
    private Double usedPercentage;

    // 디스크 I/O 정보
    private Double ioReadBps;
    private Double ioWriteBps;
    private Double ioTimePercentage;
    private Long ioReadCount;
    private Long ioWriteCount;

    // inode 정보
    private Long totalInodes;
    private Long usedInodes;
    private Long freeInodes;
    private Double usedInodePercentage;
}
