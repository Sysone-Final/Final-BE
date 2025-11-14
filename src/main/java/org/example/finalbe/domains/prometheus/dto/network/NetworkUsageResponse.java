package org.example.finalbe.domains.prometheus.dto.network;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkUsageResponse {
    private ZonedDateTime time;
    private Double rxBytesPerSec;
    private Double txBytesPerSec;
}