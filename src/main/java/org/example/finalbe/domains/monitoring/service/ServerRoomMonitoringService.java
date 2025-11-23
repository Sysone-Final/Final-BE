package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.dto.ServerRoomStatisticsDto;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.example.finalbe.domains.monitoring.repository.SystemMetricRepository;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 서버실 레벨 모니터링 집계 서비스
 * 서버실 내 모든 장비/랙의 메트릭을 집계하여 통계 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerRoomMonitoringService {

    private final ServerRoomRepository serverRoomRepository;
    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final SystemMetricRepository systemMetricRepository;
    private final DiskMetricRepository diskMetricRepository;
    private final NetworkMetricRepository networkMetricRepository;
    private final EnvironmentMetricRepository environmentMetricRepository;

    public ServerRoomStatisticsDto calculateServerRoomStatistics(Long serverRoomId) {
        log.debug("📊 서버실 통계 계산 시작: serverRoomId={}", serverRoomId);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new IllegalArgumentException("활성 서버실을 찾을 수 없습니다: " + serverRoomId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);

        // ✅ 전체 랙 수 (서버실 내 모든 랙)
        long totalRacks = rackRepository.countByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

        // ✅ 장비가 있는 랙만 조회 (활성 랙)
        List<Long> activeRackIds = equipmentRepository.findDistinctRackIdsByServerRoomId(serverRoomId);
        long activeRacks = activeRackIds.size();

        if (activeRackIds.isEmpty()) {
            log.warn("⚠️ 서버실에 장비가 배치된 랙이 없습니다: serverRoomId={}", serverRoomId);
            return createEmptyStatistics(serverRoom, now, (int) totalRacks);
        }

        // 장비 목록 조회
        List<Long> equipmentIds = equipmentRepository.findByRackIdInAndDelYn(activeRackIds, DelYN.N)
                .stream()
                .map(equipment -> equipment.getId())
                .toList();

        if (equipmentIds.isEmpty()) {
            log.warn("⚠️ 서버실에 활성 장비가 없습니다: serverRoomId={}", serverRoomId);
            return createEmptyStatistics(serverRoom, now, (int) totalRacks);
        }

        // 장비 통계
        long totalEquipments = equipmentIds.size();
        long activeEquipments = equipmentRepository.countByRackIdInAndStatusAndDelYn(
                activeRackIds, EquipmentStatus.NORMAL, DelYN.N);
        long inactiveEquipments = totalEquipments - activeEquipments;

        // CPU 통계
        Map<String, Object> cpuStats = systemMetricRepository.getAverageCpuStatsByEquipmentIds(
                equipmentIds, oneMinuteAgo, now);

        // 메모리 통계
        Map<String, Object> memoryStats = systemMetricRepository.getAverageMemoryStatsByEquipmentIds(
                equipmentIds, oneMinuteAgo, now);

        // 디스크 통계
        Map<String, Object> diskStats = diskMetricRepository.getAverageDiskStatsByEquipmentIds(
                equipmentIds, oneMinuteAgo, now);

        // 네트워크 통계
        Map<String, Object> networkStats = networkMetricRepository.getAverageNetworkStatsByEquipmentIds(
                equipmentIds, oneMinuteAgo, now);

        // 환경 통계 (활성 랙만)
        Map<String, Object> envStats = environmentMetricRepository.getAverageEnvironmentStatsByRackIds(
                activeRackIds, oneMinuteAgo, now);

        // 알람 통계
        int criticalAlerts = calculateCriticalAlerts(cpuStats, memoryStats, diskStats);
        int warningAlerts = calculateWarningAlerts(cpuStats, memoryStats, diskStats);
        int totalAlerts = criticalAlerts + warningAlerts;

        return ServerRoomStatisticsDto.builder()
                .serverRoomId(serverRoomId)
                .serverRoomName(serverRoom.getName())
                .timestamp(now)
                .totalEquipments((int) totalEquipments)
                .activeEquipments((int) activeEquipments)
                .inactiveEquipments((int) inactiveEquipments)
                .avgCpuUsage(getDoubleValue(cpuStats, "avgCpuUsage"))
                .maxCpuUsage(getDoubleValue(cpuStats, "maxCpuUsage"))
                .minCpuUsage(getDoubleValue(cpuStats, "minCpuUsage"))
                .avgLoadAvg1(getDoubleValue(cpuStats, "avgLoadAvg1"))
                .avgMemoryUsage(getDoubleValue(memoryStats, "avgMemoryUsage"))
                .maxMemoryUsage(getDoubleValue(memoryStats, "maxMemoryUsage"))
                .minMemoryUsage(getDoubleValue(memoryStats, "minMemoryUsage"))
                .totalMemoryBytes(getLongValue(memoryStats, "totalMemory"))
                .usedMemoryBytes(getLongValue(memoryStats, "totalUsedMemory"))
                .avgSwapUsage(getDoubleValue(memoryStats, "avgSwapUsage"))
                .avgDiskUsage(getDoubleValue(diskStats, "avgDiskUsage"))
                .maxDiskUsage(getDoubleValue(diskStats, "maxDiskUsage"))
                .minDiskUsage(getDoubleValue(diskStats, "minDiskUsage"))
                .totalDiskBytes(getLongValue(diskStats, "totalDiskBytes"))
                .usedDiskBytes(getLongValue(diskStats, "totalUsedDiskBytes"))
                .avgDiskIoUsage(getDoubleValue(diskStats, "avgDiskIoUsage"))
                .totalInBps(getDoubleValue(networkStats, "totalInBps"))
                .totalOutBps(getDoubleValue(networkStats, "totalOutBps"))
                .avgRxUsage(getDoubleValue(networkStats, "avgRxUsage"))
                .avgTxUsage(getDoubleValue(networkStats, "avgTxUsage"))
                .totalInErrors(getLongValue(networkStats, "totalInErrors"))
                .totalOutErrors(getLongValue(networkStats, "totalOutErrors"))
                .avgTemperature(getDoubleValue(envStats, "avgTemperature"))
                .maxTemperature(getDoubleValue(envStats, "maxTemperature"))
                .minTemperature(getDoubleValue(envStats, "minTemperature"))
                .avgHumidity(getDoubleValue(envStats, "avgHumidity"))
                .maxHumidity(getDoubleValue(envStats, "maxHumidity"))
                .minHumidity(getDoubleValue(envStats, "minHumidity"))
                .temperatureWarnings(getIntValue(envStats, "temperatureWarnings"))
                .humidityWarnings(getIntValue(envStats, "humidityWarnings"))
                .totalRacks((int) totalRacks)      // ✅ 전체 랙 (12개)
                .activeRacks((int) activeRacks)    // ✅ 활성 랙 (2개)
                .totalAlerts(totalAlerts)
                .criticalAlerts(criticalAlerts)
                .warningAlerts(warningAlerts)
                .totalPowerUsage(getDoubleValue(envStats, "totalPowerUsage"))
                .avgPowerUsagePerRack(activeRacks > 0 ? getDoubleValue(envStats, "totalPowerUsage") / activeRacks : 0.0)
                .build();
    }

    private ServerRoomStatisticsDto createEmptyStatistics(ServerRoom serverRoom, LocalDateTime now, int totalRacks) {
        return ServerRoomStatisticsDto.builder()
                .serverRoomId(serverRoom.getId())
                .serverRoomName(serverRoom.getName())
                .timestamp(now)
                .totalEquipments(0)
                .activeEquipments(0)
                .inactiveEquipments(0)
                .totalRacks(totalRacks)
                .activeRacks(0)
                .totalAlerts(0)
                .criticalAlerts(0)
                .warningAlerts(0)
                .build();
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private int calculateCriticalAlerts(Map<String, Object> cpuStats,
                                        Map<String, Object> memoryStats,
                                        Map<String, Object> diskStats) {
        int count = 0;

        Double avgCpu = getDoubleValue(cpuStats, "avgCpuUsage");
        if (avgCpu > 90.0) count++;

        Double avgMemory = getDoubleValue(memoryStats, "avgMemoryUsage");
        if (avgMemory > 90.0) count++;

        Double avgDisk = getDoubleValue(diskStats, "avgDiskUsage");
        if (avgDisk > 90.0) count++;

        return count;
    }

    private int calculateWarningAlerts(Map<String, Object> cpuStats,
                                       Map<String, Object> memoryStats,
                                       Map<String, Object> diskStats) {
        int count = 0;

        Double avgCpu = getDoubleValue(cpuStats, "avgCpuUsage");
        if (avgCpu > 70.0 && avgCpu <= 90.0) count++;

        Double avgMemory = getDoubleValue(memoryStats, "avgMemoryUsage");
        if (avgMemory > 70.0 && avgMemory <= 90.0) count++;

        Double avgDisk = getDoubleValue(diskStats, "avgDiskUsage");
        if (avgDisk > 70.0 && avgDisk <= 90.0) count++;

        return count;
    }
}