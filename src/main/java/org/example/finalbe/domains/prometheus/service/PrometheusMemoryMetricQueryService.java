package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.memory.*;
import org.example.finalbe.domains.prometheus.repository.memory.PrometheusMemoryMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusMemoryMetricQueryService {

    private final PrometheusMemoryMetricRepository prometheusMemoryMetricRepository;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_TOP_N_LIMIT = 10;

    public MemoryMetricsResponse getMemoryMetrics(Instant startTime, Instant endTime) {
        ZonedDateTime startKst = startTime.atZone(KST_ZONE);
        ZonedDateTime endKst = endTime.atZone(KST_ZONE);

        log.info("메모리 메트릭 조회 시작 (KST) - startTime: {}, endTime: {}", startKst, endKst);

        return new MemoryMetricsResponse(
                getCurrentMemoryUsage(),
                getMemoryUsageTrend(startTime, endTime),
                getMemoryComposition(startTime, endTime),
                getSwapUsageTrend(startTime, endTime),
                getTopNMemoryUsage(DEFAULT_TOP_N_LIMIT)
        );
    }

    private Double getCurrentMemoryUsage() {
        try {
            Object[] result = prometheusMemoryMetricRepository.getCurrentMemoryUsage();
            if (result != null && result.length > 2) {
                return ((Number) result[2]).doubleValue();
            }
        } catch (Exception e) {
            log.error("현재 메모리 사용률 조회 실패", e);
        }
        return 0.0;
    }

    private List<MemoryUsageResponse> getMemoryUsageTrend(Instant startTime, Instant endTime) {
        try {
            return prometheusMemoryMetricRepository.getMemoryUsageTrend(startTime, endTime)
                    .stream()
                    .map(MemoryUsageResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("메모리 사용률 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<MemoryCompositionResponse> getMemoryComposition(Instant startTime, Instant endTime) {
        try {
            return prometheusMemoryMetricRepository.getMemoryComposition(startTime, endTime)
                    .stream()
                    .map(MemoryCompositionResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("메모리 구성 상세 조회 실패", e);
            return List.of();
        }
    }

    private List<SwapUsageResponse> getSwapUsageTrend(Instant startTime, Instant endTime) {
        try {
            return prometheusMemoryMetricRepository.getSwapUsageTrend(startTime, endTime)
                    .stream()
                    .map(SwapUsageResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("SWAP 메모리 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<TopNMemoryUsageResponse> getTopNMemoryUsage(int limit) {
        try {
            return prometheusMemoryMetricRepository.getTopNMemoryUsage(limit)
                    .stream()
                    .map(TopNMemoryUsageResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("메모리 사용량 Top N 조회 실패", e);
            return List.of();
        }
    }
}