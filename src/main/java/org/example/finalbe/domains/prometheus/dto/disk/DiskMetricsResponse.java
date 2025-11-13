package org.example.finalbe.domains.prometheus.dto.disk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskMetricsResponse {
    private Double currentDiskUsagePercent;
    private List<DiskUsageResponse> diskUsageTrend;
    private List<DiskIoResponse> diskIoTrend;
    private List<InodeUsageResponse> inodeUsage;
}