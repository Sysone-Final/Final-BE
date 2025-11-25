package org.example.finalbe.domains.alert.dto;

import org.example.finalbe.domains.alert.domain.AlertSettings;

import java.time.LocalDateTime;

public record AlertSettingsDto(
        Long settingId,

        // 기본 설정
        Integer defaultConsecutiveCount,
        Integer defaultCooldownMinutes,

        // 네트워크 임계치
        Double networkErrorRateWarning,
        Double networkErrorRateCritical,
        Double networkDropRateWarning,
        Double networkDropRateCritical,

        // 시간 정보
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Entity → DTO 변환
     */
    public static AlertSettingsDto from(AlertSettings entity) {
        if (entity == null) {
            return null;
        }

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

    /**
     * DTO → Entity 변환 (생성용)
     */
    public AlertSettings toEntity() {
        return AlertSettings.builder()
                .defaultConsecutiveCount(this.defaultConsecutiveCount)
                .defaultCooldownMinutes(this.defaultCooldownMinutes)
                .networkErrorRateWarning(this.networkErrorRateWarning)
                .networkErrorRateCritical(this.networkErrorRateCritical)
                .networkDropRateWarning(this.networkDropRateWarning)
                .networkDropRateCritical(this.networkDropRateCritical)
                .build();
    }

    /**
     * 기본 설정값 생성
     */
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