/**
 * 작성자: 황요한
 * 장치 수정 요청 DTO
 */
package org.example.finalbe.domains.device.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DeviceUpdateRequest(
        @Size(max = 100, message = "장치명은 100자를 초과할 수 없습니다.")
        String deviceName,

        @Min(value = 0, message = "행 위치는 0 이상이어야 합니다.")
        Integer gridY,

        @Min(value = 0, message = "열 위치는 0 이상이어야 합니다.")
        Integer gridX,

        @Min(value = 0, message = "Z축 위치는 0 이상이어야 합니다.")
        Integer gridZ,

        @Min(value = 0, message = "회전 각도는 0 이상이어야 합니다.")
        @Max(value = 360, message = "회전 각도는 360 이하여야 합니다.")
        Integer rotation,

        String status,

        @Size(max = 100, message = "모델명은 100자를 초과할 수 없습니다.")
        String modelName,

        @Size(max = 100, message = "제조사명은 100자를 초과할 수 없습니다.")
        String manufacturer,

        @Size(max = 100, message = "시리얼 번호는 100자를 초과할 수 없습니다.")
        String serialNumber,

        LocalDate purchaseDate,

        LocalDate warrantyEndDate,

        @Size(max = 1000, message = "비고는 1000자를 초과할 수 없습니다.")
        String notes,

        Long rackId
) {
}