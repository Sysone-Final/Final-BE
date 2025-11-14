package org.example.finalbe.domains.prometheus.dto.network;

import java.util.List;

public record NetworkMetricsResponse(
        Double currentRxBytesPerSec,
        Double currentTxBytesPerSec,
        List<NetworkUsageResponse> networkUsageTrend,
        List<NetworkPacketsResponse> networkPacketsTrend,
        List<NetworkBytesResponse> networkBytesTrend,
        List<NetworkErrorsResponse> networkErrorsTrend,
        List<NetworkInterfaceStatusResponse> interfaceStatus
) {
}