// 작성자: 최산하
// 여러 장비의 메모리 상태 일괄 조회 결과 DTO

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
public class MemoryCurrentStatsBatchDto {

    // 조회 성공한 장비 수
    private Integer successCount;

    // 조회 실패한 장비 수
    private Integer failureCount;

    // 장비별 메모리 상태 목록
    private List<MemoryStatsWithEquipmentDto> equipmentStats;
}
