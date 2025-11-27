// 작성자: 최산하
// 장비 정보를 포함한 메모리 상태 DTO

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryStatsWithEquipmentDto {

    // 장비 ID
    private Long equipmentId;

    // 장비명
    private String equipmentName;

    // 조회 성공 여부
    private Boolean success;

    // 실패 시 오류 메시지
    private String errorMessage;

    // 메모리 상태 정보
    private MemoryCurrentStatsDto memoryStats;
}
