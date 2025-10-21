package org.example.finalbe.domains.device.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record DevicePositionUpdateRequest(
        @NotNull(message = "행 위치를 입력해주세요.")
        @Min(value = 0, message = "행 위치는 0 이상이어야 합니다.")
        Integer positionRow,

        @NotNull(message = "열 위치를 입력해주세요.")
        @Min(value = 0, message = "열 위치는 0 이상이어야 합니다.")
        Integer positionCol,

        @Min(value = 0, message = "Z축 위치는 0 이상이어야 합니다.")
        Integer positionZ,

        @Min(value = 0, message = "회전 각도는 0 이상이어야 합니다.")
        @Max(value = 360, message = "회전 각도는 360 이하여야 합니다.")
        Integer rotation
) {
}