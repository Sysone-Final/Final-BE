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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/prometheus/metrics")
@RequiredArgsConstructor
public class PrometheusMetricsController {

    private final PrometheusMetricService prometheusMetricService;
    private final CpuMetricQueryService cpuMetricQueryService;
    private final MemoryMetricQueryService memoryMetricQueryService;
    private final NetworkMetricQueryService networkMetricQueryService;
    private final DiskMetricQueryService diskMetricQueryService;
    private final TemperatureMetricQueryService temperatureMetricQueryService;
    private final SSEBroadcastService sseBroadcastService;

    /**
     * SSE 실시간 스트리밍 연결
     * GET /api/prometheus/metrics/stream
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics(@RequestParam(required = false) String clientId) {
        String finalClientId = clientId != null ? clientId : UUID.randomUUID().toString();
        log.info("SSE 스트리밍 연결 요청 - clientId: {}", finalClientId);

        return sseBroadcastService.createEmitter(finalClientId);
    }

    /**
     * 전체 메트릭 조회 (HTTP)
     * GET /api/prometheus/metrics/all
     */
    @GetMapping("/all")
    public ResponseEntity<ServerRoomMetricsResponse> getAllMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        Instant start = startTime != null ? startTime : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = endTime != null ? endTime : Instant.now();

        log.info("전체 메트릭 조회 - startTime: {}, endTime: {}", start, end);

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

        Instant start = startTime != null ? startTime : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = endTime != null ? endTime : Instant.now();

        log.info("CPU 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        CpuMetricsResponse response = cpuMetricQueryService.getCpuMetrics(start, end);
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

        Instant start = startTime != null ? startTime : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = endTime != null ? endTime : Instant.now();

        log.info("메모리 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        MemoryMetricsResponse response = memoryMetricQueryService.getMemoryMetrics(start, end);
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

        Instant start = startTime != null ? startTime : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = endTime != null ? endTime : Instant.now();

        log.info("네트워크 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        NetworkMetricsResponse response = networkMetricQueryService.getNetworkMetrics(start, end);
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

        Instant start = startTime != null ? startTime : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = endTime != null ? endTime : Instant.now();

        log.info("디스크 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        DiskMetricsResponse response = diskMetricQueryService.getDiskMetrics(start, end);
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

        Instant start = startTime != null ? startTime : Instant.now().minus(1, ChronoUnit.HOURS);
        Instant end = endTime != null ? endTime : Instant.now();

        log.info("온도 메트릭 조회 - startTime: {}, endTime: {}", start, end);

        TemperatureMetricsResponse response = temperatureMetricQueryService.getTemperatureMetrics(start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * SSE 연결 상태 조회
     * GET /api/prometheus/metrics/stream/status
     */
    @GetMapping("/stream/status")
    public ResponseEntity<Map<String, Object>> getStreamStatus() {
        Map<String, Integer> connections = sseBroadcastService.getConnectionStatus();
        int totalConnections = sseBroadcastService.getTotalConnections();

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
        sseBroadcastService.closeClient(clientId);
        return ResponseEntity.noContent().build();
    }
}