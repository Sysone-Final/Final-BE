package org.example.finalbe.domains.prometheus.dto.network;

import java.time.ZonedDateTime;

/**
 * 네트워크 사용량 응답 DTO
 * 그래프 3.1, 3.2, 3.7: RX/TX 사용률 및 대역폭
 */
public record NetworkUsageResponse(
        ZonedDateTime time,
        Double rxBytesPerSec,
        Double txBytesPerSec
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime time;
        private Double rxBytesPerSec;
        private Double txBytesPerSec;

        public Builder time(ZonedDateTime time) {
            this.time = time;
            return this;
        }

        public Builder rxBytesPerSec(Double rxBytesPerSec) {
            this.rxBytesPerSec = rxBytesPerSec;
            return this;
        }

        public Builder txBytesPerSec(Double txBytesPerSec) {
            this.txBytesPerSec = txBytesPerSec;
            return this;
        }

        public NetworkUsageResponse build() {
            return new NetworkUsageResponse(time, rxBytesPerSec, txBytesPerSec);
        }
    }
}