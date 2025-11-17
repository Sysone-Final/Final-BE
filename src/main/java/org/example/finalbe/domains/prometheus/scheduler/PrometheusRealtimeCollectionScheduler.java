package org.example.finalbe.domains.prometheus.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.domain.*;
import org.example.finalbe.domains.prometheus.repository.cpu.PrometheusCpuMetricRepository;
import org.example.finalbe.domains.prometheus.repository.disk.PrometheusDiskMetricRepository;
import org.example.finalbe.domains.prometheus.repository.memory.PrometheusMemoryMetricRepository;
import org.example.finalbe.domains.prometheus.repository.network.PrometheusNetworkMetricRepository;
import org.example.finalbe.domains.prometheus.repository.realtime.*;
import org.example.finalbe.domains.prometheus.repository.temperature.PrometheusTemperatureMetricRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusRealtimeCollectionScheduler {

    // Views 조회 Repository
    private final PrometheusCpuMetricRepository cpuViewRepo;
    private final PrometheusMemoryMetricRepository memoryViewRepo;
    private final PrometheusDiskMetricRepository diskViewRepo;
    private final PrometheusNetworkMetricRepository networkViewRepo;
    private final PrometheusTemperatureMetricRepository temperatureViewRepo;

    // 새 테이블 저장 Repository
    private final PrometheusCpuRealtimeRepository cpuRealtimeRepo;
    private final PrometheusMemoryRealtimeRepository memoryRealtimeRepo;
    private final PrometheusDiskRealtimeRepository diskRealtimeRepo;
    private final PrometheusNetworkRealtimeRepository networkRealtimeRepo;
    private final PrometheusTemperatureRealtimeRepository temperatureRealtimeRepo;

    /**
     * 15초마다 views에서 최신 데이터 조회 → 새 테이블 저장
     */
    @Scheduled(fixedRate = 15000)
    public void collectLatestMetrics() {
        try {
            Instant now = Instant.now();

            log.debug("실시간 메트릭 수집 시작");

            // 1. CPU 메트릭 수집 (모든 인스턴스)
            collectCpuMetrics(now);

            // 2. Memory 메트릭 수집 (모든 인스턴스)
            collectMemoryMetrics(now);

            // 3. Disk 메트릭 수집 (모든 디바이스)
            collectDiskMetrics(now);

            // 4. Network 메트릭 수집 (모든 네트워크 디바이스)
            collectNetworkMetrics(now);

            // 5. Temperature 메트릭 수집 (모든 센서)
            collectTemperatureMetrics(now);

            log.debug("실시간 메트릭 수집 완료");

        } catch (Exception e) {
            log.error("실시간 메트릭 수집 실패", e);
        }
    }

    /**
     * CPU 메트릭 수집 (모든 인스턴스)
     */
    private void collectCpuMetrics(Instant time) {
        try {
            List<Object[]> cpuDataList = cpuViewRepo.getLatestCpuUsageAllInstances();
            List<Object[]> loadDataList = cpuViewRepo.getLatestLoadAverageAllInstances();
            List<Object[]> contextDataList = cpuViewRepo.getLatestContextSwitchesAllInstances();

            if (cpuDataList.isEmpty()) {
                log.warn("CPU 데이터 없음");
                return;
            }

            List<PrometheusCpuRealtime> entities = new ArrayList<>();

            for (Object[] cpuData : cpuDataList) {
                Integer instanceId = cpuData[0] != null ? ((Number) cpuData[0]).intValue() : null;
                if (instanceId == null) continue;

                // 해당 instance의 load average 찾기
                Object[] loadData = findByInstanceId(loadDataList, instanceId);
                Object[] contextData = findByInstanceId(contextDataList, instanceId);

                PrometheusCpuRealtime entity = PrometheusCpuRealtime.builder()
                        .time(time)
                        .instanceId(instanceId)
                        .cpuUsagePercent(cpuData[1] != null ? ((Number) cpuData[1]).doubleValue() : 0.0)
                        .loadAvg1(loadData != null && loadData[1] != null ? ((Number) loadData[1]).doubleValue() : 0.0)
                        .loadAvg5(loadData != null && loadData[2] != null ? ((Number) loadData[2]).doubleValue() : 0.0)
                        .loadAvg15(loadData != null && loadData[3] != null ? ((Number) loadData[3]).doubleValue() : 0.0)
                        .contextSwitches(contextData != null && contextData[1] != null ? ((Number) contextData[1]).longValue() : 0L)
                        .build();

                entities.add(entity);
            }

            cpuRealtimeRepo.saveAll(entities);
            log.debug("CPU 메트릭 저장 완료 - {} 인스턴스", entities.size());

        } catch (Exception e) {
            log.error("CPU 메트릭 수집 실패", e);
        }
    }

    /**
     * Memory 메트릭 수집 (모든 인스턴스)
     */
    private void collectMemoryMetrics(Instant time) {
        try {
            List<Object[]> memoryDataList = memoryViewRepo.getLatestMemoryUsageAllInstances();
            List<Object[]> compositionDataList = memoryViewRepo.getLatestMemoryCompositionAllInstances();
            List<Object[]> swapDataList = memoryViewRepo.getLatestSwapUsageAllInstances();

            if (memoryDataList.isEmpty()) {
                log.warn("Memory 데이터 없음");
                return;
            }

            List<PrometheusMemoryRealtime> entities = new ArrayList<>();

            for (Object[] memoryData : memoryDataList) {
                Integer instanceId = memoryData[0] != null ? ((Number) memoryData[0]).intValue() : null;
                if (instanceId == null) continue;

                Object[] compositionData = findByInstanceId(compositionDataList, instanceId);
                Object[] swapData = findByInstanceId(swapDataList, instanceId);

                PrometheusMemoryRealtime entity = PrometheusMemoryRealtime.builder()
                        .time(time)
                        .instanceId(instanceId)
                        .totalBytes(memoryData[1] != null ? ((Number) memoryData[1]).longValue() : 0L)
                        .availableBytes(memoryData[2] != null ? ((Number) memoryData[2]).longValue() : 0L)
                        .usagePercent(memoryData[3] != null ? ((Number) memoryData[3]).doubleValue() : 0.0)
                        .activeBytes(compositionData != null && compositionData[1] != null ? ((Number) compositionData[1]).longValue() : 0L)
                        .inactiveBytes(compositionData != null && compositionData[2] != null ? ((Number) compositionData[2]).longValue() : 0L)
                        .buffersBytes(compositionData != null && compositionData[3] != null ? ((Number) compositionData[3]).longValue() : 0L)
                        .cachedBytes(compositionData != null && compositionData[4] != null ? ((Number) compositionData[4]).longValue() : 0L)
                        .freeBytes(compositionData != null && compositionData[5] != null ? ((Number) compositionData[5]).longValue() : 0L)
                        .swapTotalBytes(swapData != null && swapData[1] != null ? ((Number) swapData[1]).longValue() : 0L)
                        .swapFreeBytes(swapData != null && swapData[2] != null ? ((Number) swapData[2]).longValue() : 0L)
                        .swapUsagePercent(swapData != null && swapData[4] != null ? ((Number) swapData[4]).doubleValue() : 0.0)
                        .build();

                entities.add(entity);
            }

            memoryRealtimeRepo.saveAll(entities);
            log.debug("Memory 메트릭 저장 완료 - {} 인스턴스", entities.size());

        } catch (Exception e) {
            log.error("Memory 메트릭 수집 실패", e);
        }
    }

    /**
     * Disk 메트릭 수집 (모든 디바이스)
     */
    private void collectDiskMetrics(Instant time) {
        try {
            List<Object[]> diskDataList = diskViewRepo.getLatestDiskUsageAllDevices();
            List<Object[]> ioDataList = diskViewRepo.getLatestDiskIoAllDevices();
            List<Object[]> inodeDataList = diskViewRepo.getLatestInodeUsageAllDevices();

            if (diskDataList.isEmpty()) {
                log.warn("Disk 데이터 없음");
                return;
            }

            List<PrometheusDiskRealtime> entities = new ArrayList<>();

            for (Object[] diskData : diskDataList) {
                Integer instanceId = diskData[0] != null ? ((Number) diskData[0]).intValue() : null;
                Integer deviceId = diskData[1] != null ? ((Number) diskData[1]).intValue() : null;
                Integer mountpointId = diskData[2] != null ? ((Number) diskData[2]).intValue() : null;

                if (instanceId == null || deviceId == null || mountpointId == null) continue;

                Object[] ioData = findByDeviceId(ioDataList, instanceId, deviceId);
                Object[] inodeData = findByDeviceId(inodeDataList, instanceId, deviceId);

                PrometheusDiskRealtime entity = PrometheusDiskRealtime.builder()
                        .time(time)
                        .instanceId(instanceId)
                        .deviceId(deviceId)
                        .mountpointId(mountpointId)
                        .totalBytes(diskData[3] != null ? ((Number) diskData[3]).longValue() : 0L)
                        .freeBytes(diskData[4] != null ? ((Number) diskData[4]).longValue() : 0L)
                        .usagePercent(diskData[5] != null ? ((Number) diskData[5]).doubleValue() : 0.0)
                        .readBytesPerSec(ioData != null && ioData[3] != null ? ((Number) ioData[3]).doubleValue() : 0.0)
                        .writeBytesPerSec(ioData != null && ioData[4] != null ? ((Number) ioData[4]).doubleValue() : 0.0)
                        .readIops(0.0)
                        .writeIops(0.0)
                        .ioUtilizationPercent(0.0)
                        .totalInodes(inodeData != null && inodeData[3] != null ? ((Number) inodeData[3]).longValue() : 0L)
                        .freeInodes(inodeData != null && inodeData[4] != null ? ((Number) inodeData[4]).longValue() : 0L)
                        .inodeUsagePercent(inodeData != null && inodeData[5] != null ? ((Number) inodeData[5]).doubleValue() : 0.0)
                        .build();

                entities.add(entity);
            }

            diskRealtimeRepo.saveAll(entities);
            log.debug("Disk 메트릭 저장 완료 - {} 디바이스", entities.size());

        } catch (Exception e) {
            log.error("Disk 메트릭 수집 실패", e);
        }
    }

    /**
     * Network 메트릭 수집 (모든 네트워크 디바이스)
     */
    private void collectNetworkMetrics(Instant time) {
        try {
            List<Object[]> networkDataList = networkViewRepo.getLatestNetworkUsageAllDevices();
            List<Object[]> errorDataList = networkViewRepo.getLatestNetworkErrorsAllDevices();

            if (networkDataList.isEmpty()) {
                log.warn("Network 데이터 없음");
                return;
            }

            List<PrometheusNetworkRealtime> entities = new ArrayList<>();

            for (Object[] networkData : networkDataList) {
                Integer instanceId = networkData[0] != null ? ((Number) networkData[0]).intValue() : null;
                Integer deviceId = networkData[1] != null ? ((Number) networkData[1]).intValue() : null;

                if (instanceId == null || deviceId == null) continue;

                Object[] errorData = findByNetworkDeviceId(errorDataList, instanceId, deviceId);

                PrometheusNetworkRealtime entity = PrometheusNetworkRealtime.builder()
                        .time(time)
                        .instanceId(instanceId)
                        .deviceId(deviceId)
                        .rxBytesTotal(networkData[2] != null ? ((Number) networkData[2]).longValue() : 0L)
                        .txBytesTotal(networkData[3] != null ? ((Number) networkData[3]).longValue() : 0L)
                        .rxPacketsTotal(networkData[4] != null ? ((Number) networkData[4]).longValue() : 0L)
                        .txPacketsTotal(networkData[5] != null ? ((Number) networkData[5]).longValue() : 0L)
                        .rxErrorsTotal(errorData != null && errorData[2] != null ? ((Number) errorData[2]).longValue() : 0L)
                        .txErrorsTotal(errorData != null && errorData[3] != null ? ((Number) errorData[3]).longValue() : 0L)
                        .rxDroppedTotal(errorData != null && errorData[4] != null ? ((Number) errorData[4]).longValue() : 0L)
                        .txDroppedTotal(errorData != null && errorData[5] != null ? ((Number) errorData[5]).longValue() : 0L)
                        .rxBps(0.0)
                        .txBps(0.0)
                        .build();

                entities.add(entity);
            }

            networkRealtimeRepo.saveAll(entities);
            log.debug("Network 메트릭 저장 완료 - {} 디바이스", entities.size());

        } catch (Exception e) {
            log.error("Network 메트릭 수집 실패", e);
        }
    }

    /**
     * Temperature 메트릭 수집 (모든 센서)
     */
    private void collectTemperatureMetrics(Instant time) {
        try {
            List<Object[]> tempDataList = temperatureViewRepo.getLatestTemperatureAllSensors();

            if (tempDataList.isEmpty()) {
                log.warn("Temperature 데이터 없음");
                return;
            }

            List<PrometheusTemperatureRealtime> entities = new ArrayList<>();

            for (Object[] tempData : tempDataList) {
                Integer instanceId = tempData[0] != null ? ((Number) tempData[0]).intValue() : null;
                Integer chipId = tempData[1] != null ? ((Number) tempData[1]).intValue() : null;
                Integer sensorId = tempData[2] != null ? ((Number) tempData[2]).intValue() : null;

                if (instanceId == null || chipId == null || sensorId == null) continue;

                PrometheusTemperatureRealtime entity = PrometheusTemperatureRealtime.builder()
                        .time(time)
                        .instanceId(instanceId)
                        .chipId(chipId)
                        .sensorId(sensorId)
                        .celsius(tempData[3] != null ? ((Number) tempData[3]).doubleValue() : 0.0)
                        .build();

                entities.add(entity);
            }

            temperatureRealtimeRepo.saveAll(entities);
            log.debug("Temperature 메트릭 저장 완료 - {} 센서", entities.size());

        } catch (Exception e) {
            log.error("Temperature 메트릭 수집 실패", e);
        }
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * instanceId로 데이터 찾기
     */
    private Object[] findByInstanceId(List<Object[]> dataList, Integer instanceId) {
        return dataList.stream()
                .filter(row -> row[0] != null && ((Number) row[0]).intValue() == instanceId)
                .findFirst()
                .orElse(null);
    }

    /**
     * deviceId로 데이터 찾기
     */
    private Object[] findByDeviceId(List<Object[]> dataList, Integer instanceId, Integer deviceId) {
        return dataList.stream()
                .filter(row -> row[0] != null && ((Number) row[0]).intValue() == instanceId
                        && row[1] != null && ((Number) row[1]).intValue() == deviceId)
                .findFirst()
                .orElse(null);
    }

    /**
     * network deviceId로 데이터 찾기
     */
    private Object[] findByNetworkDeviceId(List<Object[]> dataList, Integer instanceId, Integer deviceId) {
        return dataList.stream()
                .filter(row -> row[0] != null && ((Number) row[0]).intValue() == instanceId
                        && row[1] != null && ((Number) row[1]).intValue() == deviceId)
                .findFirst()
                .orElse(null);
    }
}