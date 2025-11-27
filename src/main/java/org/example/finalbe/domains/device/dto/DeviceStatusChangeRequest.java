// 작성자: 황요한
// 설명: 장치 상태 변경 요청 DTO. 장치의 상태(status)와 변경 사유(reason)를 전달하는 역할을 함.

package org.example.finalbe.domains.device.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record DeviceStatusChangeRequest(

        @NotBlank(message = "상태를 입력해주세요.")
        String status,     // 변경할 장치 상태

        String reason      // 상태 변경 사유(선택 입력)
) {
}
