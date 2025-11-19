package org.example.finalbe.domains.prometheus.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.prometheus.dto.*;
import org.example.finalbe.domains.prometheus.service.PrometheusMetricCollector;
import org.example.finalbe.domains.prometheus.service.PrometheusMetricQueryService;
import org.example.finalbe.domains.prometheus.service.PrometheusSSEService;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusMetricScheduler {

    private final PrometheusMetricCollector collector;
    private final PrometheusMetricQueryService queryService;
    private final PrometheusSSEService sseService;
    private final EquipmentRepository equipmentRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 15초마다 메트릭 수집 및 SSE 브로드캐스트
     */
    @Scheduled(fixedRate = 15000, initialDelay = 5000)
    public void collectAndBroadcastMetrics() {
        Instant collectionStart = Instant.now();
        LocalDateTime collectionStartTime = LocalDateTime.ofInstant(collectionStart, ZoneId.systemDefault());

        log.info("=========================================");
        log.info("Prometheus 메트릭 수집 시작: {}", FORMATTER.format(collectionStartTime));

        Instant end = Instant.now();
        Instant start = end.minus(15, ChronoUnit.SECONDS);

        List<CollectionResultResponse> results = new ArrayList<>();

        try {
            // 병렬 수집
            CompletableFuture<Integer> cpuFuture = collector.collectCpuMetrics(start, end);
            CompletableFuture<Integer> memoryFuture = collector.collectMemoryMetrics(start, end);
            CompletableFuture<Integer> networkFuture = collector.collectNetworkMetrics(start, end);
            CompletableFuture<Integer> diskFuture = collector.collectDiskMetrics(start, end);
            CompletableFuture<Integer> temperatureFuture = collector.collectTemperatureMetrics(start, end);

            CompletableFuture.allOf(cpuFuture, memoryFuture, networkFuture, diskFuture, temperatureFuture).join();

            Instant collectEnd = Instant.now();
            results.add(CollectionResultResponse.success("CPU", start, collectEnd, cpuFuture.get()));
            results.add(CollectionResultResponse.success("Memory", start, collectEnd, memoryFuture.get()));
            results.add(CollectionResultResponse.success("Network", start, collectEnd, networkFuture.get()));
            results.add(CollectionResultResponse.success("Disk", start, collectEnd, diskFuture.get()));
            results.add(CollectionResultResponse.success("Temperature", start, collectEnd, temperatureFuture.get()));

            CollectionSummaryResponse summary = CollectionSummaryResponse.of(collectionStart, results);
            log.info("수집 완료 - 총 {} rows, 성공: {}, 실패: {}, 소요시간: {}ms",
                    summary.totalRecords(), summary.successCount(), summary.failureCount(), summary.totalDuration());

            // SSE 브로드캐스트 (장비별만)
            broadcastEquipmentMetrics();

        } catch (Exception e) {
            log.error("메트릭 수집 중 오류 발생", e);
        } finally {
            long totalDuration = Instant.now().toEpochMilli() - collectionStart.toEpochMilli();
            log.info("전체 실행 시간: {}ms", totalDuration);
            log.info("=========================================\n");
        }
    }

    /**
     * 장비별 메트릭 브로드캐스트 (SSEService가 자동으로 집계 처리)
     */
    private void broadcastEquipmentMetrics() {
        int connections = sseService.getTotalConnections();
        if (connections == 0) {
            log.debug("활성 SSE 연결 없음 - 브로드캐스트 스킵");
            return;
        }

        log.debug("SSE 브로드캐스트 시작 - 연결 수: {}", connections);

        List<Equipment> equipments = equipmentRepository.findByDelYn(DelYN.N);

        for (Equipment equipment : equipments) {
            try {
                EquipmentMetricsResponse metrics = queryService.getLatestMetricsByEquipment(equipment.getId());
                // SSEService가 equipmentId를 보고 자동으로 집계 대상 구독자에게도 전달
                sseService.broadcastEquipmentMetrics(equipment.getId(), metrics);
            } catch (Exception e) {
                log.error("장비 {} 메트릭 브로드캐스트 실패", equipment.getId(), e);
            }
        }

        log.debug("SSE 브로드캐스트 완료");
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
                log.debug("Heartbeat 전송 완료 - 연결 수: {}", connections);
            }
        } catch (Exception e) {
            log.error("Heartbeat 전송 실패", e);
        }
    }

    /**
     * 연결 상태 로깅 (1분마다)
     */
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    public void logConnectionStatus() {
        int connections = sseService.getTotalConnections();
        if (connections > 0) {
            log.info("SSE 연결 상태 - 총 {} 연결", connections);
            Map<String, Object> status = sseService.getConnectionStatus();
            log.info("상세: {}", status);
        }
    }
}