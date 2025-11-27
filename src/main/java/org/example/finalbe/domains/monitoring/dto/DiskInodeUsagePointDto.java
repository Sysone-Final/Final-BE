// 작성자: 최산하
// Inode 사용률 시계열 포인트 DTO

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskInodeUsagePointDto {

    // 데이터 발생 시각
    private LocalDateTime timestamp;

    // Inode 사용률 (%)
    private Double inodeUsagePercent;
}
