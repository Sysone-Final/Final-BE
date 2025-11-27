/**
 * 작성자: 황요한
 * 장비의 네트워크 상태와 조회 결과를 포함하는 DTO
 */
package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkStatsWithEquipmentDto {

    private Long equipmentId;          // 장비 ID
    private String equipmentName;      // 장비명(선택)
    private Boolean success;           // 조회 성공 여부
    private String errorMessage;       // 실패 시 에러 메시지
    private NetworkCurrentStatsDto networkStats; // 네트워크 상태 정보
}
