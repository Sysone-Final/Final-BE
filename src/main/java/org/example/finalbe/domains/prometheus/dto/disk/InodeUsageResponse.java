package org.example.finalbe.domains.prometheus.dto.disk;

/**
 * Inode 사용률 응답 DTO
 * 그래프 4.6: Inode 사용률
 */
public record InodeUsageResponse(
        Integer deviceId,
        Integer mountpointId,
        Double totalInodes,
        Double freeInodes,
        Double usedInodes,
        Double inodeUsagePercent
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer deviceId;
        private Integer mountpointId;
        private Double totalInodes;
        private Double freeInodes;
        private Double usedInodes;
        private Double inodeUsagePercent;

        public Builder deviceId(Integer deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder mountpointId(Integer mountpointId) {
            this.mountpointId = mountpointId;
            return this;
        }

        public Builder totalInodes(Double totalInodes) {
            this.totalInodes = totalInodes;
            return this;
        }

        public Builder freeInodes(Double freeInodes) {
            this.freeInodes = freeInodes;
            return this;
        }

        public Builder usedInodes(Double usedInodes) {
            this.usedInodes = usedInodes;
            return this;
        }

        public Builder inodeUsagePercent(Double inodeUsagePercent) {
            this.inodeUsagePercent = inodeUsagePercent;
            return this;
        }

        public InodeUsageResponse build() {
            return new InodeUsageResponse(
                    deviceId,
                    mountpointId,
                    totalInodes,
                    freeInodes,
                    usedInodes,
                    inodeUsagePercent
            );
        }
    }
}