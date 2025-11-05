package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 장비 정보가 포함된 CPU 상태 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuStatsWithEquipmentDto {

    /**
     * 장비 ID
     */
    private Long equipmentId;

    /**
     * 장비명 (선택적)
     */
    private String equipmentName;

    /**
     * 조회 성공 여부
     */
    private Boolean success;

    /**
     * 실패 시 에러 메시지
     */
    private String errorMessage;

    /**
     * CPU 상태 정보
     */
    private CpuCurrentStatsDto cpuStats;
}