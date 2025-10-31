package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.device.domain.DeviceType;

/**
 * 장치 타입 목록 조회 응답 DTO
 */
@Builder
public record DeviceTypeListResponse(
        Long id,
        String typeName,
        String category,
        String iconUrl,
        String description
) {
    public static DeviceTypeListResponse from(DeviceType deviceType) {
        return DeviceTypeListResponse.builder()
                .id(deviceType.getId())
                .typeName(deviceType.getTypeName())
                .category(deviceType.getCategory() != null ? deviceType.getCategory().name() : null)
                .iconUrl(deviceType.getIconUrl())
                .description(deviceType.getDescription())
                .build();
    }
}