package org.example.finalbe.domains.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * 장비 상태 변경 요청 DTO
 */
@Builder
public record EquipmentStatusChangeRequest(
        @NotBlank(message = "상태를 입력해주세요.")
        String status,

        String reason
) {
}