package org.example.finalbe.domains.prometheus.dto.disk;

import java.time.ZonedDateTime;

/**
 * 디스크 I/O 응답 DTO
 * 그래프 4.2, 4.3, 4.4: 디스크 I/O (읽기/쓰기 속도, IOPS, 사용률)
 */
public record DiskIoResponse(
        ZonedDateTime time,
        Double readBytesPerSec,
        Double writeBytesPerSec,
        Double readIops,
        Double writeIops,
        Double ioUtilizationPercent
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime time;
        private Double readBytesPerSec;
        private Double writeBytesPerSec;
        private Double readIops;
        private Double writeIops;
        private Double ioUtilizationPercent;

        public Builder time(ZonedDateTime time) {
            this.time = time;
            return this;
        }

        public Builder readBytesPerSec(Double readBytesPerSec) {
            this.readBytesPerSec = readBytesPerSec;
            return this;
        }

        public Builder writeBytesPerSec(Double writeBytesPerSec) {
            this.writeBytesPerSec = writeBytesPerSec;
            return this;
        }

        public Builder readIops(Double readIops) {
            this.readIops = readIops;
            return this;
        }

        public Builder writeIops(Double writeIops) {
            this.writeIops = writeIops;
            return this;
        }

        public Builder ioUtilizationPercent(Double ioUtilizationPercent) {
            this.ioUtilizationPercent = ioUtilizationPercent;
            return this;
        }

        public DiskIoResponse build() {
            return new DiskIoResponse(
                    time,
                    readBytesPerSec,
                    writeBytesPerSec,
                    readIops,
                    writeIops,
                    ioUtilizationPercent
            );
        }
    }
}