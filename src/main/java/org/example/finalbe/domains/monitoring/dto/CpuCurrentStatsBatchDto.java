package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 장비의 CPU 상태 일괄 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuCurrentStatsBatchDto {

    /**
     * 조회 성공한 장비 수
     */
    private Integer successCount;

    /**
     * 조회 실패한 장비 수
     */
    private Integer failureCount;

    /**
     * 각 장비별 CPU 상태 리스트
     */
    private List<CpuStatsWithEquipmentDto> equipmentStats;
}