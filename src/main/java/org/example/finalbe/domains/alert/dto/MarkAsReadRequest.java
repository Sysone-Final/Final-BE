/**
 * 작성자: 황요한
 * 선택한 알림들을 읽음 처리하기 위한 요청 DTO
 */
package org.example.finalbe.domains.alert.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MarkAsReadRequest(
        @NotEmpty(message = "알림 ID 목록은 필수입니다.")
        List<Long> alertIds
) {}
