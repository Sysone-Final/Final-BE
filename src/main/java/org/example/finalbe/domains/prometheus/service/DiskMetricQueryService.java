package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.disk.*;
import org.example.finalbe.domains.prometheus.dto.disk.DiskMetricsResponse;
import org.example.finalbe.domains.prometheus.repository.disk.DiskMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiskMetricQueryService {

    private final DiskMetricRepository diskMetricRepository;

    public DiskMetricsResponse getDiskMetrics(Instant startTime, Instant endTime) {
        log.info("디스크 메트릭 조회 시작 - startTime: {}, endTime: {}", startTime, endTime);

        Double currentDiskUsagePercent = getCurrentDiskUsage();
        List<DiskUsageResponse> diskUsageTrend = getDiskUsageTrend(startTime, endTime);
        List<DiskIoResponse> diskIoTrend = getDiskIoTrend(startTime, endTime);
        List<InodeUsageResponse> inodeUsage = getInodeUsage(endTime);

        return DiskMetricsResponse.builder()
                .currentDiskUsagePercent(currentDiskUsagePercent)
                .diskUsageTrend(diskUsageTrend)
                .diskIoTrend(diskIoTrend)
                .inodeUsage(inodeUsage)
                .build();
    }

    private Double getCurrentDiskUsage() {
        try {
            Object[] result = diskMetricRepository.getCurrentDiskUsage();
            if (result != null && result.length > 2) {
                return ((Number) result[2]).doubleValue();
            }
        } catch (Exception e) {
            log.error("현재 디스크 사용률 조회 실패", e);
        }
        return 0.0;
    }

    private List<DiskUsageResponse> getDiskUsageTrend(Instant startTime, Instant endTime) {
        List<DiskUsageResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = diskMetricRepository.getDiskUsageTrend(startTime, endTime);
            for (Object[] row : rows) {
                result.add(DiskUsageResponse.builder()
                        .time((Instant) row[0])
                        .totalBytes(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .freeBytes(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .usedBytes(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .usagePercent(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("디스크 사용률 추이 조회 실패", e);
        }
        return result;
    }

    private List<DiskIoResponse> getDiskIoTrend(Instant startTime, Instant endTime) {
        List<DiskIoResponse> result = new ArrayList<>();
        try {
            List<Object[]> ioSpeedRows = diskMetricRepository.getDiskIoSpeed(startTime, endTime);
            List<Object[]> iopsRows = diskMetricRepository.getDiskIops(startTime, endTime);
            List<Object[]> utilizationRows = diskMetricRepository.getDiskIoUtilization(startTime, endTime);

            for (int i = 0; i < ioSpeedRows.size(); i++) {
                Object[] ioSpeed = ioSpeedRows.get(i);
                Object[] iops = i < iopsRows.size() ? iopsRows.get(i) : null;
                Object[] utilization = i < utilizationRows.size() ? utilizationRows.get(i) : null;

                result.add(DiskIoResponse.builder()
                        .time((Instant) ioSpeed[0])
                        .readBytesPerSec(ioSpeed[1] != null ? ((Number) ioSpeed[1]).doubleValue() : 0.0)
                        .writeBytesPerSec(ioSpeed[2] != null ? ((Number) ioSpeed[2]).doubleValue() : 0.0)
                        .readIops(iops != null && iops[1] != null ? ((Number) iops[1]).doubleValue() : 0.0)
                        .writeIops(iops != null && iops[2] != null ? ((Number) iops[2]).doubleValue() : 0.0)
                        .ioUtilizationPercent(utilization != null && utilization[1] != null ? ((Number) utilization[1]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("디스크 I/O 추이 조회 실패", e);
        }
        return result;
    }

    private List<InodeUsageResponse> getInodeUsage(Instant time) {
        List<InodeUsageResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = diskMetricRepository.getInodeUsage(time);
            for (Object[] row : rows) {
                result.add(InodeUsageResponse.builder()
                        .deviceId((Integer) row[0])
                        .mountpointId((Integer) row[1])
                        .totalInodes(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .freeInodes(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .usedInodes(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                        .inodeUsagePercent(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("inode 사용률 조회 실패", e);
        }
        return result;
    }
}