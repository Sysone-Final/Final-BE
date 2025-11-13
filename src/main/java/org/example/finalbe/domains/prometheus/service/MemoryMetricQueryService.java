package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.memory.*;
import org.example.finalbe.domains.prometheus.dto.memory.MemoryMetricsResponse;
import org.example.finalbe.domains.prometheus.repository.memory.MemoryMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoryMetricQueryService {

    private final MemoryMetricRepository memoryMetricRepository;

    public MemoryMetricsResponse getMemoryMetrics(Instant startTime, Instant endTime) {
        log.info("메모리 메트릭 조회 시작 - startTime: {}, endTime: {}", startTime, endTime);

        Double currentMemoryUsagePercent = getCurrentMemoryUsage();
        List<MemoryUsageResponse> memoryUsageTrend = getMemoryUsageTrend(startTime, endTime);
        List<MemoryCompositionResponse> memoryComposition = getMemoryComposition(startTime, endTime);
        List<SwapUsageResponse> swapUsageTrend = getSwapUsageTrend(startTime, endTime);

        return MemoryMetricsResponse.builder()
                .currentMemoryUsagePercent(currentMemoryUsagePercent)
                .memoryUsageTrend(memoryUsageTrend)
                .memoryComposition(memoryComposition)
                .swapUsageTrend(swapUsageTrend)
                .build();
    }

    private Double getCurrentMemoryUsage() {
        try {
            Object[] result = memoryMetricRepository.getCurrentMemoryUsage();
            if (result != null && result.length > 2) {
                return ((Number) result[2]).doubleValue();
            }
        } catch (Exception e) {
            log.error("현재 메모리 사용률 조회 실패", e);
        }
        return 0.0;
    }

    private List<MemoryUsageResponse> getMemoryUsageTrend(Instant startTime, Instant endTime) {
        List<MemoryUsageResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = memoryMetricRepository.getMemoryUsageTrend(startTime, endTime);
            for (Object[] row : rows) {
                Double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                Double available = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                result.add(MemoryUsageResponse.builder()
                        .time((Instant) row[0])
                        .totalMemory(total)
                        .availableMemory(available)
                        .usedMemory(total - available)
                        .memoryUsagePercent(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("메모리 사용률 추이 조회 실패", e);
        }
        return result;
    }

    private List<MemoryCompositionResponse> getMemoryComposition(Instant startTime, Instant endTime) {
        List<MemoryCompositionResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = memoryMetricRepository.getMemoryComposition(startTime, endTime);
            for (Object[] row : rows) {
                result.add(MemoryCompositionResponse.builder()
                        .time((Instant) row[0])
                        .active(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .inactive(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .buffers(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .cached(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                        .free(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("메모리 구성 상세 조회 실패", e);
        }
        return result;
    }

    private List<SwapUsageResponse> getSwapUsageTrend(Instant startTime, Instant endTime) {
        List<SwapUsageResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = memoryMetricRepository.getSwapUsageTrend(startTime, endTime);
            for (Object[] row : rows) {
                result.add(SwapUsageResponse.builder()
                        .time((Instant) row[0])
                        .totalSwap(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .freeSwap(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .usedSwap(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .swapUsagePercent(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("SWAP 메모리 추이 조회 실패", e);
        }
        return result;
    }
}