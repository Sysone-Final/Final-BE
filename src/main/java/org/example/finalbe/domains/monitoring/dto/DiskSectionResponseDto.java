// 작성자: 최산하
// 디스크 대시보드 섹션 전체 응답 DTO

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskSectionResponseDto {

    // 현재 디스크 상태
    private DiskCurrentStatsDto currentStats;

    // 디스크 사용률 추이
    private List<DiskUsagePointDto> diskUsageTrend;

    // 디스크 I/O 속도 추이
    private List<DiskIoPointDto> diskIoTrend;

    // inode 사용률 추이
    private List<DiskInodeUsagePointDto> inodeUsageTrend;
}
