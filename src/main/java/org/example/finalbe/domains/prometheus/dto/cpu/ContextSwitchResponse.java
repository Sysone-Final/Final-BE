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
public class ContextSwitchResponse {
    private Instant time;
    private Double contextSwitchesPerSec;
}