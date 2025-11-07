package org.example.finalbe.domains.equipment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 장비 대량 상태 변경 요청 DTO
 */
public record EquipmentStatusBulkUpdateRequest(
        @NotEmpty(message = "장비 ID 목록이 비어있습니다.")
        List<Long> ids,

        @NotNull(message = "변경할 상태를 입력해주세요.")
        String status
) {
}