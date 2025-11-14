package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.disk.*;
import org.example.finalbe.domains.prometheus.repository.disk.PrometheusDiskMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusDiskMetricQueryService {

    private final PrometheusDiskMetricRepository prometheusDiskMetricRepository;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public DiskMetricsResponse getDiskMetrics(Instant startTime, Instant endTime) {
        ZonedDateTime startKst = startTime.atZone(KST_ZONE);
        ZonedDateTime endKst = endTime.atZone(KST_ZONE);

        log.info("디스크 메트릭 조회 시작 (KST) - startTime: {}, endTime: {}", startKst, endKst);

        Double currentDiskUsagePercent = getCurrentDiskUsage();
        List<DiskUsageResponse> diskUsageTrend = getDiskUsageTrend(startTime, endTime);
        List<DiskIoResponse> diskIoTrend = getDiskIoTrend(startTime, endTime);
        List<InodeUsageResponse> inodeUsage = getInodeUsage(endTime);
        List<DiskSpacePredictionResponse> spacePredictionTrend = generateSpacePrediction(startTime, endTime);

        return DiskMetricsResponse.builder()
                .currentDiskUsagePercent(currentDiskUsagePercent)
                .diskUsageTrend(diskUsageTrend)
                .diskIoTrend(diskIoTrend)
                .spacePredictionTrend(spacePredictionTrend)
                .inodeUsage(inodeUsage)
                .build();
    }

    private Double getCurrentDiskUsage() {
        try {
            Object[] result = prometheusDiskMetricRepository.getCurrentDiskUsage();
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
            List<Object[]> rows = prometheusDiskMetricRepository.getDiskUsageTrend(startTime, endTime);
            for (Object[] row : rows) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(DiskUsageResponse.builder()
                        .time(timeKst)
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
            List<Object[]> ioSpeedRows = prometheusDiskMetricRepository.getDiskIoSpeed(startTime, endTime);
            List<Object[]> iopsRows = prometheusDiskMetricRepository.getDiskIops(startTime, endTime);
            List<Object[]> utilizationRows = prometheusDiskMetricRepository.getDiskIoUtilization(startTime, endTime);

            for (int i = 0; i < ioSpeedRows.size(); i++) {
                Object[] ioSpeed = ioSpeedRows.get(i);
                Object[] iops = i < iopsRows.size() ? iopsRows.get(i) : null;
                Object[] utilization = i < utilizationRows.size() ? utilizationRows.get(i) : null;

                Instant instant = (Instant) ioSpeed[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(DiskIoResponse.builder()
                        .time(timeKst)
                        .readBytesPerSec(ioSpeed[1] != null ? ((Number) ioSpeed[1]).doubleValue() : 0.0)
                        .writeBytesPerSec(ioSpeed[2] != null ? ((Number) ioSpeed[2]).doubleValue() : 0.0)
                        .readIops(iops != null && iops[1] != null ? ((Number) iops[1]).doubleValue() : 0.0)
                        .writeIops(iops != null && iops[2] != null ? ((Number) iops[2]).doubleValue() : 0.0)
                        .ioUtilizationPercent(utilization != null && utilization[1] != null ?
                                ((Number) utilization[1]).doubleValue() : 0.0)
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
            List<Object[]> rows = prometheusDiskMetricRepository.getInodeUsage(time);
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
            log.error("Inode 사용률 조회 실패", e);
        }
        return result;
    }

    /**
     * 디스크 공간 예측 (그래프 4.5)
     * 선형 회귀를 사용한 7일 예측
     */
    private List<DiskSpacePredictionResponse> generateSpacePrediction(Instant startTime, Instant endTime) {
        List<DiskSpacePredictionResponse> result = new ArrayList<>();

        try {
            List<Object[]> historicalData = prometheusDiskMetricRepository
                    .getDiskUsageForPrediction(startTime, endTime);

            if (historicalData.isEmpty()) {
                log.warn("디스크 예측을 위한 과거 데이터가 없습니다.");
                return result;
            }

            List<Double> usageValues = new ArrayList<>();
            for (Object[] row : historicalData) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);
                Double freeBytes = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                Double usedBytes = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                Double usagePercent = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;

                result.add(DiskSpacePredictionResponse.builder()
                        .time(timeKst)
                        .freeBytes(freeBytes)
                        .usedBytes(usedBytes)
                        .usagePercent(usagePercent)
                        .isPrediction(false)
                        .predictedUsagePercent(null)
                        .build());

                usageValues.add(usagePercent);
            }

            int n = usageValues.size();
            if (n < 2) {
                log.warn("예측을 위한 데이터 포인트가 부족합니다. (최소 2개 필요)");
                return result;
            }

            double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
            for (int i = 0; i < n; i++) {
                double x = i;
                double y = usageValues.get(i);
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumXX += x * x;
            }

            double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;

            log.info("디스크 공간 예측 - 기울기: {}, 절편: {}", slope, intercept);

            ZonedDateTime lastTime = result.get(result.size() - 1).time();
            for (int i = 1; i <= 7; i++) {
                ZonedDateTime futureTime = lastTime.plus(i, ChronoUnit.DAYS);
                double predictedUsage = slope * (n + i) + intercept;
                predictedUsage = Math.max(0, Math.min(100, predictedUsage));

                result.add(DiskSpacePredictionResponse.builder()
                        .time(futureTime)
                        .freeBytes(null)
                        .usedBytes(null)
                        .usagePercent(null)
                        .isPrediction(true)
                        .predictedUsagePercent(predictedUsage)
                        .build());
            }

            log.info("디스크 공간 예측 완료 - 총 {} 포인트 (실제 {} + 예측 7)", result.size(), n);

        } catch (Exception e) {
            log.error("디스크 공간 예측 실패", e);
        }

        return result;
    }
}