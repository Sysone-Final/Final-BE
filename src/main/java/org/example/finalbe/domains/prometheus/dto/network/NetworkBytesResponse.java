package org.example.finalbe.domains.prometheus.dto.network;

import java.time.ZonedDateTime;

/**
 * 네트워크 누적 바이트 응답 DTO
 * 그래프 3.5, 3.6: 총 수신/송신 바이트
 */
public record NetworkBytesResponse(
        ZonedDateTime time,
        Double totalReceiveBytes,
        Double totalTransmitBytes
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime time;
        private Double totalReceiveBytes;
        private Double totalTransmitBytes;

        public Builder time(ZonedDateTime time) {
            this.time = time;
            return this;
        }

        public Builder totalReceiveBytes(Double totalReceiveBytes) {
            this.totalReceiveBytes = totalReceiveBytes;
            return this;
        }

        public Builder totalTransmitBytes(Double totalTransmitBytes) {
            this.totalTransmitBytes = totalTransmitBytes;
            return this;
        }

        public NetworkBytesResponse build() {
            return new NetworkBytesResponse(time, totalReceiveBytes, totalTransmitBytes);
        }
    }
}