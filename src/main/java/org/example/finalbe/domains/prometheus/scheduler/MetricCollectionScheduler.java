package org.example.finalbe.domains.prometheus.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.service.PrometheusRealtimeMetricService;
import org.example.finalbe.domains.prometheus.service.PrometheusSSEBroadcastService;
import org.example.finalbe.domains.prometheus.dto.serverroom.ServerRoomMetricsResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricCollectionScheduler {

    private final PrometheusRealtimeMetricService prometheusRealtimeMetricService;
    private final PrometheusSSEBroadcastService prometheusSseBroadcastService;

    /**
     * 15초마다 최신 메트릭 브로드캐스트 (새 테이블에서 조회)
     */
    @Scheduled(fixedRateString = "${monitoring.scheduler.metric-collection.fixed-rate:15000}")
    public void collectAndBroadcastMetrics() {
        try {
            if (prometheusSseBroadcastService.getTotalConnections() == 0) {
                log.debug("활성 SSE 연결 없음 - 메트릭 수집 스킵");
                return;
            }

            log.debug("메트릭 브로드캐스트 시작 - 연결 수: {}", prometheusSseBroadcastService.getTotalConnections());

            // ✅ 새 테이블에서 최신 1개만 조회 (초고속)
            ServerRoomMetricsResponse metrics = prometheusRealtimeMetricService.getLatestMetrics();

            prometheusSseBroadcastService.broadcast("metrics", metrics);

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
            int connections = prometheusSseBroadcastService.getTotalConnections();
            if (connections > 0) {
                prometheusSseBroadcastService.sendHeartbeat();
                log.debug("Heartbeat 전송 완료 - 연결 수: {}", connections);
            }
        } catch (Exception e) {
            log.error("Heartbeat 전송 실패", e);
        }
    }
}