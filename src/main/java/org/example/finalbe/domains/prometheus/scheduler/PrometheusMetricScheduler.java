package org.example.finalbe.domains.prometheus.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.CollectionResultResponse;
import org.example.finalbe.domains.prometheus.dto.CollectionSummaryResponse;
import org.example.finalbe.domains.prometheus.dto.MetricsResponse;
import org.example.finalbe.domains.prometheus.service.PrometheusMetricCollector;
import org.example.finalbe.domains.prometheus.service.PrometheusMetricQueryService;
import org.example.finalbe.domains.prometheus.service.PrometheusSSEService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "prometheus.collection.enabled", havingValue = "true", matchIfMissing = true)
public class PrometheusMetricScheduler {

    private final PrometheusMetricCollector collector;
    private final PrometheusMetricQueryService queryService;
    private final PrometheusSSEService sseService;

    /**
     * 메트릭 수집 스케줄러 (15초마다 실행 - fixedDelay)
     * 이전 실행이 완료된 후 15초 대기
     */
    @Scheduled(
            fixedDelayString = "${prometheus.collection.fixed-delay:15000}",
            initialDelayString = "${prometheus.collection.initial-delay:5000}"
    )
    public void collectMetrics() {
        Instant collectionStart = Instant.now();

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 Prometheus 메트릭 수집 시작: {}", collectionStart);

        // 최근 15초간 데이터 수집
        Instant end = Instant.now();
        Instant start = end.minus(15, ChronoUnit.SECONDS);

        List<CollectionResultResponse> results = new ArrayList<>();

        try {
            // 병렬 수집
            CompletableFuture<Integer> cpuFuture = collector.collectCpuMetrics(start, end);
            CompletableFuture<Integer> memoryFuture = collector.collectMemoryMetrics(start, end);
            CompletableFuture<Integer> networkFuture = collector.collectNetworkMetrics(start, end);
            CompletableFuture<Integer> diskFuture = collector.collectDiskMetrics(start, end);

            // 모든 작업 완료 대기
            CompletableFuture.allOf(cpuFuture, memoryFuture, networkFuture, diskFuture).join();

            // 결과 수집
            Instant collectEnd = Instant.now();
            results.add(CollectionResultResponse.success("CPU", start, collectEnd, cpuFuture.get()));
            results.add(CollectionResultResponse.success("Memory", start, collectEnd, memoryFuture.get()));
            results.add(CollectionResultResponse.success("Network", start, collectEnd, networkFuture.get()));
            results.add(CollectionResultResponse.success("Disk", start, collectEnd, diskFuture.get()));

            // 요약 출력
            CollectionSummaryResponse summary = CollectionSummaryResponse.of(collectionStart, results);
            log.info("📊 수집 완료 - 총 {} rows, 성공: {}, 실패: {}, 소요시간: {}",
                    summary.totalRecords(), summary.successCount(), summary.failureCount(), summary.totalDuration());

            // SSE 브로드캐스트 (최근 15초 데이터)
            broadcastMetrics(start);

        } catch (Exception e) {
            log.error("❌ 메트릭 수집 중 오류 발생", e);
        } finally {
            long totalDuration = Instant.now().toEpochMilli() - collectionStart.toEpochMilli();
            log.info("⏱️ 전체 실행 시간: {}ms", totalDuration);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        }
    }

    /**
     * SSE 브로드캐스트 (수집 직후)
     */
    private void broadcastMetrics(Instant since) {
        try {
            int connections = sseService.getTotalConnections();
            if (connections == 0) {
                log.debug("📭 활성 SSE 연결 없음 - 브로드캐스트 스킵");
                return;
            }

            log.debug("📡 SSE 브로드캐스트 시작 - 연결 수: {}", connections);

            MetricsResponse metrics = queryService.getRecentMetrics(since);
            sseService.broadcast("metrics", metrics);

            log.debug("✅ SSE 브로드캐스트 완료 - {} records", metrics.totalRecords());

        } catch (Exception e) {
            log.error("❌ SSE 브로드캐스트 실패", e);
        }
    }

    /**
     * Heartbeat 전송 (30초마다)
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void sendHeartbeat() {
        try {
            int connections = sseService.getTotalConnections();
            if (connections > 0) {
                sseService.sendHeartbeat();
                log.debug("💓 Heartbeat 전송 완료 - 연결 수: {}", connections);
            }
        } catch (Exception e) {
            log.error("❌ Heartbeat 전송 실패", e);
        }
    }

    /**
     * 연결 상태 로깅 (1분마다)
     */
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    public void logConnectionStatus() {
        int connections = sseService.getTotalConnections();
        if (connections > 0) {
            log.info("📊 SSE 연결 상태 - 총 {} 연결", connections);
            sseService.getConnectionStatus().forEach((clientId, count) ->
                    log.info("   └─ {}: {} 연결", clientId, count)
            );
        }
    }
}