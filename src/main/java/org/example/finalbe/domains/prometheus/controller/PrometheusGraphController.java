package org.example.finalbe.domains.prometheus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.service.PrometheusGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prometheus 그래프 API 컨트롤러
 * 대시보드 그래프 데이터 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/prometheus/graphs")
@RequiredArgsConstructor
public class PrometheusGraphController {

    private final PrometheusGraphService graphService;

    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("^(\\d+)(m|h|d)$");

    /**
     * CPU 섹션 전체 그래프 데이터
     * GET /api/prometheus/graphs/cpu?instance=server1:9100&timeRange=1h
     */
    @GetMapping("/cpu")
    public ResponseEntity<CpuSectionDto> getCpuSection(
            @RequestParam String instance,
            @RequestParam(defaultValue = "1h") String timeRange) {

        Instant end = Instant.now();
        Instant start = parseTimeRange(timeRange, end);

        log.info("CPU 섹션 조회 - instance: {}, timeRange: {} ({}~{})",
                instance, timeRange, start, end);

        CpuSectionDto response = graphService.getCpuSection(instance, start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 메모리 섹션 전체 그래프 데이터
     * GET /api/prometheus/graphs/memory?instance=server1:9100&timeRange=1h
     */
    @GetMapping("/memory")
    public ResponseEntity<MemorySectionDto> getMemorySection(
            @RequestParam String instance,
            @RequestParam(defaultValue = "1h") String timeRange) {

        Instant end = Instant.now();
        Instant start = parseTimeRange(timeRange, end);

        log.info("메모리 섹션 조회 - instance: {}, timeRange: {} ({}~{})",
                instance, timeRange, start, end);

        MemorySectionDto response = graphService.getMemorySection(instance, start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 네트워크 섹션 전체 그래프 데이터
     * GET /api/prometheus/graphs/network?instance=server1:9100&timeRange=1h
     */
    @GetMapping("/network")
    public ResponseEntity<NetworkSectionDto> getNetworkSection(
            @RequestParam String instance,
            @RequestParam(defaultValue = "1h") String timeRange) {

        Instant end = Instant.now();
        Instant start = parseTimeRange(timeRange, end);

        log.info("네트워크 섹션 조회 - instance: {}, timeRange: {} ({}~{})",
                instance, timeRange, start, end);

        NetworkSectionDto response = graphService.getNetworkSection(instance, start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 디스크 섹션 전체 그래프 데이터
     * GET /api/prometheus/graphs/disk?instance=server1:9100&timeRange=1h
     */
    @GetMapping("/disk")
    public ResponseEntity<DiskSectionDto> getDiskSection(
            @RequestParam String instance,
            @RequestParam(defaultValue = "1h") String timeRange) {

        Instant end = Instant.now();
        Instant start = parseTimeRange(timeRange, end);

        log.info("디스크 섹션 조회 - instance: {}, timeRange: {} ({}~{})",
                instance, timeRange, start, end);

        DiskSectionDto response = graphService.getDiskSection(instance, start, end);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 대시보드 데이터 (모든 섹션)
     * GET /api/prometheus/graphs/dashboard?instance=server1:9100&timeRange=1h
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDto> getDashboard(
            @RequestParam String instance,
            @RequestParam(defaultValue = "1h") String timeRange) {

        Instant end = Instant.now();
        Instant start = parseTimeRange(timeRange, end);

        log.info("전체 대시보드 조회 - instance: {}, timeRange: {} ({}~{})",
                instance, timeRange, start, end);

        DashboardDto dashboard = DashboardDto.builder()
                .cpu(graphService.getCpuSection(instance, start, end))
                .memory(graphService.getMemorySection(instance, start, end))
                .network(graphService.getNetworkSection(instance, start, end))
                .disk(graphService.getDiskSection(instance, start, end))
                .build();

        return ResponseEntity.ok(dashboard);
    }

    /**
     * 시간 범위 파싱 (예: 1h, 30m, 7d)
     */
    private Instant parseTimeRange(String timeRange, Instant end) {
        Matcher matcher = TIME_RANGE_PATTERN.matcher(timeRange);

        if (!matcher.matches()) {
            log.warn("잘못된 시간 범위 형식: {}, 기본값 1시간 사용", timeRange);
            return end.minus(1, ChronoUnit.HOURS);
        }

        int value = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);

        return switch (unit) {
            case "m" -> end.minus(value, ChronoUnit.MINUTES);
            case "h" -> end.minus(value, ChronoUnit.HOURS);
            case "d" -> end.minus(value, ChronoUnit.DAYS);
            default -> end.minus(1, ChronoUnit.HOURS);
        };
    }
}