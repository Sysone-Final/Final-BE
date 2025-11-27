/**
 * 작성자: 최산하
 * 여러 장비의 네트워크 상태 일괄 조회 응답 DTO
 */
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
public class NetworkCurrentStatsBatchDto {

    // 조회 성공한 장비 수
    private Integer successCount;

    // 조회 실패한 장비 수
    private Integer failureCount;

    // 각 장비별 네트워크 상태 목록
    private List<NetworkStatsWithEquipmentDto> equipmentStats;
}
