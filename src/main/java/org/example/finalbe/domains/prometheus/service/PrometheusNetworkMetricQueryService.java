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
import java.util.ArrayList;
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

        List<NetworkUsageResponse> networkUsageTrend = getNetworkUsageTrend(startTime, endTime);
        List<NetworkPacketsResponse> networkPacketsTrend = getNetworkPacketsTrend(startTime, endTime);
        List<NetworkBytesResponse> networkBytesTrend = getNetworkBytesTrend(startTime, endTime);
        List<NetworkErrorsResponse> networkErrorsTrend = getNetworkErrorsTrend(startTime, endTime);
        List<NetworkInterfaceStatusResponse> interfaceStatus = getNetworkInterfaceStatus(endTime);

        return NetworkMetricsResponse.builder()
                .currentRxBytesPerSec(currentRxBps)
                .currentTxBytesPerSec(currentTxBps)
                .networkUsageTrend(networkUsageTrend)
                .networkPacketsTrend(networkPacketsTrend)
                .networkBytesTrend(networkBytesTrend)
                .networkErrorsTrend(networkErrorsTrend)
                .interfaceStatus(interfaceStatus)
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
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(NetworkUsageResponse.builder()
                        .time(timeKst)
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
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(NetworkPacketsResponse.builder()
                        .time(timeKst)
                        .totalRxPackets(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .totalTxPackets(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("네트워크 패킷 추이 조회 실패", e);
        }
        return result;
    }

    private List<NetworkBytesResponse> getNetworkBytesTrend(Instant startTime, Instant endTime) {
        List<NetworkBytesResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusNetworkMetricRepository.getNetworkBytesTrend(startTime, endTime);
            for (Object[] row : rows) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(NetworkBytesResponse.builder()
                        .time(timeKst)
                        .totalReceiveBytes(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0)
                        .totalTransmitBytes(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0)
                        .build());
            }
        } catch (Exception e) {
            log.error("네트워크 바이트 추이 조회 실패", e);
        }
        return result;
    }

    private List<NetworkErrorsResponse> getNetworkErrorsTrend(Instant startTime, Instant endTime) {
        List<NetworkErrorsResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusNetworkMetricRepository.getNetworkErrorsAndDrops(startTime, endTime);
            for (Object[] row : rows) {
                Instant instant = (Instant) row[0];
                ZonedDateTime timeKst = instant.atZone(KST_ZONE);

                result.add(NetworkErrorsResponse.builder()
                        .time(timeKst)
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

    private List<NetworkInterfaceStatusResponse> getNetworkInterfaceStatus(Instant time) {
        List<NetworkInterfaceStatusResponse> result = new ArrayList<>();
        try {
            List<Object[]> rows = prometheusNetworkMetricRepository.getNetworkInterfaceStatus(time);
            for (Object[] row : rows) {
                String deviceName = (String) row[0];
                Integer operStatus = row[1] != null ? ((Number) row[1]).intValue() : 0;
                String statusText = (operStatus == 1) ? "UP" : "DOWN";

                result.add(NetworkInterfaceStatusResponse.builder()
                        .device(deviceName)
                        .operStatus(operStatus)
                        .statusText(statusText)
                        .build());
            }
        } catch (Exception e) {
            log.error("네트워크 인터페이스 상태 조회 실패", e);
        }
        return result;
    }
}