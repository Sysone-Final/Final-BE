/**
 * 작성자: 황요한
 * 알림 설정 정보 전달을 위한 DTO
 */
package org.example.finalbe.domains.alert.dto;

import org.example.finalbe.domains.alert.domain.AlertSettings;

import java.time.LocalDateTime;

public record AlertSettingsDto(
        Long settingId,

        Integer defaultConsecutiveCount,
        Integer defaultCooldownMinutes,

        Double networkErrorRateWarning,
        Double networkErrorRateCritical,
        Double networkDropRateWarning,
        Double networkDropRateCritical,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // Entity → DTO 변환
    public static AlertSettingsDto from(AlertSettings entity) {
        if (entity == null) return null;

        return new AlertSettingsDto(
                entity.getId(),
                entity.getDefaultConsecutiveCount(),
                entity.getDefaultCooldownMinutes(),
                entity.getNetworkErrorRateWarning(),
                entity.getNetworkErrorRateCritical(),
                entity.getNetworkDropRateWarning(),
                entity.getNetworkDropRateCritical(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // DTO → Entity 변환 (생성용)
    public AlertSettings toEntity() {
        return AlertSettings.builder()
                .defaultConsecutiveCount(defaultConsecutiveCount)
                .defaultCooldownMinutes(defaultCooldownMinutes)
                .networkErrorRateWarning(networkErrorRateWarning)
                .networkErrorRateCritical(networkErrorRateCritical)
                .networkDropRateWarning(networkDropRateWarning)
                .networkDropRateCritical(networkDropRateCritical)
                .build();
    }

    // 기본값 DTO 생성
    public static AlertSettingsDto getDefault() {
        return new AlertSettingsDto(
                null,
                3,
                10,
                0.1,
                1.0,
                0.1,
                1.0,
                null,
                null
        );
    }
}
