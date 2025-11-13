package org.example.finalbe.domains.prometheus.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkMetricsResponse {
    private Double currentRxBytesPerSec;
    private Double currentTxBytesPerSec;
    private List<NetworkUsageResponse> networkUsageTrend;
    private List<NetworkPacketsResponse> networkPacketsTrend;
    private List<NetworkErrorsResponse> networkErrorsTrend;
}