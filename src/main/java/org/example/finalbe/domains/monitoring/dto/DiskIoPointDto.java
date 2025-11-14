package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 디스크 I/O 포인트 DTO
 * 그래프 4.2: 디스크 I/O (읽기/쓰기 속도) (라인 차트)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskIoPointDto {

    private LocalDateTime timestamp;

    /**
     * 읽기 속도 (bytes/sec)
     */
    private Double readBps;

    /**
     * 쓰기 속도 (bytes/sec)
     */
    private Double writeBps;
}