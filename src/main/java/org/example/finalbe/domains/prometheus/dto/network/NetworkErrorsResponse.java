package org.example.finalbe.domains.prometheus.dto.network;

import java.time.ZonedDateTime;

/**
 * 네트워크 에러 응답 DTO
 * 그래프 3.8: 에러 및 드롭 패킷
 */
public record NetworkErrorsResponse(
        ZonedDateTime time,
        Double rxErrors,
        Double txErrors,
        Double rxDrops,
        Double txDrops
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime time;
        private Double rxErrors;
        private Double txErrors;
        private Double rxDrops;
        private Double txDrops;

        public Builder time(ZonedDateTime time) {
            this.time = time;
            return this;
        }

        public Builder rxErrors(Double rxErrors) {
            this.rxErrors = rxErrors;
            return this;
        }

        public Builder txErrors(Double txErrors) {
            this.txErrors = txErrors;
            return this;
        }

        public Builder rxDrops(Double rxDrops) {
            this.rxDrops = rxDrops;
            return this;
        }

        public Builder txDrops(Double txDrops) {
            this.txDrops = txDrops;
            return this;
        }

        public NetworkErrorsResponse build() {
            return new NetworkErrorsResponse(time, rxErrors, txErrors, rxDrops, txDrops);
        }
    }
}