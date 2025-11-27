// 작성자: 황요한
// 여러 장비의 CPU 현재 상태를 일괄 조회한 결과 DTO

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
public class CpuCurrentStatsBatchDto {

    // 조회 성공한 장비 수
    private Integer successCount;

    // 조회 실패한 장비 수
    private Integer failureCount;

    // 장비별 CPU 상태 목록
    private List<CpuStatsWithEquipmentDto> equipmentStats;
}
