package org.example.finalbe.domains.prometheus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.cpu.CpuMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.disk.DiskMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.memory.MemoryMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.network.NetworkMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.serverroom.ServerRoomMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.temperature.TemperatureMetricsResponse;
import org.example.finalbe.domains.prometheus.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/prometheus/metrics")
@RequiredArgsConstructor
public class PrometheusMetricsController {

    private final PrometheusMetricService prometheusMetricService;
    private final PrometheusRealtimeMetricService prometheusRealtimeMetricService;
    private final PrometheusCpuMetricQueryService prometheusCpuMetricQueryService;
    private final PrometheusMemoryMetricQueryService prometheusMemoryMetricQueryService;
    private final PrometheusNetworkMetricQueryService prometheusNetworkMetricQueryService;
    private final PrometheusDiskMetricQueryService prometheusDiskMetricQueryService;
    private final PrometheusTemperatureMetricQueryService prometheusTemperatureMetricQueryService;
    private final PrometheusSSEBroadcastService prometheusSseBroadcastService;

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * SSE 실시간 스트리밍 연결
     * GET /api/prometheus/metrics/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics(@RequestParam(required = false) String clientId) {
        String finalClientId = clientId != null ? clientId : UUID.randomUUID().toString();
        log.info("SSE 스트리밍 연결 요청 - clientId: {}", finalClientId);

        return prometheusSseBroadcastService.createEmitter(finalClientId);
    }

    /**
     * 전체 메트릭 조회 (HTTP) - range 파라미터 지원
     * GET /api/prometheus/metrics/all?range=5m
     * GET /api/prometheus/metrics/all?startTime=...&endTime=...
     */
    @GetMapping("/all")
    public ResponseEntity<ServerRoomMetricsResponse> getAllMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start;

        // range 파라미터 우선 (startTime보다 우선순위 높음)
        if (range != null) {
            start = parseRange(range, end);
            log.info("전체 메트릭 조회 (range) - range: {}, startTime: {}, endTime: {}", range, start, end);
        } else if (startTime != null) {
            start = startTime;
            log.info("전체 메트릭 조회 (시간 지정) - startTime: {}, endTime: {}", start, end);
        } else {
            // 기본값: 최근 5분
            start = end.minus(5, ChronoUnit.MINUTES);
            log.info("전체 메트릭 조회 (기본 5분) - startTime: {}, endTime: {}", start, end);
        }

        // 새 테이블에서 조회 (빠름!)
        ServerRoomMetricsResponse response = prometheusRealtimeMetricService.getMetricsByTimeRange(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * CPU 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/cpu?range=1h
     */
    @GetMapping("/cpu")
    public ResponseEntity<CpuMetricsResponse> getCpuMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("CPU 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        CpuMetricsResponse response = prometheusCpuMetricQueryService.getCpuMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 메모리 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/memory?range=1h
     */
    @GetMapping("/memory")
    public ResponseEntity<MemoryMetricsResponse> getMemoryMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("메모리 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        MemoryMetricsResponse response = prometheusMemoryMetricQueryService.getMemoryMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 네트워크 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/network?range=1h
     */
    @GetMapping("/network")
    public ResponseEntity<NetworkMetricsResponse> getNetworkMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("네트워크 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        NetworkMetricsResponse response = prometheusNetworkMetricQueryService.getNetworkMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 디스크 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/disk?range=1h
     */
    @GetMapping("/disk")
    public ResponseEntity<DiskMetricsResponse> getDiskMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("디스크 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        DiskMetricsResponse response = prometheusDiskMetricQueryService.getDiskMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 온도 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/temperature?range=1h
     */
    @GetMapping("/temperature")
    public ResponseEntity<TemperatureMetricsResponse> getTemperatureMetrics(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = (range != null) ? parseRange(range, end)
                : (startTime != null) ? startTime
                : end.minus(1, ChronoUnit.HOURS);

        log.info("온도 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        TemperatureMetricsResponse response = prometheusTemperatureMetricQueryService.getTemperatureMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * Range 파라미터 파싱
     *
     * 지원 형식:
     * - 5m, 15m, 30m (분)
     * - 1h, 3h, 6h, 12h, 24h (시간)
     * - 1d, 3d, 7d (일)
     * - 1w (주)
     */
    private Instant parseRange(String range, Instant end) {
        return switch(range.toLowerCase()) {
            case "5m" -> end.minus(5, ChronoUnit.MINUTES);
            case "15m" -> end.minus(15, ChronoUnit.MINUTES);
            case "30m" -> end.minus(30, ChronoUnit.MINUTES);
            case "1h" -> end.minus(1, ChronoUnit.HOURS);
            case "3h" -> end.minus(3, ChronoUnit.HOURS);
            case "6h" -> end.minus(6, ChronoUnit.HOURS);
            case "12h" -> end.minus(12, ChronoUnit.HOURS);
            case "24h", "1d" -> end.minus(24, ChronoUnit.HOURS);
            case "3d" -> end.minus(3, ChronoUnit.DAYS);
            case "7d", "1w" -> end.minus(7, ChronoUnit.DAYS);
            default -> {
                log.warn("알 수 없는 range 파라미터: {}, 기본값 5m 적용", range);
                yield end.minus(5, ChronoUnit.MINUTES);
            }
        };
    }
}