package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.RackStatus;

/**
 * 랙 상태 변경 요청 DTO
 */
@Builder
public record RackStatusChangeRequest(
        RackStatus status,
        String reason
) {
}