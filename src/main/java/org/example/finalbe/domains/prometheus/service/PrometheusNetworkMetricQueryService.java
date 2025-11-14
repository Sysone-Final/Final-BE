package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.network.*;
import org.example.finalbe.domains.prometheus.repository.network.PrometheusNetworkMetricRepository;
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
public class PrometheusNetworkMetricQueryService {

    private final PrometheusNetworkMetricRepository prometheusNetworkMetricRepository;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public NetworkMetricsResponse getNetworkMetrics(Instant startTime, Instant endTime) {
        ZonedDateTime startKst = startTime.atZone(KST_ZONE);
        ZonedDateTime endKst = endTime.atZone(KST_ZONE);

        log.info("네트워크 메트릭 조회 시작 (KST) - startTime: {}, endTime: {}", startKst, endKst);

        Object[] currentUsage = getCurrentNetworkUsage();
        Double currentRxBps = currentUsage != null && currentUsage[0] != null ?
                ((Number) currentUsage[0]).doubleValue() : 0.0;
        Double currentTxBps = currentUsage != null && currentUsage[1] != null ?
                ((Number) currentUsage[1]).doubleValue() : 0.0;

        return new NetworkMetricsResponse(
                currentRxBps,
                currentTxBps,
                getNetworkUsageTrend(startTime, endTime),
                getNetworkPacketsTrend(startTime, endTime),
                getNetworkBytesTrend(startTime, endTime),
                getNetworkErrorsTrend(startTime, endTime),
                getNetworkInterfaceStatus(endTime)
        );
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
        try {
            return prometheusNetworkMetricRepository.getNetworkUsageTrend(startTime, endTime)
                    .stream()
                    .map(NetworkUsageResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("네트워크 사용량 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<NetworkPacketsResponse> getNetworkPacketsTrend(Instant startTime, Instant endTime) {
        try {
            return prometheusNetworkMetricRepository.getNetworkPacketsTrend(startTime, endTime)
                    .stream()
                    .map(NetworkPacketsResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("네트워크 패킷 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<NetworkBytesResponse> getNetworkBytesTrend(Instant startTime, Instant endTime) {
        try {
            return prometheusNetworkMetricRepository.getNetworkBytesTrend(startTime, endTime)
                    .stream()
                    .map(NetworkBytesResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("네트워크 바이트 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<NetworkErrorsResponse> getNetworkErrorsTrend(Instant startTime, Instant endTime) {
        try {
            return prometheusNetworkMetricRepository.getNetworkErrorsAndDrops(startTime, endTime)
                    .stream()
                    .map(NetworkErrorsResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("네트워크 에러 추이 조회 실패", e);
            return List.of();
        }
    }

    private List<NetworkInterfaceStatusResponse> getNetworkInterfaceStatus(Instant time) {
        try {
            return prometheusNetworkMetricRepository.getNetworkInterfaceStatus(time)
                    .stream()
                    .map(NetworkInterfaceStatusResponse::from)
                    .toList();
        } catch (Exception e) {
            log.error("네트워크 인터페이스 상태 조회 실패", e);
            return List.of();
        }
    }
}