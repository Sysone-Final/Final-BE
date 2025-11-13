package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여러 랙의 환경 상태 일괄 조회 응답 DTO     rackId 기준
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentCurrentStatsBatchDto {

    /**
     * 조회 성공한 랙 수
     */
    private Integer successCount;

    /**
     * 조회 실패한 랙 수
     */
    private Integer failureCount;

    /**
     * 각 랙별 환경 상태 리스트
     */
    private List<EnvironmentStatsWithRackDto> rackStats;
}