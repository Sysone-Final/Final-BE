package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 랙 정보가 포함된 환경 상태 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentStatsWithRackDto {

    /**
     * 랙 ID
     */
    private Long rackId;

    /**
     * 랙 이름 (선택적)
     */
    private String rackName;

    /**
     * 조회 성공 여부
     */
    private Boolean success;

    /**
     * 실패 시 에러 메시지
     */
    private String errorMessage;

    /**
     * 환경 상태 정보
     */
    private EnvironmentCurrentStatsDto environmentStats;
}