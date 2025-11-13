package org.example.finalbe.domains.prometheus.dto.disk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskUsageResponse {
    private Instant time;
    private Double totalBytes;
    private Double freeBytes;
    private Double usedBytes;
    private Double usagePercent;
}