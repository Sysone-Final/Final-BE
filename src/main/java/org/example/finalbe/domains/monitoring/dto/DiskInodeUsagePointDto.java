package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Inode 사용률 포인트 DTO
 * 그래프 4.6: Inode 사용률 (라인 차트)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskInodeUsagePointDto {

    private LocalDateTime timestamp;

    /**
     * Inode 사용률 (%)
     */
    private Double inodeUsagePercent;
}