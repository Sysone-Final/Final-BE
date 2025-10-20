package org.example.finalbe.domains.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * 장비 상태 변경 요청 DTO
 * Bean Validation 추가하여 입력값 검증
 */
@Builder
public record EquipmentStatusChangeRequest(
        @NotBlank(message = "상태를 입력해주세요.")
        String status,

        String reason  // nullable - 선택사항
) {
    /**
     * 입력값 검증 메서드
     * 추가적인 비즈니스 로직 검증이 필요한 경우 사용
     */
    public void validate() {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("상태를 입력해주세요.");
        }

        // status가 유효한 enum 값인지 검증
        try {
            org.example.finalbe.domains.common.enumdir.EquipmentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 상태값입니다: " + status);
        }
    }
}