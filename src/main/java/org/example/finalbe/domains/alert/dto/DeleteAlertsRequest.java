/**
 * 작성자: 황요한
 * 선택한 알림들을 삭제하기 위한 요청 DTO
 */
package org.example.finalbe.domains.alert.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DeleteAlertsRequest(
        @NotEmpty(message = "알림 ID 목록은 필수입니다.")
        List<Long> alertIds
) {}
