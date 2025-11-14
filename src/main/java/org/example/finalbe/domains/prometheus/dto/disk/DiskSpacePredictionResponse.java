package org.example.finalbe.domains.prometheus.dto.disk;

import java.time.ZonedDateTime;

/**
 * 디스크 공간 예측 응답 DTO
 * 그래프 4.5: 디스크 공간 추이 with 예측
 */
public record DiskSpacePredictionResponse(
        ZonedDateTime time,
        Double freeBytes,
        Double usedBytes,
        Double usagePercent,
        Boolean isPrediction,
        Double predictedUsagePercent
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime time;
        private Double freeBytes;
        private Double usedBytes;
        private Double usagePercent;
        private Boolean isPrediction;
        private Double predictedUsagePercent;

        public Builder time(ZonedDateTime time) {
            this.time = time;
            return this;
        }

        public Builder freeBytes(Double freeBytes) {
            this.freeBytes = freeBytes;
            return this;
        }

        public Builder usedBytes(Double usedBytes) {
            this.usedBytes = usedBytes;
            return this;
        }

        public Builder usagePercent(Double usagePercent) {
            this.usagePercent = usagePercent;
            return this;
        }

        public Builder isPrediction(Boolean isPrediction) {
            this.isPrediction = isPrediction;
            return this;
        }

        public Builder predictedUsagePercent(Double predictedUsagePercent) {
            this.predictedUsagePercent = predictedUsagePercent;
            return this;
        }

        public DiskSpacePredictionResponse build() {
            return new DiskSpacePredictionResponse(
                    time,
                    freeBytes,
                    usedBytes,
                    usagePercent,
                    isPrediction,
                    predictedUsagePercent
            );
        }
    }
}