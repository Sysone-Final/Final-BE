package org.example.finalbe.domains.prometheus.dto.network;

import java.util.List;

/**
 * 네트워크 메트릭 전체 응답 DTO (완전 구현)
 */
public record NetworkMetricsResponse(
        Double currentRxBytesPerSec,
        Double currentTxBytesPerSec,
        List<NetworkUsageResponse> networkUsageTrend,
        List<NetworkPacketsResponse> networkPacketsTrend,
        List<NetworkBytesResponse> networkBytesTrend,
        List<NetworkErrorsResponse> networkErrorsTrend,
        List<NetworkInterfaceStatusResponse> interfaceStatus
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Double currentRxBytesPerSec;
        private Double currentTxBytesPerSec;
        private List<NetworkUsageResponse> networkUsageTrend;
        private List<NetworkPacketsResponse> networkPacketsTrend;
        private List<NetworkBytesResponse> networkBytesTrend;
        private List<NetworkErrorsResponse> networkErrorsTrend;
        private List<NetworkInterfaceStatusResponse> interfaceStatus;

        public Builder currentRxBytesPerSec(Double currentRxBytesPerSec) {
            this.currentRxBytesPerSec = currentRxBytesPerSec;
            return this;
        }

        public Builder currentTxBytesPerSec(Double currentTxBytesPerSec) {
            this.currentTxBytesPerSec = currentTxBytesPerSec;
            return this;
        }

        public Builder networkUsageTrend(List<NetworkUsageResponse> networkUsageTrend) {
            this.networkUsageTrend = networkUsageTrend;
            return this;
        }

        public Builder networkPacketsTrend(List<NetworkPacketsResponse> networkPacketsTrend) {
            this.networkPacketsTrend = networkPacketsTrend;
            return this;
        }

        public Builder networkBytesTrend(List<NetworkBytesResponse> networkBytesTrend) {
            this.networkBytesTrend = networkBytesTrend;
            return this;
        }

        public Builder networkErrorsTrend(List<NetworkErrorsResponse> networkErrorsTrend) {
            this.networkErrorsTrend = networkErrorsTrend;
            return this;
        }

        public Builder interfaceStatus(List<NetworkInterfaceStatusResponse> interfaceStatus) {
            this.interfaceStatus = interfaceStatus;
            return this;
        }

        public NetworkMetricsResponse build() {
            return new NetworkMetricsResponse(
                    currentRxBytesPerSec,
                    currentTxBytesPerSec,
                    networkUsageTrend,
                    networkPacketsTrend,
                    networkBytesTrend,
                    networkErrorsTrend,
                    interfaceStatus
            );
        }
    }
}