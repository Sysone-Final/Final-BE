package org.example.finalbe.domains.prometheus.dto.disk;

import java.util.List;

public record DiskMetricsResponse(
        Double currentDiskUsagePercent,
        List<DiskUsageResponse> diskUsageTrend,
        List<DiskIoResponse> diskIoTrend,
        List<DiskSpacePredictionResponse> spacePredictionTrend,
        List<InodeUsageResponse> inodeUsage
) {
}