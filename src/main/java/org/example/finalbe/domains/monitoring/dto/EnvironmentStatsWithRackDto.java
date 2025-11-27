// 작성자: 최산하
// 랙 환경 상태 + 랙 정보 DTO

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentStatsWithRackDto {

    // 랙 ID
    private Long rackId;

    // 랙 이름
    private String rackName;

    // 조회 성공 여부
    private Boolean success;

    // 실패 시 메시지
    private String errorMessage;

    // 환경 상태 정보
    private EnvironmentCurrentStatsDto environmentStats;
}
