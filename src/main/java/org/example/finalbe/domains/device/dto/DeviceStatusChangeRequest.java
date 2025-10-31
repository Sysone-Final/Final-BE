package org.example.finalbe.domains.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * 장치 상태 변경 요청 DTO
 */
@Builder
public record DeviceStatusChangeRequest(
        @NotBlank(message = "상태를 입력해주세요.")
        String status,

        String reason
) {
}