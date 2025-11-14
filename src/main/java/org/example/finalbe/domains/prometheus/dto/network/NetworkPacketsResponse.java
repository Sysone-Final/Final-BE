package org.example.finalbe.domains.prometheus.dto.network;

import java.time.ZonedDateTime;

/**
 * 네트워크 패킷 응답 DTO
 */
public record NetworkPacketsResponse(
        ZonedDateTime time,
        Double totalRxPackets,
        Double totalTxPackets
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ZonedDateTime time;
        private Double totalRxPackets;
        private Double totalTxPackets;

        public Builder time(ZonedDateTime time) {
            this.time = time;
            return this;
        }

        public Builder totalRxPackets(Double totalRxPackets) {
            this.totalRxPackets = totalRxPackets;
            return this;
        }

        public Builder totalTxPackets(Double totalTxPackets) {
            this.totalTxPackets = totalTxPackets;
            return this;
        }

        public NetworkPacketsResponse build() {
            return new NetworkPacketsResponse(time, totalRxPackets, totalTxPackets);
        }
    }
}