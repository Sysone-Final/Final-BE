package org.example.finalbe.domains.prometheus.dto.cpu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadAverageResponse {
    private Instant time;
    private Double load1;
    private Double load5;
    private Double load15;
}