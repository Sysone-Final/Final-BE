package org.example.finalbe.domains.alert.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 여러 알림 일괄 해결 요청 DTO
 */
public record ResolveMultipleRequest(
        @NotEmpty(message = "알림 ID 목록은 비어있을 수 없습니다.")
        List<Long> alertIds
) {
}