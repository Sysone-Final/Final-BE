/**
 * 작성자: 황요한
 * 랙 상태 변경 요청 DTO
 */
package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.RackStatus;

@Builder
public record RackStatusChangeRequest(
        RackStatus status,   // 변경할 상태
        String reason        // 변경 사유
) {
}
