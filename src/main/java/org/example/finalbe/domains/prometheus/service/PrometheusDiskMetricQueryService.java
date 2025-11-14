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

        return new DiskMetricsResponse(
                getCurrentDiskUsage(),
                getDiskUsageTrend(startTime, endTime),
                getDiskIoTrend(startTime, endTime),
                generateSpacePrediction(startTime, endTime),
                getInodeUsage(endTime)
        );
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
        try {
            return prometheusDiskMetricRepository.getDiskUsageTrend(startTime, endTime)
                    .stream()
                    .map(DiskUsageResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("디스크 사용률 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<DiskIoResponse> getDiskIoTrend(Instant startTime, Instant endTime) {
        try {
            List<Object[]> ioSpeedRows = prometheusDiskMetricRepository.getDiskIoSpeed(startTime, endTime);
            List<Object[]> iopsRows = prometheusDiskMetricRepository.getDiskIops(startTime, endTime);
            List<Object[]> utilizationRows = prometheusDiskMetricRepository.getDiskIoUtilization(startTime, endTime);

            List<DiskIoResponse> result = new ArrayList<>();
            for (int i = 0; i < ioSpeedRows.size(); i++) {
                Object[] ioSpeed = ioSpeedRows.get(i);
                Object[] iops = i < iopsRows.size() ? iopsRows.get(i) : null;
                Object[] utilization = i < utilizationRows.size() ? utilizationRows.get(i) : null;

                result.add(DiskIoResponse.from(ioSpeed, iops, utilization));
            }
            return result;
        } catch (Exception e) {
            log.error("디스크 I/O 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<InodeUsageResponse> getInodeUsage(Instant time) {
        try {
            return prometheusDiskMetricRepository.getInodeUsage(time)
                    .stream()
                    .map(InodeUsageResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("Inode 사용률 조회 실패", e);
            return List.of();
        }
    }

    private List<DiskSpacePredictionResponse> generateSpacePrediction(Instant startTime, Instant endTime) {
        List<DiskSpacePredictionResponse> result = new ArrayList<>();

        try {
            // 실제 데이터 추가
            List<Object[]> actualData = prometheusDiskMetricRepository.getDiskUsageTrend(startTime, endTime);

            for (Object[] row : actualData) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);
                Double freeBytes = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                Double usedBytes = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                Double usagePercent = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

                result.add(DiskSpacePredictionResponse.actual(timeKst, freeBytes, usedBytes, usagePercent));
            }

            // 예측 데이터 생성 (선형 회귀 기반 단순 예측)
            if (actualData.size() >= 2) {
                Object[] lastData = actualData.get(actualData.size() - 1);
                Object[] prevData = actualData.get(actualData.size() - 2);

                Instant lastTime = (Instant) lastData[0];
                Double lastUsage = lastData[4] != null ? ((Number) lastData[4]).doubleValue() : 0.0;
                Double prevUsage = prevData[4] != null ? ((Number) prevData[4]).doubleValue() : 0.0;

                double growthRate = lastUsage - prevUsage;

                // 향후 24시간 예측
                for (int i = 1; i <= 24; i++) {
                    ZonedDateTime predictedTime = lastTime.atZone(KST_ZONE).plusHours(i);
                    Double predictedUsage = Math.min(100.0, lastUsage + (growthRate * i));

                    result.add(DiskSpacePredictionResponse.predicted(predictedTime, predictedUsage));
                }
            }
        } catch (Exception e) {
            log.error("디스크 공간 예측 생성 실패", e);
        }

        return result;
    }
}