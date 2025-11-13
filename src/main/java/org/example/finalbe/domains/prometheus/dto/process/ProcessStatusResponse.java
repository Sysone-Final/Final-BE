package org.example.finalbe.domains.prometheus.dto.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStatusResponse {
    private Instant time;
    private Double runningProcesses;
    private Double blockedProcesses;
}