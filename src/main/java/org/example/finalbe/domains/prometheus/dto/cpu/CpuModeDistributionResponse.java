package org.example.finalbe.domains.prometheus.dto.cpu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CpuModeDistributionResponse {
    private ZonedDateTime time;
    private Double userMode;
    private Double systemMode;
    private Double iowaitMode;
    private Double irqMode;
    private Double softirqMode;
}