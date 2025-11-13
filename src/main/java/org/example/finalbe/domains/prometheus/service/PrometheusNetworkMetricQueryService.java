package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.network.*;
import org.example.finalbe.domains.prometheus.dto.network.NetworkMetricsResponse;
import org.example.finalbe.domains.prometheus.repository.network.PrometheusNetworkMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrometheusNetworkMetricQueryService {

    private final PrometheusNetworkMetricRepository prometheusNetworkMetricRepository;

    public NetworkMetricsResponse getNetworkMetrics(Instant startTime, Instant endTime) {
        log.info("네트워크 메트릭 조회 시작 - startTime: {}, endTime: {}", startTime, endTime);

        Object[] currentUsage = getCurrentNetworkUsage();
        Double currentRxBps = currentUsage != null ? ((Number) currentUsage[0]).doubleValue() : 0.0;
        Double currentTxBps = currentUsage != null ? ((Number) currentUsage[1]).doubleValue() : 0.0;

        List<NetworkUsageResponse> networkUsageTrend = getNetworkUsageTrend(startTime, endTime);
        List<NetworkPacketsResponse> networkPacketsTrend = getNetworkPacketsTrend(startTime, endTime);
        List<NetworkErrorsResponse> networkErrorsTrend = getNetworkErrorsTrend(startTime, endTime);

        return NetworkMetricsResponse.builder()
                .currentRxBytesPerSec(currentRxBps)
                .currentTxBytesPerSec(currentTxBps)
                .networkUsageTrend(networkUsageTrend)
                .networkPacketsTrend(networkPacketsTrend)
                .networkErrorsTrend(networkErrorsTrend)
                .build();
    }

    private Object[] getCurrentNetworkUsage() {
        try {
            return prometheusNetworkMetricRepository.getCurrentNetworkUsage();
        } catch (Exception e) {
            log.error("현재 네트워크 사용량 조회 실패", e);
            return null;
        }
    }

    private List<NetworkUsageResponse> getNetworkUsageTrend(Instant startTime, Instant endTime) {
        List<NetworkUsageResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusNetworkMetricRepository.getNetworkUsageTrend(startTime, endTime);
            for (Object[] row : rows) {
                result.add(NetworkUsageResponse.builder()
                        .time((Instant) row[0])
                        .rxBytesPerSec(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .txBytesPerSec(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("네트워크 사용량 추이 조회 실패", e);
        }
        return result;
    }

    private List<NetworkPacketsResponse> getNetworkPacketsTrend(Instant startTime, Instant endTime) {
        List<NetworkPacketsResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusNetworkMetricRepository.getNetworkPacketsTrend(startTime, endTime);
            for (Object[] row : rows) {
                result.add(NetworkPacketsResponse.builder()
                        .time((Instant) row[0])
                        .totalRxPackets(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .totalTxPackets(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("네트워크 패킷 추이 조회 실패", e);
        }
        return result;
    }

    private List<NetworkErrorsResponse> getNetworkErrorsTrend(Instant startTime, Instant endTime) {
        List<NetworkErrorsResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusNetworkMetricRepository.getNetworkErrorsAndDrops(startTime, endTime);
            for (Object[] row : rows) {
                result.add(NetworkErrorsResponse.builder()
                        .time((Instant) row[0])
                        .rxErrors(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .txErrors(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .rxDrops(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                        .txDrops(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("네트워크 에러 추이 조회 실패", e);
        }
        return result;
    }
}