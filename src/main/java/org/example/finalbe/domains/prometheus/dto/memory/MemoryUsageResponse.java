package org.example.finalbe.domains.prometheus.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryUsageResponse {
    private ZonedDateTime time;
    private Double totalMemory;
    private Double availableMemory;
    private Double usedMemory;
    private Double memoryUsagePercent;
}