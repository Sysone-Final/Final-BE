// 작성자: 황요한
// 장비 정보가 포함된 CPU 상태 DTO

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuStatsWithEquipmentDto {

    // 장비 ID
    private Long equipmentId;

    // 장비 이름
    private String equipmentName;

    // 조회 성공 여부
    private Boolean success;

    // 실패 시 에러 메시지
    private String errorMessage;

    // CPU 상태 정보
    private CpuCurrentStatsDto cpuStats;
}
