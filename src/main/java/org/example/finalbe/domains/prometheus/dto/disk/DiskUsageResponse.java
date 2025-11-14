package org.example.finalbe.domains.prometheus.dto.disk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskUsageResponse {
    private ZonedDateTime time;
    private Double totalBytes;
    private Double freeBytes;
    private Double usedBytes;
    private Double usagePercent;
}