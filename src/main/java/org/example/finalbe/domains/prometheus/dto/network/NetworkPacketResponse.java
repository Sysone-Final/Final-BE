package org.example.finalbe.domains.prometheus.dto.network;

import java.time.ZonedDateTime;

/**
 * 네트워크 누적 패킷 응답 DTO
 * 그래프 3.3, 3.4: 총 수신/송신 패킷 수
 */
public record NetworkPacketResponse(
        ZonedDateTime time,
        Double totalReceivePackets,
        Double totalTransmitPackets
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime time;
        private Double totalReceivePackets;
        private Double totalTransmitPackets;

        public Builder time(ZonedDateTime time) {
            this.time = time;
            return this;
        }

        public Builder totalReceivePackets(Double totalReceivePackets) {
            this.totalReceivePackets = totalReceivePackets;
            return this;
        }

        public Builder totalTransmitPackets(Double totalTransmitPackets) {
            this.totalTransmitPackets = totalTransmitPackets;
            return this;
        }

        public NetworkPacketResponse build() {
            return new NetworkPacketResponse(time, totalReceivePackets, totalTransmitPackets);
        }
    }
}