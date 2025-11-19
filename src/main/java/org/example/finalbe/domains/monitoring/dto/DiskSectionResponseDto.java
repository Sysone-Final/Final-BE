package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 디스크 섹션 전체 응답 DTO
 * 디스크 대시보드 섹션의 모든 그래프 데이터 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskSectionResponseDto {

    /**
     * 현재 디스크 상태 (게이지 및 요약 정보)
     */
    private DiskCurrentStatsDto currentStats;

    /**
     * 그래프 4.1: 디스크 사용률 추이
     */
    private List<DiskUsagePointDto> diskUsageTrend;

    /**
     * 그래프 4.2: 디스크 I/O (읽기/쓰기 속도)
     */
    private List<DiskIoPointDto> diskIoTrend;

    /**
     * 그래프 4.6: Inode 사용률 추이
     */
    private List<DiskInodeUsagePointDto> inodeUsageTrend;

    // 참고: 그래프 4.3(I/O 사용률), 4.4(I/O 횟수), 4.5(용량) 등은
    // 필요에 따라 DTO를 추가하거나, 기존 DTO에 필드를 추가하여 확장할 수 있습니다.
    // 여기서는 3개의 핵심 그래프만 구현합니다.
}