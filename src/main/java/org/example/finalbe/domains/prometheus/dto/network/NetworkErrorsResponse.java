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
public class NetworkErrorsResponse {
    private ZonedDateTime time;
    private Double rxErrors;
    private Double txErrors;
    private Double rxDrops;
    private Double txDrops;
}