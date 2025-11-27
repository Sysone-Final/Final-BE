// 작성자: 황요한
// 설명: 장치 위치 변경 요청 DTO. 장치의 위치(gridY, gridX, gridZ)와 회전(rotation)을 변경할 때 사용.

package org.example.finalbe.domains.device.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record DevicePositionUpdateRequest(

        @NotNull(message = "행 위치를 입력해주세요.")
        @Min(value = 0, message = "행 위치는 0 이상이어야 합니다.")
        Integer gridY,

        @NotNull(message = "열 위치를 입력해주세요.")
        @Min(value = 0, message = "열 위치는 0 이상이어야 합니다.")
        Integer gridX,

        @Min(value = 0, message = "Z축 위치는 0 이상이어야 합니다.")
        Integer gridZ,

        @Min(value = 0, message = "회전 각도는 0 이상이어야 합니다.")
        @Max(value = 360, message = "회전 각도는 360 이하여야 합니다.")
        Integer rotation
) {

    // rotation 값이 null일 경우 기본값 0을 반환
    public int safeRotation() {
        return rotation == null ? 0 : rotation;
    }

    // gridZ 값이 null일 경우 기본값 0을 반환
    public int safeGridZ() {
        return gridZ == null ? 0 : gridZ;
    }
}
