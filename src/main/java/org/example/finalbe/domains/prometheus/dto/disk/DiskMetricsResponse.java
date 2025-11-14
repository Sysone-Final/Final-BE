package org.example.finalbe.domains.prometheus.dto.disk;

import java.util.List;

/**
 * 디스크 메트릭 전체 응답 DTO (완전 구현)
 */
public record DiskMetricsResponse(
        Double currentDiskUsagePercent,
        List<DiskUsageResponse> diskUsageTrend,
        List<DiskIoResponse> diskIoTrend,
        List<DiskSpacePredictionResponse> spacePredictionTrend,
        List<InodeUsageResponse> inodeUsage
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double currentDiskUsagePercent;
        private List<DiskUsageResponse> diskUsageTrend;
        private List<DiskIoResponse> diskIoTrend;
        private List<DiskSpacePredictionResponse> spacePredictionTrend;
        private List<InodeUsageResponse> inodeUsage;

        public Builder currentDiskUsagePercent(Double currentDiskUsagePercent) {
            this.currentDiskUsagePercent = currentDiskUsagePercent;
            return this;
        }

        public Builder diskUsageTrend(List<DiskUsageResponse> diskUsageTrend) {
            this.diskUsageTrend = diskUsageTrend;
            return this;
        }

        public Builder diskIoTrend(List<DiskIoResponse> diskIoTrend) {
            this.diskIoTrend = diskIoTrend;
            return this;
        }

        public Builder spacePredictionTrend(List<DiskSpacePredictionResponse> spacePredictionTrend) {
            this.spacePredictionTrend = spacePredictionTrend;
            return this;
        }

        public Builder inodeUsage(List<InodeUsageResponse> inodeUsage) {
            this.inodeUsage = inodeUsage;
            return this;
        }

        public DiskMetricsResponse build() {
            return new DiskMetricsResponse(
                    currentDiskUsagePercent,
                    diskUsageTrend,
                    diskIoTrend,
                    spacePredictionTrend,
                    inodeUsage
            );
        }
    }
}