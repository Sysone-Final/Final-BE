package org.example.finalbe.domains.prometheus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.*;
import org.example.finalbe.domains.prometheus.service.PrometheusMetricQueryService;
import org.example.finalbe.domains.prometheus.service.PrometheusSSEService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/prometheus/metrics")
@RequiredArgsConstructor
public class PrometheusMetricsController {

    private final PrometheusMetricQueryService queryService;
    private final PrometheusSSEService sseService;

    /**
     * SSE 실시간 스트리밍 연결
     * GET /api/prometheus/metrics/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics(@RequestParam(required = false) String clientId) {
        String finalClientId = clientId != null ? clientId : UUID.randomUUID().toString();
        log.info("📡 SSE 스트리밍 연결 요청 - clientId: {}", finalClientId);

        return sseService.createEmitter(finalClientId);
    }

    /**
     * 전체 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/all?range=15s
     */
    @GetMapping("/all")
    public ResponseEntity<MetricsApiResponse> getAllMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start;

        if (range != null) {
            start = parseRange(range, end);
            log.info("전체 메트릭 조회 (range) - range: {}, startTime: {}, endTime: {}", range, start, end);
        } else if (startTime != null) {
            start = startTime;
            log.info("전체 메트릭 조회 (시간 지정) - startTime: {}, endTime: {}", start, end);
        } else {
            start = end.minus(15, ChronoUnit.SECONDS);
            log.info("전체 메트릭 조회 (기본 15초) - startTime: {}, endTime: {}", start, end);
        }

        MetricsApiResponse response = queryService.getMetricsByTimeRange(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 최신 메트릭 조회 (현재 상태)
     * GET /api/prometheus/metrics/latest
     */
    @GetMapping("/latest")
    public ResponseEntity<MetricsResponse> getLatestMetrics() {
        log.info("최신 메트릭 조회");
        MetricsResponse response = queryService.getLatestMetrics();
        return ResponseEntity.ok(response);
    }

    /**
     * CPU 메트릭 조회
     * GET /api/prometheus/metrics/cpu?range=1h
     */
    @GetMapping("/cpu")
    public ResponseEntity<List<CpuMetricResponse>> getCpuMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("CPU 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        List<CpuMetricResponse> response = queryService.getCpuMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * Memory 메트릭 조회
     * GET /api/prometheus/metrics/memory?range=1h
     */
    @GetMapping("/memory")
    public ResponseEntity<List<MemoryMetricResponse>> getMemoryMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("Memory 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        List<MemoryMetricResponse> response = queryService.getMemoryMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * Network 메트릭 조회
     * GET /api/prometheus/metrics/network?range=1h
     */
    @GetMapping("/network")
    public ResponseEntity<List<NetworkMetricResponse>> getNetworkMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("Network 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        List<NetworkMetricResponse> response = queryService.getNetworkMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * Disk 메트릭 조회
     * GET /api/prometheus/metrics/disk?range=1h
     */
    @GetMapping("/disk")
    public ResponseEntity<List<DiskMetricResponse>> getDiskMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("Disk 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        List<DiskMetricResponse> response = queryService.getDiskMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * SSE 연결 상태 조회
     * GET /api/prometheus/metrics/sse/status
     */
    @GetMapping("/sse/status")
    public ResponseEntity<Map<String, Object>> getSseStatus() {
        Map<String, Integer> connections = sseService.getConnectionStatus();
        int total = sseService.getTotalConnections();

        return ResponseEntity.ok(Map.of(
                "totalConnections", total,
                "clients", connections
        ));
    }

    /**
     * Range 파라미터 파싱
     *
     * 지원 형식:
     * - 15s, 30s (초)
     * - 5m, 15m, 30m (분)
     * - 1h, 3h, 6h, 12h, 24h (시간)
     * - 1d, 3d, 7d (일)
     */
    private Instant parseRange(String range, Instant end) {
        return switch (range.toLowerCase()) {
            case "15s" -> end.minus(15, ChronoUnit.SECONDS);
            case "30s" -> end.minus(30, ChronoUnit.SECONDS);
            case "5m" -> end.minus(5, ChronoUnit.MINUTES);
            case "15m" -> end.minus(15, ChronoUnit.MINUTES);
            case "30m" -> end.minus(30, ChronoUnit.MINUTES);
            case "1h" -> end.minus(1, ChronoUnit.HOURS);
            case "3h" -> end.minus(3, ChronoUnit.HOURS);
            case "6h" -> end.minus(6, ChronoUnit.HOURS);
            case "12h" -> end.minus(12, ChronoUnit.HOURS);
            case "24h", "1d" -> end.minus(24, ChronoUnit.HOURS);
            case "3d" -> end.minus(3, ChronoUnit.DAYS);
            case "7d" -> end.minus(7, ChronoUnit.DAYS);
            default -> {
                log.warn("알 수 없는 range 파라미터: {}, 기본값 15s 적용", range);
                yield end.minus(15, ChronoUnit.SECONDS);
            }
        };
    }
}
