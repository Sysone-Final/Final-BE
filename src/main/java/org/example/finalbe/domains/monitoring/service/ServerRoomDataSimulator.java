package org.example.finalbe.domains.monitoring.service;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
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
        }

        log.info("✅ 초기화 완료! {}개 서버 모니터링 시작", DEVICE_IDS.length);
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
        long totalBytes = partition.startsWith("/") ?
                100L * 1024 * 1024 * 1024 : 500L * 1024 * 1024 * 1024;

        double baseUsage = 30 + rand.nextDouble() * 30;
        double usagePercent = state.hasDiskAnomaly ?
                Math.min(95, baseUsage + 25 + rand.nextDouble() * 15) : baseUsage;

        long usedBytes = (long)(totalBytes * usagePercent / 100);
        long freeBytes = totalBytes - usedBytes;

        metric.setTotalBytes(totalBytes);
        metric.setUsedBytes(usedBytes);
        metric.setFreeBytes(freeBytes);
        metric.setUsedPercentage(usagePercent);

        // ===== I/O 메트릭 (그래프 4.2, 4.3, 4.4) =====
        double ioReadBps = state.hasDiskAnomaly ?
                rand.nextDouble() * 100_000_000 : rand.nextDouble() * 10_000_000;

        double ioWriteBps = state.hasDiskAnomaly ?
                rand.nextDouble() * 150_000_000 : rand.nextDouble() * 20_000_000;

        metric.setIoReadBps(ioReadBps);
        metric.setIoWriteBps(ioWriteBps);

        double ioTimePercent = Math.min(100, (ioReadBps + ioWriteBps) / 2_000_000);
        metric.setIoTimePercentage(ioTimePercent);

        // IOPS 누적 카운터
        String readKey = "read_" + deviceId + "_" + partition;
        String writeKey = "write_" + deviceId + "_" + partition;

        long prevReads = cumulativeIoReads.getOrDefault(readKey, 0L);
        long prevWrites = cumulativeIoWrites.getOrDefault(writeKey, 0L);

        long readInc = (long)(ioReadBps / 4096 * 5);  // 5초간 증가량
        long writeInc = (long)(ioWriteBps / 4096 * 5);

        long newReads = prevReads + readInc;
        long newWrites = prevWrites + writeInc;

        cumulativeIoReads.put(readKey, newReads);
        cumulativeIoWrites.put(writeKey, newWrites);

        metric.setIoReadCount(newReads);
        metric.setIoWriteCount(newWrites);

        // ===== inode (그래프 4.6) =====
        long totalInodes = 6_000_000L;
        double inodeUsagePercent = usagePercent * 0.7;  // 디스크 사용률의 70%
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
    }
}