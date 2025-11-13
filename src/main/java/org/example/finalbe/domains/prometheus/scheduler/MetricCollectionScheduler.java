package org.example.finalbe.domains.prometheus.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.serverroom.ServerRoomMetricsResponse;
import org.example.finalbe.domains.prometheus.service.PrometheusMetricService;
import org.example.finalbe.domains.prometheus.service.SSEBroadcastService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricCollectionScheduler {

    private final PrometheusMetricService prometheusMetricService;
    private final SSEBroadcastService sseBroadcastService;

    /**
     * 5초마다 메트릭 수집 및 브로드캐스트
     */
    @Scheduled(fixedRateString = "${monitoring.scheduler.metric-collection.fixed-rate:5000}")
    public void collectAndBroadcastMetrics() {
        try {
            if (sseBroadcastService.getTotalConnections() == 0) {
                log.debug("활성 SSE 연결 없음 - 메트릭 수집 스킵");
                return;
            }

            log.debug("메트릭 수집 시작 - 연결 수: {}", sseBroadcastService.getTotalConnections());

            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(5, ChronoUnit.MINUTES);

            ServerRoomMetricsResponse metrics = prometheusMetricService.getAllMetrics(startTime, endTime);

            sseBroadcastService.broadcast("metrics", metrics);

            log.debug("메트릭 브로드캐스트 완료");

        } catch (Exception e) {
            log.error("메트릭 수집 및 브로드캐스트 실패", e);
        }
    }

    /**
     * 30초마다 Heartbeat 전송
     */
    @Scheduled(fixedRateString = "${monitoring.sse.heartbeat-interval:30000}")
    public void sendHeartbeat() {
        try {
            int connections = sseBroadcastService.getTotalConnections();
            if (connections > 0) {
                sseBroadcastService.sendHeartbeat();
                log.debug("Heartbeat 전송 완료 - 연결 수: {}", connections);
            }
        } catch (Exception e) {
            log.error("Heartbeat 전송 실패", e);
        }
    }
}