package org.example.finalbe.domains.prometheus.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwapUsageResponse {
    private Instant time;
    private Double totalSwap;
    private Double freeSwap;
    private Double usedSwap;
    private Double swapUsagePercent;
}