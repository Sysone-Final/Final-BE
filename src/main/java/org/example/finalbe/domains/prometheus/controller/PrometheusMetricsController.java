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
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/prometheus/metrics")
@RequiredArgsConstructor
public class PrometheusMetricsController {

    private final PrometheusMetricService prometheusMetricService;
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
     * 전체 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/all
     */
    @GetMapping("/all")
    public ResponseEntity<ServerRoomMetricsResponse> getAllMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        ZonedDateTime nowKst = ZonedDateTime.now(KST_ZONE);

        Instant start = startTime != null
                ? startTime
                : nowKst.minus(1, ChronoUnit.HOURS).toInstant();

        Instant end = endTime != null
                ? endTime
                : nowKst.toInstant();

        // KST로만 로그 출력 (UTC는 제거)
        ZonedDateTime startKst = start.atZone(KST_ZONE);
        ZonedDateTime endKst = end.atZone(KST_ZONE);

        log.info("전체 메트릭 조회 (KST) - startTime: {}, endTime: {}", startKst, endKst);

        ServerRoomMetricsResponse response = prometheusMetricService.getAllMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * CPU 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/cpu
     */
    @GetMapping("/cpu")
    public ResponseEntity<CpuMetricsResponse> getCpuMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        ZonedDateTime nowKst = ZonedDateTime.now(KST_ZONE);

        Instant start = startTime != null
                ? startTime
                : nowKst.minus(1, ChronoUnit.HOURS).toInstant();

        Instant end = endTime != null
                ? endTime
                : nowKst.toInstant();

        log.info("CPU 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        CpuMetricsResponse response = prometheusCpuMetricQueryService.getCpuMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 메모리 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/memory
     */
    @GetMapping("/memory")
    public ResponseEntity<MemoryMetricsResponse> getMemoryMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        ZonedDateTime nowKst = ZonedDateTime.now(KST_ZONE);

        Instant start = startTime != null
                ? startTime
                : nowKst.minus(1, ChronoUnit.HOURS).toInstant();

        Instant end = endTime != null
                ? endTime
                : nowKst.toInstant();

        log.info("메모리 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        MemoryMetricsResponse response = prometheusMemoryMetricQueryService.getMemoryMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 네트워크 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/network
     */
    @GetMapping("/network")
    public ResponseEntity<NetworkMetricsResponse> getNetworkMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        ZonedDateTime nowKst = ZonedDateTime.now(KST_ZONE);

        Instant start = startTime != null
                ? startTime
                : nowKst.minus(1, ChronoUnit.HOURS).toInstant();

        Instant end = endTime != null
                ? endTime
                : nowKst.toInstant();

        log.info("네트워크 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        NetworkMetricsResponse response = prometheusNetworkMetricQueryService.getNetworkMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 디스크 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/disk
     */
    @GetMapping("/disk")
    public ResponseEntity<DiskMetricsResponse> getDiskMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        ZonedDateTime nowKst = ZonedDateTime.now(KST_ZONE);

        Instant start = startTime != null
                ? startTime
                : nowKst.minus(1, ChronoUnit.HOURS).toInstant();

        Instant end = endTime != null
                ? endTime
                : nowKst.toInstant();

        log.info("디스크 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        DiskMetricsResponse response = prometheusDiskMetricQueryService.getDiskMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 온도 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/temperature
     */
    @GetMapping("/temperature")
    public ResponseEntity<TemperatureMetricsResponse> getTemperatureMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        ZonedDateTime nowKst = ZonedDateTime.now(KST_ZONE);

        Instant start = startTime != null
                ? startTime
                : nowKst.minus(1, ChronoUnit.HOURS).toInstant();

        Instant end = endTime != null
                ? endTime
                : nowKst.toInstant();

        log.info("온도 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        TemperatureMetricsResponse response = prometheusTemperatureMetricQueryService.getTemperatureMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * SSE 연결 상태 조회
     * GET /api/prometheus/metrics/stream/status
     */
    @GetMapping("/stream/status")
    public ResponseEntity<Map<String, Object>> getStreamStatus() {
        Map<String, Integer> connections = prometheusSseBroadcastService.getConnectionStatus();
        int totalConnections = prometheusSseBroadcastService.getTotalConnections();

        return ResponseEntity.ok(Map.of(
                "totalConnections", totalConnections,
                "connections", connections
        ));
    }

    /**
     * SSE 연결 종료
     * DELETE /api/prometheus/metrics/stream/{clientId}
     */
    @DeleteMapping("/stream/{clientId}")
    public ResponseEntity<Void> closeStream(@PathVariable String clientId) {
        log.info("SSE 연결 종료 요청 - clientId: {}", clientId);
        prometheusSseBroadcastService.closeClient(clientId);
        return ResponseEntity.noContent().build();
    }
}