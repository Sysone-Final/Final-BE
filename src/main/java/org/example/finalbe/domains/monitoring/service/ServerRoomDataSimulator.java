package org.example.finalbe.domains.monitoring.service;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerRoomDataSimulator {

    private final SystemMetricRepository systemMetricRepository;
    private final DiskMetricRepository diskMetricRepository;
    private final NetworkMetricRepository networkMetricRepository;
    private final EnvironmentMetricRepository environmentMetricRepository;

    private static final int[] DEVICE_IDS = {1, 3, 4, 6, 7, 8, 9, 10, 11};
    private static final Map<Integer, List<String>> DEVICE_PARTITIONS = new HashMap<>();
    private static final Map<Integer, List<String>> DEVICE_NICS = new HashMap<>();

    private final Map<Integer, AnomalyState> anomalyStates = new HashMap<>();
    private final Random random = new Random();

    // 누적 카운터
    private final Map<String, Long> cumulativeInPackets = new HashMap<>();
    private final Map<String, Long> cumulativeOutPackets = new HashMap<>();
    private final Map<String, Long> cumulativeInBytes = new HashMap<>();
    private final Map<String, Long> cumulativeOutBytes = new HashMap<>();
    private final Map<String, Long> cumulativeInErrors = new HashMap<>();
    private final Map<String, Long> cumulativeOutErrors = new HashMap<>();
    private final Map<String, Long> cumulativeInDiscards = new HashMap<>();
    private final Map<String, Long> cumulativeOutDiscards = new HashMap<>();
    private final Map<String, Long> cumulativeContextSwitches = new HashMap<>();
    private final Map<String, Long> cumulativeIoReads = new HashMap<>();
    private final Map<String, Long> cumulativeIoWrites = new HashMap<>();

    // 환경 메트릭 추적용 (최저/최고값 계산)
    private final Map<Integer, Double> minTemperatureTracker = new HashMap<>();
    private final Map<Integer, Double> maxTemperatureTracker = new HashMap<>();
    private final Map<Integer, Double> minHumidityTracker = new HashMap<>();
    private final Map<Integer, Double> maxHumidityTracker = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("🚀 서버실 데이터 시뮬레이터 초기화 시작...");

        // 파티션 구성
        DEVICE_PARTITIONS.put(1, Arrays.asList("/", "/boot", "/home"));
        DEVICE_PARTITIONS.put(3, Arrays.asList("C:", "D:"));
        DEVICE_PARTITIONS.put(4, Arrays.asList("C:", "D:", "E:"));
        DEVICE_PARTITIONS.put(6, Arrays.asList("/", "/var"));
        DEVICE_PARTITIONS.put(7, Arrays.asList("C:"));
        DEVICE_PARTITIONS.put(8, Arrays.asList("/", "/boot"));
        DEVICE_PARTITIONS.put(9, Arrays.asList("C:", "D:"));
        DEVICE_PARTITIONS.put(10, Arrays.asList("/"));
        DEVICE_PARTITIONS.put(11, Arrays.asList("C:", "F:"));

        // NIC 구성
        DEVICE_NICS.put(1, Arrays.asList("eth0", "eth1"));
        DEVICE_NICS.put(3, Arrays.asList("GigabitEthernet1/0/1", "GigabitEthernet1/0/2"));
        DEVICE_NICS.put(4, Arrays.asList("GigabitEthernet1/0/1", "GigabitEthernet1/0/2", "GigabitEthernet1/0/3"));
        DEVICE_NICS.put(6, Arrays.asList("eth0"));
        DEVICE_NICS.put(7, Arrays.asList("Ethernet0", "Ethernet1"));
        DEVICE_NICS.put(8, Arrays.asList("eth0", "eth1", "eth2"));
        DEVICE_NICS.put(9, Arrays.asList("Ethernet0"));
        DEVICE_NICS.put(10, Arrays.asList("enp0s3", "enp0s8"));
        DEVICE_NICS.put(11, Arrays.asList("eth0"));

        for (int deviceId : DEVICE_IDS) {
            anomalyStates.put(deviceId, new AnomalyState());
            // 환경 메트릭 초기값 설정
            minTemperatureTracker.put(deviceId, 22.0);
            maxTemperatureTracker.put(deviceId, 22.0);
            minHumidityTracker.put(deviceId, 45.0);
            maxHumidityTracker.put(deviceId, 45.0);
        }

        log.info("✅ 초기화 완료! {}개 서버 모니터링 시작 (온도/습도 포함)", DEVICE_IDS.length);
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 2000)
    @Transactional
    public void generateRealtimeMetrics() {
        LocalDateTime now = LocalDateTime.now();

        try {
            for (int deviceId : DEVICE_IDS) {
                SystemMetric sysMetric = generateSystemMetric(deviceId, now);
                systemMetricRepository.save(sysMetric);

                List<String> partitions = DEVICE_PARTITIONS.get(deviceId);
                for (String partition : partitions) {
                    DiskMetric diskMetric = generateDiskMetric(deviceId, partition, now);
                    diskMetricRepository.save(diskMetric);
                }

                List<String> nics = DEVICE_NICS.get(deviceId);
                for (String nic : nics) {
                    NetworkMetric nicMetric = generateNetworkMetric(deviceId, nic, now);
                    networkMetricRepository.save(nicMetric);
                }

                // 환경 메트릭 생성 및 저장
                EnvironmentMetric envMetric = generateEnvironmentMetric(deviceId, now);
                environmentMetricRepository.save(envMetric);
            }

            maybeUpdateAnomalies();

        } catch (Exception e) {
            log.error("❌ 메트릭 생성 중 오류 발생", e);
        }
    }

    /**
     * 시스템 메트릭 생성 - 모든 그래프 지원
     */
    private SystemMetric generateSystemMetric(int deviceId, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(deviceId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        SystemMetric metric = SystemMetric.builder()
                .deviceId(deviceId)
                .generateTime(time)
                .build();

        // ===== CPU 메트릭 (그래프 1.1, 1.2) =====
        double baseCpu = 15 + rand.nextDouble() * 20;
        double cpuUsage = state.hasCpuAnomaly ?
                Math.min(95, baseCpu + 50 + rand.nextDouble() * 20) : baseCpu;

        metric.setCpuIdle(100 - cpuUsage);
        metric.setCpuUser(cpuUsage * 0.55);
        metric.setCpuSystem(cpuUsage * 0.20);
        metric.setCpuWait(cpuUsage * 0.10);
        metric.setCpuNice(cpuUsage * 0.02);
        metric.setCpuIrq(cpuUsage * 0.05);
        metric.setCpuSoftirq(cpuUsage * 0.05);
        metric.setCpuSteal(cpuUsage * 0.03);

        // ===== 시스템 부하 (그래프 1.3) =====
        double baseLoad = cpuUsage / 25.0;
        metric.setLoadAvg1(baseLoad + rand.nextDouble() * 0.5);
        metric.setLoadAvg5(baseLoad * 0.9 + rand.nextDouble() * 0.3);
        metric.setLoadAvg15(baseLoad * 0.8 + rand.nextDouble() * 0.2);

        // ===== 컨텍스트 스위치 (그래프 1.4) =====
        String contextKey = "context_" + deviceId;
        long prevContext = cumulativeContextSwitches.getOrDefault(contextKey, 0L);
        long contextInc = (long)(cpuUsage * 100 + rand.nextInt(5000));
        long newContext = prevContext + contextInc;
        cumulativeContextSwitches.put(contextKey, newContext);
        metric.setContextSwitches(newContext);

        // ===== 메모리 메트릭 (그래프 2.1, 2.2) =====
        long totalMemory = 16L * 1024 * 1024 * 1024;  // 16GB
        double baseMemUsage = 40 + rand.nextDouble() * 20;
        double memUsagePercent = state.hasMemoryAnomaly ?
                Math.min(95, baseMemUsage + 30 + rand.nextDouble() * 15) : baseMemUsage;

        long usedMemory = (long)(totalMemory * memUsagePercent / 100);
        long freeMemory = totalMemory - usedMemory;

        metric.setTotalMemory(totalMemory);
        metric.setUsedMemory(usedMemory);
        metric.setFreeMemory(freeMemory);
        metric.setUsedMemoryPercentage(memUsagePercent);

        // 메모리 구성 상세
        metric.setMemoryActive(usedMemory / 2);
        metric.setMemoryInactive(usedMemory / 4);
        metric.setMemoryBuffers(usedMemory / 10);
        metric.setMemoryCached(usedMemory / 5);

        // ===== SWAP 메트릭 (그래프 2.3) =====
        long totalSwap = 8L * 1024 * 1024 * 1024;  // 8GB
        double swapUsagePercent = memUsagePercent > 85 ?
                rand.nextDouble() * 50 : rand.nextDouble() * 5;

        long usedSwap = (long)(totalSwap * swapUsagePercent / 100);

        metric.setTotalSwap(totalSwap);
        metric.setUsedSwap(usedSwap);
        metric.setUsedSwapPercentage(swapUsagePercent);

        return metric;
    }

    /**
     * 디스크 메트릭 생성 - 모든 그래프 지원
     */
    private DiskMetric generateDiskMetric(int deviceId, String partition, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(deviceId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        DiskMetric metric = DiskMetric.builder()
                .deviceId(deviceId)
                .partitionPath(partition)
                .generateTime(time)
                .build();

        // ===== 디스크 용량 (그래프 4.1, 4.5) =====
        long totalBytes = 500L * 1024 * 1024 * 1024;  // 500GB
        double baseUsage = 30 + rand.nextDouble() * 40;
        double usedPercent = Math.min(95, baseUsage);

        long usedBytes = (long)(totalBytes * usedPercent / 100);
        long freeBytes = totalBytes - usedBytes;

        metric.setTotalBytes(totalBytes);
        metric.setUsedBytes(usedBytes);
        metric.setFreeBytes(freeBytes);
        metric.setUsedPercentage(usedPercent);

        // ===== 디스크 I/O (그래프 4.2, 4.3, 4.4) =====
        double baseReadBps = 5_000_000 + rand.nextDouble() * 10_000_000;  // 5~15 MB/s
        double baseWriteBps = 3_000_000 + rand.nextDouble() * 7_000_000;  // 3~10 MB/s

        double ioReadBps = state.hasDiskAnomaly ?
                baseReadBps * (2 + rand.nextDouble() * 3) : baseReadBps;

        double ioWriteBps = state.hasDiskAnomaly ?
                baseWriteBps * (2 + rand.nextDouble() * 3) : baseWriteBps;

        metric.setIoReadBps(ioReadBps);
        metric.setIoWriteBps(ioWriteBps);

        // I/O 사용률
        double ioTimePercentage = state.hasDiskAnomaly ?
                Math.min(95, 30 + rand.nextDouble() * 50) : 5 + rand.nextDouble() * 20;

        metric.setIoTimePercentage(ioTimePercentage);

        // 누적 I/O 카운터
        String key = deviceId + "_" + partition;

        long prevReadCount = cumulativeIoReads.getOrDefault(key, 0L);
        long prevWriteCount = cumulativeIoWrites.getOrDefault(key, 0L);

        long readInc = (long)(ioReadBps / 4096 * 5);  // 5초간 읽기 횟수
        long writeInc = (long)(ioWriteBps / 4096 * 5);

        long newReadCount = prevReadCount + readInc;
        long newWriteCount = prevWriteCount + writeInc;

        cumulativeIoReads.put(key, newReadCount);
        cumulativeIoWrites.put(key, newWriteCount);

        metric.setIoReadCount(newReadCount);
        metric.setIoWriteCount(newWriteCount);

        // ===== inode (그래프 4.6) =====
        long totalInodes = 32_000_000L;
        double inodeUsagePercent = 15 + rand.nextDouble() * 30;

        long usedInodes = (long)(totalInodes * inodeUsagePercent / 100);
        long freeInodes = totalInodes - usedInodes;

        metric.setTotalInodes(totalInodes);
        metric.setUsedInodes(usedInodes);
        metric.setFreeInodes(freeInodes);
        metric.setUsedInodePercentage(inodeUsagePercent);

        return metric;
    }

    /**
     * 네트워크 메트릭 생성 - 모든 그래프 지원
     */
    private NetworkMetric generateNetworkMetric(int deviceId, String nicName, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(deviceId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        NetworkMetric metric = NetworkMetric.builder()
                .deviceId(deviceId)
                .nicName(nicName)
                .generateTime(time)
                .build();

        double bandwidthBps = 1_000_000_000.0;  // 1Gbps

        // ===== RX/TX 사용률 (그래프 3.1, 3.2) =====
        double baseRxUsage = 5 + rand.nextDouble() * 15;
        double baseTxUsage = 3 + rand.nextDouble() * 12;

        double rxUsage = state.hasNetworkAnomaly ?
                Math.min(95, baseRxUsage + 50 + rand.nextDouble() * 25) : baseRxUsage;

        double txUsage = state.hasNetworkAnomaly ?
                Math.min(95, baseTxUsage + 50 + rand.nextDouble() * 25) : baseTxUsage;

        metric.setRxUsage(rxUsage);
        metric.setTxUsage(txUsage);

        // ===== 초당 전송량 (그래프 3.7) =====
        double inBytesPerSec = (bandwidthBps / 8) * (rxUsage / 100.0);
        double outBytesPerSec = (bandwidthBps / 8) * (txUsage / 100.0);

        metric.setInBytesPerSec(inBytesPerSec);
        metric.setOutBytesPerSec(outBytesPerSec);

        double inPktsPerSec = inBytesPerSec / 1500;  // 평균 패킷 크기 1500 bytes
        double outPktsPerSec = outBytesPerSec / 1500;

        metric.setInPktsPerSec(inPktsPerSec);
        metric.setOutPktsPerSec(outPktsPerSec);

        // ===== 누적 카운터 업데이트 =====
        String key = deviceId + "_" + nicName;

        long prevInPackets = cumulativeInPackets.getOrDefault(key, 0L);
        long prevOutPackets = cumulativeOutPackets.getOrDefault(key, 0L);
        long prevInBytes = cumulativeInBytes.getOrDefault(key, 0L);
        long prevOutBytes = cumulativeOutBytes.getOrDefault(key, 0L);

        long inPacketsInc = (long)(inPktsPerSec * 5);  // 5초간 증가량
        long outPacketsInc = (long)(outPktsPerSec * 5);
        long inBytesInc = (long)(inBytesPerSec * 5);
        long outBytesInc = (long)(outBytesPerSec * 5);

        long newInPackets = prevInPackets + inPacketsInc;
        long newOutPackets = prevOutPackets + outPacketsInc;
        long newInBytes = prevInBytes + inBytesInc;
        long newOutBytes = prevOutBytes + outBytesInc;

        cumulativeInPackets.put(key, newInPackets);
        cumulativeOutPackets.put(key, newOutPackets);
        cumulativeInBytes.put(key, newInBytes);
        cumulativeOutBytes.put(key, newOutBytes);

        // ===== 패킷/바이트 누적 (그래프 3.3, 3.4, 3.5, 3.6) =====
        metric.setInPktsTot(newInPackets);
        metric.setOutPktsTot(newOutPackets);
        metric.setInBytesTot(newInBytes);
        metric.setOutBytesTot(newOutBytes);

        // ===== 에러/드롭 패킷 (그래프 3.8) =====
        long inErrorInc = state.hasNetworkAnomaly ? rand.nextLong(100) : rand.nextLong(5);
        long outErrorInc = state.hasNetworkAnomaly ? rand.nextLong(100) : rand.nextLong(5);
        long inDiscardInc = state.hasNetworkAnomaly ? rand.nextLong(50) : rand.nextLong(2);
        long outDiscardInc = state.hasNetworkAnomaly ? rand.nextLong(50) : rand.nextLong(2);

        long prevInErrors = cumulativeInErrors.getOrDefault(key, 0L);
        long prevOutErrors = cumulativeOutErrors.getOrDefault(key, 0L);
        long prevInDiscards = cumulativeInDiscards.getOrDefault(key, 0L);
        long prevOutDiscards = cumulativeOutDiscards.getOrDefault(key, 0L);

        long newInErrors = prevInErrors + inErrorInc;
        long newOutErrors = prevOutErrors + outErrorInc;
        long newInDiscards = prevInDiscards + inDiscardInc;
        long newOutDiscards = prevOutDiscards + outDiscardInc;

        cumulativeInErrors.put(key, newInErrors);
        cumulativeOutErrors.put(key, newOutErrors);
        cumulativeInDiscards.put(key, newInDiscards);
        cumulativeOutDiscards.put(key, newOutDiscards);

        metric.setInErrorPktsTot(newInErrors);
        metric.setOutErrorPktsTot(newOutErrors);
        metric.setInDiscardPktsTot(newInDiscards);
        metric.setOutDiscardPktsTot(newOutDiscards);

        // ===== 인터페이스 상태 (그래프 3.9) =====
        metric.setOperStatus(1);  // 1=UP, 0=DOWN

        return metric;
    }

    /**
     * 환경 메트릭 생성 (온도/습도)
     */
    private EnvironmentMetric generateEnvironmentMetric(int deviceId, LocalDateTime time) {
        AnomalyState state = anomalyStates.get(deviceId);
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        EnvironmentMetric metric = EnvironmentMetric.builder()
                .deviceId(deviceId)
                .generateTime(time)
                .build();

        // ===== 온도 생성 (정상 범위: 18°C ~ 26°C, 권장: 20°C ~ 24°C) =====
        double baseTemperature = 20.0 + rand.nextDouble() * 4.0;  // 20~24°C

        double currentTemperature;
        if (state.hasTemperatureAnomaly) {
            // 이상 징후 시 온도 급상승 (28°C ~ 35°C)
            currentTemperature = 28.0 + rand.nextDouble() * 7.0;
        } else {
            // 정상 범위 내 변동
            currentTemperature = baseTemperature + (rand.nextDouble() - 0.5) * 2.0;
        }

        metric.setTemperature(Math.round(currentTemperature * 100.0) / 100.0);

        // 최저/최고 온도 추적
        double currentMin = minTemperatureTracker.get(deviceId);
        double currentMax = maxTemperatureTracker.get(deviceId);

        if (currentTemperature < currentMin) {
            minTemperatureTracker.put(deviceId, currentTemperature);
            currentMin = currentTemperature;
        }
        if (currentTemperature > currentMax) {
            maxTemperatureTracker.put(deviceId, currentTemperature);
            currentMax = currentTemperature;
        }

        metric.setMinTemperature(Math.round(currentMin * 100.0) / 100.0);
        metric.setMaxTemperature(Math.round(currentMax * 100.0) / 100.0);

        // 온도 경고 설정 (26°C 이상 경고)
        metric.setTemperatureWarning(currentTemperature >= 26.0);

        // ===== 습도 생성 (정상 범위: 40% ~ 60%, 권장: 45% ~ 55%) =====
        double baseHumidity = 45.0 + rand.nextDouble() * 10.0;  // 45~55%

        double currentHumidity;
        if (state.hasHumidityAnomaly) {
            // 이상 징후 시 습도 급상승 또는 급하강
            if (rand.nextBoolean()) {
                currentHumidity = 65.0 + rand.nextDouble() * 15.0;  // 높음: 65~80%
            } else {
                currentHumidity = 20.0 + rand.nextDouble() * 15.0;  // 낮음: 20~35%
            }
        } else {
            // 정상 범위 내 변동
            currentHumidity = baseHumidity + (rand.nextDouble() - 0.5) * 5.0;
        }

        metric.setHumidity(Math.round(currentHumidity * 100.0) / 100.0);

        // 최저/최고 습도 추적
        double currentMinHumidity = minHumidityTracker.get(deviceId);
        double currentMaxHumidity = maxHumidityTracker.get(deviceId);

        if (currentHumidity < currentMinHumidity) {
            minHumidityTracker.put(deviceId, currentHumidity);
            currentMinHumidity = currentHumidity;
        }
        if (currentHumidity > currentMaxHumidity) {
            maxHumidityTracker.put(deviceId, currentHumidity);
            currentMaxHumidity = currentHumidity;
        }

        metric.setMinHumidity(Math.round(currentMinHumidity * 100.0) / 100.0);
        metric.setMaxHumidity(Math.round(currentMaxHumidity * 100.0) / 100.0);

        // 습도 경고 설정 (40% 미만 또는 60% 초과 시 경고)
        metric.setHumidityWarning(currentHumidity < 40.0 || currentHumidity > 60.0);

        return metric;
    }

    private void maybeUpdateAnomalies() {
        long currentTime = System.currentTimeMillis();

        for (int deviceId : DEVICE_IDS) {
            AnomalyState state = anomalyStates.get(deviceId);

            // CPU 이상 징후
            if (state.hasCpuAnomaly) {
                if (currentTime - state.cpuAnomalyStartTime > state.cpuAnomalyDuration) {
                    state.hasCpuAnomaly = false;
                    log.warn("✅ [Device {}] CPU 이상 징후 해소!", deviceId);
                }
            } else if (random.nextDouble() < 0.05) {
                state.hasCpuAnomaly = true;
                state.cpuAnomalyStartTime = currentTime;
                state.cpuAnomalyDuration = 30_000 + random.nextInt(90_000);
                log.error("🚨 [Device {}] CPU 이상 징후 발생! (지속: {}초)",
                        deviceId, state.cpuAnomalyDuration / 1000);
            }

            // 메모리 이상 징후
            if (state.hasMemoryAnomaly) {
                if (currentTime - state.memoryAnomalyStartTime > state.memoryAnomalyDuration) {
                    state.hasMemoryAnomaly = false;
                    log.warn("✅ [Device {}] 메모리 이상 징후 해소!", deviceId);
                }
            } else if (random.nextDouble() < 0.04) {
                state.hasMemoryAnomaly = true;
                state.memoryAnomalyStartTime = currentTime;
                state.memoryAnomalyDuration = 40_000 + random.nextInt(80_000);
                log.error("🚨 [Device {}] 메모리 이상 징후 발생! (지속: {}초)",
                        deviceId, state.memoryAnomalyDuration / 1000);
            }

            // 디스크 I/O 이상 징후
            if (state.hasDiskAnomaly) {
                if (currentTime - state.diskAnomalyStartTime > state.diskAnomalyDuration) {
                    state.hasDiskAnomaly = false;
                    log.warn("✅ [Device {}] 디스크 I/O 이상 징후 해소!", deviceId);
                }
            } else if (random.nextDouble() < 0.03) {
                state.hasDiskAnomaly = true;
                state.diskAnomalyStartTime = currentTime;
                state.diskAnomalyDuration = 20_000 + random.nextInt(60_000);
                log.error("🚨 [Device {}] 디스크 I/O 이상 징후 발생! (지속: {}초)",
                        deviceId, state.diskAnomalyDuration / 1000);
            }

            // 네트워크 이상 징후
            if (state.hasNetworkAnomaly) {
                if (currentTime - state.networkAnomalyStartTime > state.networkAnomalyDuration) {
                    state.hasNetworkAnomaly = false;
                    log.warn("✅ [Device {}] 네트워크 이상 징후 해소!", deviceId);
                }
            } else if (random.nextDouble() < 0.06) {
                state.hasNetworkAnomaly = true;
                state.networkAnomalyStartTime = currentTime;
                state.networkAnomalyDuration = 25_000 + random.nextInt(75_000);
                log.error("🚨 [Device {}] 네트워크 이상 징후 발생! (지속: {}초)",
                        deviceId, state.networkAnomalyDuration / 1000);
            }

            // ===== 온도 이상 징후 =====
            if (state.hasTemperatureAnomaly) {
                if (currentTime - state.temperatureAnomalyStartTime > state.temperatureAnomalyDuration) {
                    state.hasTemperatureAnomaly = false;
                    log.warn("✅ [Device {}] 온도 이상 징후 해소!", deviceId);
                }
            } else if (random.nextDouble() < 0.04) {
                state.hasTemperatureAnomaly = true;
                state.temperatureAnomalyStartTime = currentTime;
                state.temperatureAnomalyDuration = 35_000 + random.nextInt(85_000);
                log.error("🚨 [Device {}] 온도 이상 징후 발생! (지속: {}초)",
                        deviceId, state.temperatureAnomalyDuration / 1000);
            }

            // ===== 습도 이상 징후 =====
            if (state.hasHumidityAnomaly) {
                if (currentTime - state.humidityAnomalyStartTime > state.humidityAnomalyDuration) {
                    state.hasHumidityAnomaly = false;
                    log.warn("✅ [Device {}] 습도 이상 징후 해소!", deviceId);
                }
            } else if (random.nextDouble() < 0.03) {
                state.hasHumidityAnomaly = true;
                state.humidityAnomalyStartTime = currentTime;
                state.humidityAnomalyDuration = 30_000 + random.nextInt(70_000);
                log.error("🚨 [Device {}] 습도 이상 징후 발생! (지속: {}초)",
                        deviceId, state.humidityAnomalyDuration / 1000);
            }
        }
    }

    private static class AnomalyState {
        boolean hasCpuAnomaly = false;
        long cpuAnomalyStartTime = 0;
        long cpuAnomalyDuration = 0;

        boolean hasMemoryAnomaly = false;
        long memoryAnomalyStartTime = 0;
        long memoryAnomalyDuration = 0;

        boolean hasDiskAnomaly = false;
        long diskAnomalyStartTime = 0;
        long diskAnomalyDuration = 0;

        boolean hasNetworkAnomaly = false;
        long networkAnomalyStartTime = 0;
        long networkAnomalyDuration = 0;

        // 온도/습도 이상 징후 추가
        boolean hasTemperatureAnomaly = false;
        long temperatureAnomalyStartTime = 0;
        long temperatureAnomalyDuration = 0;

        boolean hasHumidityAnomaly = false;
        long humidityAnomalyStartTime = 0;
        long humidityAnomalyDuration = 0;
    }
}