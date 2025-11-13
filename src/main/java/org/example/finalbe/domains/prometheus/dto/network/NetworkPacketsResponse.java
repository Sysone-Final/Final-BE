package org.example.finalbe.domains.prometheus.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkPacketsResponse {
    private Instant time;
    private Double totalRxPackets;
    private Double totalTxPackets;
}