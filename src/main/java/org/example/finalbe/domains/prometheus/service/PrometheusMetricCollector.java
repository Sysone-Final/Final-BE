package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.client.PrometheusClient;
import org.example.finalbe.domains.prometheus.domain.*;
import org.example.finalbe.domains.prometheus.dto.PrometheusQueryResponse;
import org.example.finalbe.domains.prometheus.repository.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusMetricCollector {

    private final PrometheusClient prometheusClient;
    private final PrometheusCpuMetricRepository cpuMetricRepository;
    private final PrometheusMemoryMetricRepository memoryMetricRepository;
    private final PrometheusNetworkMetricRepository networkMetricRepository;
    private final PrometheusDiskMetricRepository diskMetricRepository;
    private final PrometheusTemperatureMetricRepository temperatureMetricRepository;


    private static final String STEP = "15s";

    /**
     * CPU 메트릭 수집 (병렬)
     */
    @Async("prometheusExecutor")
    @Transactional
    public CompletableFuture<Integer> collectCpuMetrics(Instant start, Instant end) {
        Instant collectStart = Instant.now();
        try {
            log.debug("CPU 메트릭 수집 시작: {} ~ {}", start, end);

            Map<String, Map<Long, PrometheusCpuMetric.PrometheusCpuMetricBuilder>> builderMap = new HashMap<>();

            // 1. CPU 사용률 (100 - idle)
            PrometheusQueryResponse cpuUsageResult = prometheusClient.queryRange(
                    "100 - (avg by(instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[1m])) * 100)",
                    start, end, STEP
            );
            processCpuUsage(cpuUsageResult, builderMap);

            // 2. CPU 모드별 사용률
            String[] modes = {"user", "system", "iowait", "idle", "nice", "irq", "softirq", "steal"};
            for (String mode : modes) {
                PrometheusQueryResponse modeResult = prometheusClient.queryRange(
                        String.format("avg by(instance) (rate(node_cpu_seconds_total{mode=\"%s\"}[1m])) * 100", mode),
                        start, end, STEP
                );
                processCpuMode(modeResult, mode, builderMap);
            }

            // 3. 시스템 부하
            PrometheusQueryResponse load1 = prometheusClient.queryRange("node_load1", start, end, STEP);
            PrometheusQueryResponse load5 = prometheusClient.queryRange("node_load5", start, end, STEP);
            PrometheusQueryResponse load15 = prometheusClient.queryRange("node_load15", start, end, STEP);
            processLoadAvg(load1, load5, load15, builderMap);

            // 4. 컨텍스트 스위치
            PrometheusQueryResponse contextSwitches = prometheusClient.queryRange(
                    "rate(node_context_switches_total[1m])",
                    start, end, STEP
            );
            processContextSwitches(contextSwitches, builderMap);

            // 5. 데이터 저장
            List<PrometheusCpuMetric> metrics = builderMap.values().stream()
                    .flatMap(map -> map.values().stream())
                    .map(PrometheusCpuMetric.PrometheusCpuMetricBuilder::build)
                    .collect(Collectors.toList());

            if (!metrics.isEmpty()) {
                cpuMetricRepository.saveAll(metrics);
                long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
                log.info("CPU 메트릭 저장 완료: {} rows ({}ms)", metrics.size(), duration);
                return CompletableFuture.completedFuture(metrics.size());
            }

            return CompletableFuture.completedFuture(0);

        } catch (Exception e) {
            long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
            log.error("CPU 메트릭 수집 실패 ({}ms)", duration, e);
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Memory 메트릭 수집 (병렬)
     */
    @Async("prometheusExecutor")
    @Transactional
    public CompletableFuture<Integer> collectMemoryMetrics(Instant start, Instant end) {
        Instant collectStart = Instant.now();
        try {
            log.debug("Memory 메트릭 수집 시작: {} ~ {}", start, end);

            Map<String, Map<Long, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder>> builderMap = new HashMap<>();

            // 1. 총 메모리
            PrometheusQueryResponse totalMemory = prometheusClient.queryRange(
                    "node_memory_MemTotal_bytes", start, end, STEP
            );
            processMemoryField(totalMemory, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::totalBytes);

            // 2. 사용 가능 메모리
            PrometheusQueryResponse availableMemory = prometheusClient.queryRange(
                    "node_memory_MemAvailable_bytes", start, end, STEP
            );
            processMemoryField(availableMemory, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::availableBytes);

            // 3. 여유 메모리
            PrometheusQueryResponse freeMemory = prometheusClient.queryRange(
                    "node_memory_MemFree_bytes", start, end, STEP
            );
            processMemoryField(freeMemory, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::freeBytes);

            // 4. 메모리 구성
            PrometheusQueryResponse buffers = prometheusClient.queryRange("node_memory_Buffers_bytes", start, end, STEP);
            processMemoryField(buffers, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::buffersBytes);

            PrometheusQueryResponse cached = prometheusClient.queryRange("node_memory_Cached_bytes", start, end, STEP);
            processMemoryField(cached, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::cachedBytes);

            PrometheusQueryResponse active = prometheusClient.queryRange("node_memory_Active_bytes", start, end, STEP);
            processMemoryField(active, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::activeBytes);

            PrometheusQueryResponse inactive = prometheusClient.queryRange("node_memory_Inactive_bytes", start, end, STEP);
            processMemoryField(inactive, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::inactiveBytes);

            // 5. 스왑 메모리
            PrometheusQueryResponse swapTotal = prometheusClient.queryRange("node_memory_SwapTotal_bytes", start, end, STEP);
            processMemoryField(swapTotal, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::swapTotalBytes);

            PrometheusQueryResponse swapFree = prometheusClient.queryRange("node_memory_SwapFree_bytes", start, end, STEP);
            processMemoryField(swapFree, builderMap, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::swapFreeBytes);

            // 6. 계산 필드 및 저장
            List<PrometheusMemoryMetric> metrics = builderMap.values().stream()
                    .flatMap(map -> map.values().stream())
                    .map(PrometheusMemoryMetric.PrometheusMemoryMetricBuilder::build)
                    .peek(this::calculateMemoryFields)
                    .collect(Collectors.toList());

            if (!metrics.isEmpty()) {
                memoryMetricRepository.saveAll(metrics);
                long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
                log.info("Memory 메트릭 저장 완료: {} rows ({}ms)", metrics.size(), duration);
                return CompletableFuture.completedFuture(metrics.size());
            }

            return CompletableFuture.completedFuture(0);

        } catch (Exception e) {
            long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
            log.error("Memory 메트릭 수집 실패 ({}ms)", duration, e);
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Network 메트릭 수집 (병렬)
     */
    @Async("prometheusExecutor")
    @Transactional
    public CompletableFuture<Integer> collectNetworkMetrics(Instant start, Instant end) {
        Instant collectStart = Instant.now();
        try {
            log.debug("Network 메트릭 수집 시작: {} ~ {}", start, end);

            Map<String, PrometheusNetworkMetric.PrometheusNetworkMetricBuilder> builderMap = new HashMap<>();

            // 1. 패킷 수 (누적)
            PrometheusQueryResponse rxPackets = prometheusClient.queryRange(
                    "node_network_receive_packets_total{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(rxPackets, builderMap, (b, v) -> b.rxPacketsTotal(v.longValue()));

            PrometheusQueryResponse txPackets = prometheusClient.queryRange(
                    "node_network_transmit_packets_total{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(txPackets, builderMap, (b, v) -> b.txPacketsTotal(v.longValue()));

            // 2. 바이트 수 (누적)
            PrometheusQueryResponse rxBytes = prometheusClient.queryRange(
                    "node_network_receive_bytes_total{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(rxBytes, builderMap, (b, v) -> b.rxBytesTotal(v.longValue()));

            PrometheusQueryResponse txBytes = prometheusClient.queryRange(
                    "node_network_transmit_bytes_total{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(txBytes, builderMap, (b, v) -> b.txBytesTotal(v.longValue()));

            // 3. 초당 전송률
            PrometheusQueryResponse rxBytesRate = prometheusClient.queryRange(
                    "rate(node_network_receive_bytes_total{device!~\"lo|veth.*\"}[1m])", start, end, STEP
            );
            processNetworkField(rxBytesRate, builderMap, PrometheusNetworkMetric.PrometheusNetworkMetricBuilder::rxBytesPerSec);

            PrometheusQueryResponse txBytesRate = prometheusClient.queryRange(
                    "rate(node_network_transmit_bytes_total{device!~\"lo|veth.*\"}[1m])", start, end, STEP
            );
            processNetworkField(txBytesRate, builderMap, PrometheusNetworkMetric.PrometheusNetworkMetricBuilder::txBytesPerSec);

            // 4. 초당 패킷 수
            PrometheusQueryResponse rxPacketsRate = prometheusClient.queryRange(
                    "rate(node_network_receive_packets_total{device!~\"lo|veth.*\"}[1m])", start, end, STEP
            );
            processNetworkField(rxPacketsRate, builderMap, PrometheusNetworkMetric.PrometheusNetworkMetricBuilder::rxPacketsPerSec);

            PrometheusQueryResponse txPacketsRate = prometheusClient.queryRange(
                    "rate(node_network_transmit_packets_total{device!~\"lo|veth.*\"}[1m])", start, end, STEP
            );
            processNetworkField(txPacketsRate, builderMap, PrometheusNetworkMetric.PrometheusNetworkMetricBuilder::txPacketsPerSec);

            // 5. 에러 및 드롭
            PrometheusQueryResponse rxErrors = prometheusClient.queryRange(
                    "node_network_receive_errs_total{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(rxErrors, builderMap, (b, v) -> b.rxErrorsTotal(v.longValue()));

            PrometheusQueryResponse txErrors = prometheusClient.queryRange(
                    "node_network_transmit_errs_total{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(txErrors, builderMap, (b, v) -> b.txErrorsTotal(v.longValue()));

            PrometheusQueryResponse rxDropped = prometheusClient.queryRange(
                    "node_network_receive_drop_total{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(rxDropped, builderMap, (b, v) -> b.rxDroppedTotal(v.longValue()));

            PrometheusQueryResponse txDropped = prometheusClient.queryRange(
                    "node_network_transmit_drop_total{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(txDropped, builderMap, (b, v) -> b.txDroppedTotal(v.longValue()));

            // 6. 인터페이스 상태
            PrometheusQueryResponse interfaceUp = prometheusClient.queryRange(
                    "node_network_up{device!~\"lo|veth.*\"}", start, end, STEP
            );
            processNetworkField(interfaceUp, builderMap, (b, v) -> b.interfaceUp(v == 1.0));

            // 7. 계산 필드 및 저장
            List<PrometheusNetworkMetric> metrics = builderMap.values().stream()
                    .map(PrometheusNetworkMetric.PrometheusNetworkMetricBuilder::build)
                    .peek(this::calculateNetworkFields)
                    .collect(Collectors.toList());

            if (!metrics.isEmpty()) {
                networkMetricRepository.saveAll(metrics);
                long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
                log.info("Network 메트릭 저장 완료: {} rows ({}ms)", metrics.size(), duration);
                return CompletableFuture.completedFuture(metrics.size());
            }

            return CompletableFuture.completedFuture(0);

        } catch (Exception e) {
            long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
            log.error("Network 메트릭 수집 실패 ({}ms)", duration, e);
            return CompletableFuture.completedFuture(0);
        }
    }

    /**
     * Disk 메트릭 수집 (병렬)
     */
    @Async("prometheusExecutor")
    @Transactional
    public CompletableFuture<Integer> collectDiskMetrics(Instant start, Instant end) {
        Instant collectStart = Instant.now();
        try {
            log.debug("Disk 메트릭 수집 시작: {} ~ {}", start, end);

            Map<String, PrometheusDiskMetric.PrometheusDiskMetricBuilder> builderMap = new HashMap<>();

            // 1. 디스크 용량 - mountpoint 필터 추가
            PrometheusQueryResponse totalBytes = prometheusClient.queryRange(
                    "node_filesystem_size_bytes{fstype!~\"tmpfs|fuse.*\",mountpoint!=\"\"}",
                    start, end, STEP
            );
            processDiskField(totalBytes, builderMap, (b, v) -> b.totalBytes(v.longValue()));

            PrometheusQueryResponse freeBytes = prometheusClient.queryRange(
                    "node_filesystem_free_bytes{fstype!~\"tmpfs|fuse.*\",mountpoint!=\"\"}",
                    start, end, STEP
            );
            processDiskField(freeBytes, builderMap, (b, v) -> b.freeBytes(v.longValue()));

            // 2. I/O 속도
            PrometheusQueryResponse readBytesRate = prometheusClient.queryRange(
                    "rate(node_disk_read_bytes_total[1m])", start, end, STEP
            );
            processDiskIoField(readBytesRate, builderMap,
                    PrometheusDiskMetric.PrometheusDiskMetricBuilder::readBytesPerSec);

            PrometheusQueryResponse writeBytesRate = prometheusClient.queryRange(
                    "rate(node_disk_written_bytes_total[1m])", start, end, STEP
            );
            processDiskIoField(writeBytesRate, builderMap,
                    PrometheusDiskMetric.PrometheusDiskMetricBuilder::writeBytesPerSec);

            // 3. IOPS
            PrometheusQueryResponse readIops = prometheusClient.queryRange(
                    "rate(node_disk_reads_completed_total[1m])", start, end, STEP
            );
            processDiskIoField(readIops, builderMap,
                    PrometheusDiskMetric.PrometheusDiskMetricBuilder::readIops);

            PrometheusQueryResponse writeIops = prometheusClient.queryRange(
                    "rate(node_disk_writes_completed_total[1m])", start, end, STEP
            );
            processDiskIoField(writeIops, builderMap,
                    PrometheusDiskMetric.PrometheusDiskMetricBuilder::writeIops);

            // 4. I/O 사용률
            PrometheusQueryResponse ioUtil = prometheusClient.queryRange(
                    "rate(node_disk_io_time_seconds_total[1m]) * 100", start, end, STEP
            );
            processDiskIoField(ioUtil, builderMap,
                    PrometheusDiskMetric.PrometheusDiskMetricBuilder::ioUtilizationPercent);

            PrometheusQueryResponse readTime = prometheusClient.queryRange(
                    "rate(node_disk_read_time_seconds_total[1m]) * 100", start, end, STEP
            );
            processDiskIoField(readTime, builderMap,
                    PrometheusDiskMetric.PrometheusDiskMetricBuilder::readTimePercent);

            PrometheusQueryResponse writeTime = prometheusClient.queryRange(
                    "rate(node_disk_write_time_seconds_total[1m]) * 100", start, end, STEP
            );
            processDiskIoField(writeTime, builderMap,
                    PrometheusDiskMetric.PrometheusDiskMetricBuilder::writeTimePercent);

            // 5. inode - mountpoint 필터 추가
            PrometheusQueryResponse totalInodes = prometheusClient.queryRange(
                    "node_filesystem_files{fstype!~\"tmpfs|fuse.*\",mountpoint!=\"\"}",
                    start, end, STEP
            );
            processDiskField(totalInodes, builderMap, (b, v) -> b.totalInodes(v.longValue()));

            PrometheusQueryResponse freeInodes = prometheusClient.queryRange(
                    "node_filesystem_files_free{fstype!~\"tmpfs|fuse.*\",mountpoint!=\"\"}",
                    start, end, STEP
            );
            processDiskField(freeInodes, builderMap, (b, v) -> b.freeInodes(v.longValue()));

            // 6. 계산 필드 및 저장
            List<PrometheusDiskMetric> metrics = builderMap.values().stream()
                    .map(PrometheusDiskMetric.PrometheusDiskMetricBuilder::build)
                    .peek(this::calculateDiskFields)
                    .filter(metric -> metric.getMountpoint() != null && !metric.getMountpoint().isEmpty())
                    .collect(Collectors.toList());

            if (!metrics.isEmpty()) {
                diskMetricRepository.saveAll(metrics);
                long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
                log.info("Disk 메트릭 저장 완료: {} rows ({}ms)", metrics.size(), duration);
                return CompletableFuture.completedFuture(metrics.size());
            }

            return CompletableFuture.completedFuture(0);

        } catch (Exception e) {
            long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
            log.error("❌ Disk 메트릭 수집 실패 ({}ms)", duration, e);
            return CompletableFuture.completedFuture(0);
        }
    }

    // ==================== Helper Methods ====================

    private void processCpuUsage(PrometheusQueryResponse response,
                                 Map<String, Map<Long, PrometheusCpuMetric.PrometheusCpuMetricBuilder>> builderMap) {
        for (PrometheusQueryResponse.Result result : response.results()) {
            String instance = result.getInstance();
            Long timestamp = result.timestamp();

            builderMap
                    .computeIfAbsent(instance, k -> new HashMap<>())
                    .computeIfAbsent(timestamp, k -> PrometheusCpuMetric.builder()
                            .time(Instant.ofEpochSecond(timestamp))
                            .instance(instance)
                            .createdAt(Instant.now()))
                    .cpuUsagePercent(result.value());
        }
    }

    private void processCpuMode(PrometheusQueryResponse response, String mode,
                                Map<String, Map<Long, PrometheusCpuMetric.PrometheusCpuMetricBuilder>> builderMap) {
        for (PrometheusQueryResponse.Result result : response.results()) {
            String instance = result.getInstance();
            Long timestamp = result.timestamp();

            PrometheusCpuMetric.PrometheusCpuMetricBuilder builder = builderMap
                    .computeIfAbsent(instance, k -> new HashMap<>())
                    .computeIfAbsent(timestamp, k -> PrometheusCpuMetric.builder()
                            .time(Instant.ofEpochSecond(timestamp))
                            .instance(instance)
                            .createdAt(Instant.now()));

            switch (mode) {
                case "user" -> builder.userPercent(result.value());
                case "system" -> builder.systemPercent(result.value());
                case "iowait" -> builder.iowaitPercent(result.value());
                case "idle" -> builder.idlePercent(result.value());
                case "nice" -> builder.nicePercent(result.value());
                case "irq" -> builder.irqPercent(result.value());
                case "softirq" -> builder.softirqPercent(result.value());
                case "steal" -> builder.stealPercent(result.value());
            }
        }
    }

    private void processLoadAvg(PrometheusQueryResponse load1, PrometheusQueryResponse load5, PrometheusQueryResponse load15,
                                Map<String, Map<Long, PrometheusCpuMetric.PrometheusCpuMetricBuilder>> builderMap) {
        if (load1 == null || load5 == null || load15 == null) {
            log.warn("Load average 데이터가 null입니다");
            return;
        }

        for (PrometheusQueryResponse.Result result : load1.results()) {
            Map<Long, PrometheusCpuMetric.PrometheusCpuMetricBuilder> instanceMap = builderMap.get(result.getInstance());
            if (instanceMap != null) {
                PrometheusCpuMetric.PrometheusCpuMetricBuilder builder = instanceMap.get(result.timestamp());
                if (builder != null) {
                    builder.loadAvg1(result.value());
                }
            }
        }

        for (PrometheusQueryResponse.Result result : load5.results()) {
            Map<Long, PrometheusCpuMetric.PrometheusCpuMetricBuilder> instanceMap = builderMap.get(result.getInstance());
            if (instanceMap != null) {
                PrometheusCpuMetric.PrometheusCpuMetricBuilder builder = instanceMap.get(result.timestamp());
                if (builder != null) {
                    builder.loadAvg5(result.value());
                }
            }
        }

        for (PrometheusQueryResponse.Result result : load15.results()) {
            Map<Long, PrometheusCpuMetric.PrometheusCpuMetricBuilder> instanceMap = builderMap.get(result.getInstance());
            if (instanceMap != null) {
                PrometheusCpuMetric.PrometheusCpuMetricBuilder builder = instanceMap.get(result.timestamp());
                if (builder != null) {
                    builder.loadAvg15(result.value());
                }
            }
        }
    }

    private void processContextSwitches(PrometheusQueryResponse response,
                                        Map<String, Map<Long, PrometheusCpuMetric.PrometheusCpuMetricBuilder>> builderMap) {
        for (PrometheusQueryResponse.Result result : response.results()) {
            var builder = builderMap.get(result.getInstance()).get(result.timestamp());
            if (builder != null) {
                builder.contextSwitchesPerSec(result.value());
            }
        }
    }

    private void processMemoryField(PrometheusQueryResponse response,
                                    Map<String, Map<Long, PrometheusMemoryMetric.PrometheusMemoryMetricBuilder>> builderMap,
                                    java.util.function.BiConsumer<PrometheusMemoryMetric.PrometheusMemoryMetricBuilder, Long> setter) {
        for (PrometheusQueryResponse.Result result : response.results()) {
            String instance = result.getInstance();
            Long timestamp = result.timestamp();

            PrometheusMemoryMetric.PrometheusMemoryMetricBuilder builder = builderMap
                    .computeIfAbsent(instance, k -> new HashMap<>())
                    .computeIfAbsent(timestamp, k -> PrometheusMemoryMetric.builder()
                            .time(Instant.ofEpochSecond(timestamp))
                            .instance(instance)
                            .createdAt(Instant.now()));

            setter.accept(builder, result.value().longValue());
        }
    }

    private void calculateMemoryFields(PrometheusMemoryMetric metric) {
        if (metric.getTotalBytes() != null && metric.getAvailableBytes() != null) {
            metric.setUsedBytes(metric.getTotalBytes() - metric.getAvailableBytes());
            metric.setUsagePercent((double) metric.getUsedBytes() / metric.getTotalBytes() * 100);
        }
        if (metric.getSwapTotalBytes() != null && metric.getSwapFreeBytes() != null) {
            metric.setSwapUsedBytes(metric.getSwapTotalBytes() - metric.getSwapFreeBytes());
            if (metric.getSwapTotalBytes() > 0) {
                metric.setSwapUsagePercent((double) metric.getSwapUsedBytes() / metric.getSwapTotalBytes() * 100);
            }
        }
    }

    private void processNetworkField(PrometheusQueryResponse response,
                                     Map<String, PrometheusNetworkMetric.PrometheusNetworkMetricBuilder> builderMap,
                                     java.util.function.BiConsumer<PrometheusNetworkMetric.PrometheusNetworkMetricBuilder, Double> setter) {
        for (PrometheusQueryResponse.Result result : response.results()) {
            String key = result.getInstance() + "_" + result.timestamp() + "_" + result.getDevice();

            PrometheusNetworkMetric.PrometheusNetworkMetricBuilder builder = builderMap.computeIfAbsent(key, k ->
                    PrometheusNetworkMetric.builder()
                            .time(Instant.ofEpochSecond(result.timestamp()))
                            .instance(result.getInstance())
                            .device(result.getDevice())
                            .createdAt(Instant.now())
            );

            setter.accept(builder, result.value());
        }
    }

    private void calculateNetworkFields(PrometheusNetworkMetric metric) {
        double interfaceSpeedBps = 10_000_000_000.0; // 10Gbps
        if (metric.getRxBytesPerSec() != null) {
            metric.setRxUsagePercent(metric.getRxBytesPerSec() * 8 / interfaceSpeedBps * 100);
        }
        if (metric.getTxBytesPerSec() != null) {
            metric.setTxUsagePercent(metric.getTxBytesPerSec() * 8 / interfaceSpeedBps * 100);
        }
        if (metric.getRxUsagePercent() != null && metric.getTxUsagePercent() != null) {
            metric.setTotalUsagePercent((metric.getRxUsagePercent() + metric.getTxUsagePercent()) / 2);
        }
    }

    /**
     * ✅ 새로 추가: Disk 용량/inode 필드 처리 (device 기준 키 사용)
     */
    private void processDiskField(PrometheusQueryResponse response,
                                  Map<String, PrometheusDiskMetric.PrometheusDiskMetricBuilder> builderMap,
                                  java.util.function.BiConsumer<PrometheusDiskMetric.PrometheusDiskMetricBuilder, Double> setter) {
        for (PrometheusQueryResponse.Result result : response.results()) {
            String device = result.getDevice();
            String mountpoint = result.getMountpoint();

            // ✅ 루트 파티션만 처리
            if (mountpoint == null || !mountpoint.equals("/")) {
                continue;
            }

            if (device == null) {
                continue;
            }

            // ✅ device를 primary key로 사용 (I/O 메트릭과 매칭 가능하도록)
            String key = result.getInstance() + "_" + result.timestamp() + "_" + device;

            PrometheusDiskMetric.PrometheusDiskMetricBuilder builder = builderMap.computeIfAbsent(key, k ->
                    PrometheusDiskMetric.builder()
                            .time(Instant.ofEpochSecond(result.timestamp()))
                            .instance(result.getInstance())
                            .device(device)
                            .mountpoint(mountpoint)
                            .createdAt(Instant.now())
            );

            setter.accept(builder, result.value());
        }
    }


    private void processDiskIoField(PrometheusQueryResponse response,
                                    Map<String, PrometheusDiskMetric.PrometheusDiskMetricBuilder> builderMap,
                                    java.util.function.BiConsumer<PrometheusDiskMetric.PrometheusDiskMetricBuilder, Double> setter) {
        for (PrometheusQueryResponse.Result result : response.results()) {
            String device = result.getDevice();
            if (device == null) continue;

            String key = result.getInstance() + "_" + result.timestamp() + "_" + device;

            PrometheusDiskMetric.PrometheusDiskMetricBuilder builder = builderMap.get(key);

            // 기존 builder가 없으면 새로 생성 (I/O만 있는 device의 경우)
            if (builder == null) {
                builder = builderMap.computeIfAbsent(key, k ->
                        PrometheusDiskMetric.builder()
                                .time(Instant.ofEpochSecond(result.timestamp()))
                                .instance(result.getInstance())
                                .device(device)
                                .mountpoint(null) // I/O 메트릭은 mountpoint 없음
                                .createdAt(Instant.now())
                );
            }

            setter.accept(builder, result.value());
        }
    }


    private void calculateDiskFields(PrometheusDiskMetric metric) {
        // 용량 계산
        if (metric.getTotalBytes() != null && metric.getFreeBytes() != null) {
            metric.setUsedBytes(metric.getTotalBytes() - metric.getFreeBytes());
            metric.setUsagePercent((double) metric.getUsedBytes() / metric.getTotalBytes() * 100);
        }

        // Total I/O
        if (metric.getReadBytesPerSec() != null && metric.getWriteBytesPerSec() != null) {
            metric.setTotalIoBytesPerSec(metric.getReadBytesPerSec() + metric.getWriteBytesPerSec());
        }

        // inode 계산
        if (metric.getTotalInodes() != null && metric.getFreeInodes() != null) {
            metric.setUsedInodes(metric.getTotalInodes() - metric.getFreeInodes());
            if (metric.getTotalInodes() > 0) {
                metric.setInodeUsagePercent((double) metric.getUsedInodes() / metric.getTotalInodes() * 100);
            }
        }
    }

    /**
     * Temperature 메트릭 수집 (병렬)
     */
    @Async("prometheusExecutor")
    @Transactional
    public CompletableFuture<Integer> collectTemperatureMetrics(Instant start, Instant end) {
        Instant collectStart = Instant.now();
        try {
            log.debug("🌡️ Temperature 메트릭 수집 시작: {} ~ {}", start, end);

            Map<String, PrometheusTemperatureMetric.PrometheusTemperatureMetricBuilder> builderMap = new HashMap<>();

            // 온도 메트릭 수집
            PrometheusQueryResponse tempResponse = prometheusClient.queryRange(
                    "node_hwmon_temp_celsius", start, end, STEP
            );

            for (PrometheusQueryResponse.Result result : tempResponse.results()) {
                String instance = result.getInstance();
                String chip = result.metric() != null ? result.metric().get("chip") : null;
                String sensor = result.metric() != null ? result.metric().get("sensor") : null;

                String key = instance + "_" + result.timestamp() + "_" +
                        (chip != null ? chip : "unknown") + "_" +
                        (sensor != null ? sensor : "unknown");

                PrometheusTemperatureMetric.PrometheusTemperatureMetricBuilder builder =
                        builderMap.computeIfAbsent(key, k ->
                                PrometheusTemperatureMetric.builder()
                                        .time(Instant.ofEpochSecond(result.timestamp()))
                                        .instance(instance)
                                        .chip(chip)
                                        .sensor(sensor)
                                        .createdAt(Instant.now())
                        );

                builder.tempCelsius(result.value());
            }

            // 데이터 저장
            List<PrometheusTemperatureMetric> metrics = builderMap.values().stream()
                    .map(PrometheusTemperatureMetric.PrometheusTemperatureMetricBuilder::build)
                    .collect(Collectors.toList());

            if (!metrics.isEmpty()) {
                temperatureMetricRepository.saveAll(metrics);
                long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
                log.info("✅ Temperature 메트릭 저장 완료: {} rows ({}ms)", metrics.size(), duration);
                return CompletableFuture.completedFuture(metrics.size());
            }

            return CompletableFuture.completedFuture(0);

        } catch (Exception e) {
            long duration = Instant.now().toEpochMilli() - collectStart.toEpochMilli();
            log.error("❌ Temperature 메트릭 수집 실패 ({}ms)", duration, e);
            return CompletableFuture.completedFuture(0);
        }
    }
}