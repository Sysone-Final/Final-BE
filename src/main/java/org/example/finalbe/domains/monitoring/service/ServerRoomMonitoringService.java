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

    /**
     * 서버실 실시간 통계 계산
     * ✅ 수정: findById() → findActiveById() 로 변경하여 삭제된 서버실 제외
     */
    public ServerRoomStatisticsDto calculateServerRoomStatistics(Long serverRoomId) {
        log.debug("📊 서버실 통계 계산 시작: serverRoomId={}", serverRoomId);

        // ✅ 수정: 삭제된 서버실은 통계 계산하지 않음
        ServerRoom serverRoom = serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new IllegalArgumentException("활성 서버실을 찾을 수 없습니다: " + serverRoomId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);

        // 1. 서버실 내 모든 랙 조회 (활성 랙만)
        List<Long> rackIds = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N)
                .stream()
                .map(rack -> rack.getId())
                .toList();

        if (rackIds.isEmpty()) {
            log.warn("⚠️ 서버실에 활성 랙이 없습니다: serverRoomId={}", serverRoomId);
            return createEmptyStatistics(serverRoom, now);
        }

        // 2. 서버실 내 모든 장비 조회 (활성 장비만)
        List<Long> equipmentIds = equipmentRepository.findByRackIdInAndDelYn(rackIds, DelYN.N)
                .stream()
                .map(equipment -> equipment.getId())
                .toList();

        if (equipmentIds.isEmpty()) {
            log.warn("⚠️ 서버실에 활성 장비가 없습니다: serverRoomId={}", serverRoomId);
            return createEmptyStatistics(serverRoom, now);
        }

        // 3. 장비 통계
        long totalEquipments = equipmentIds.size();
        long activeEquipments = equipmentRepository.countByRackIdInAndStatusAndDelYn(
                rackIds, EquipmentStatus.NORMAL, DelYN.N);
        long inactiveEquipments = totalEquipments - activeEquipments;

        // 4. CPU 통계 (최근 1분)
        Map<String, Object> cpuStats = systemMetricRepository.getAverageCpuStatsByEquipmentIds(
                equipmentIds, oneMinuteAgo, now);

        // 5. 메모리 통계 (최근 1분)
        Map<String, Object> memoryStats = systemMetricRepository.getAverageMemoryStatsByEquipmentIds(
                equipmentIds, oneMinuteAgo, now);

        // 6. 디스크 통계 (최근 1분)
        Map<String, Object> diskStats = diskMetricRepository.getAverageDiskStatsByEquipmentIds(
                equipmentIds, oneMinuteAgo, now);

        // 7. 네트워크 통계 (최근 1분)
        Map<String, Object> networkStats = networkMetricRepository.getAverageNetworkStatsByEquipmentIds(
                equipmentIds, oneMinuteAgo, now);

        // 8. 랙 통계
        long totalRacks = rackIds.size();
        long activeRacks = rackRepository.countByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

        // 9. 환경 통계 (최근 1분)
        Map<String, Object> envStats = environmentMetricRepository.getAverageEnvironmentStatsByRackIds(
                rackIds, oneMinuteAgo, now);

        // 10. 알람 통계 계산 (임계값 기반)
        int criticalAlerts = calculateCriticalAlerts(cpuStats, memoryStats, diskStats);
        int warningAlerts = calculateWarningAlerts(cpuStats, memoryStats, diskStats);
        int totalAlerts = criticalAlerts + warningAlerts;

        // 11. 통계 DTO 생성
        return ServerRoomStatisticsDto.builder()
                .serverRoomId(serverRoomId)
                .serverRoomName(serverRoom.getName())
                .timestamp(now)
                // 장비 통계
                .totalEquipments((int) totalEquipments)
                .activeEquipments((int) activeEquipments)
                .inactiveEquipments((int) inactiveEquipments)
                // CPU 통계
                .avgCpuUsage(getDoubleValue(cpuStats, "avgCpuUsage"))
                .maxCpuUsage(getDoubleValue(cpuStats, "maxCpuUsage"))
                .minCpuUsage(getDoubleValue(cpuStats, "minCpuUsage"))
                .avgLoadAvg1(getDoubleValue(cpuStats, "avgLoadAvg1"))
                // 메모리 통계
                .avgMemoryUsage(getDoubleValue(memoryStats, "avgMemoryUsage"))
                .maxMemoryUsage(getDoubleValue(memoryStats, "maxMemoryUsage"))
                .minMemoryUsage(getDoubleValue(memoryStats, "minMemoryUsage"))
                .totalMemoryBytes(getLongValue(memoryStats, "totalMemory"))
                .usedMemoryBytes(getLongValue(memoryStats, "totalUsedMemory"))
                .avgSwapUsage(getDoubleValue(memoryStats, "avgSwapUsage"))
                // 디스크 통계
                .avgDiskUsage(getDoubleValue(diskStats, "avgDiskUsage"))
                .maxDiskUsage(getDoubleValue(diskStats, "maxDiskUsage"))
                .minDiskUsage(getDoubleValue(diskStats, "minDiskUsage"))
                .totalDiskBytes(getLongValue(diskStats, "totalDiskBytes"))
                .usedDiskBytes(getLongValue(diskStats, "totalUsedDiskBytes"))
                .avgDiskIoUsage(getDoubleValue(diskStats, "avgDiskIoUsage"))
                // 네트워크 통계
                .totalInBps(getDoubleValue(networkStats, "totalInBps"))
                .totalOutBps(getDoubleValue(networkStats, "totalOutBps"))
                .avgRxUsage(getDoubleValue(networkStats, "avgRxUsage"))
                .avgTxUsage(getDoubleValue(networkStats, "avgTxUsage"))
                .totalInErrors(getLongValue(networkStats, "totalInErrors"))
                .totalOutErrors(getLongValue(networkStats, "totalOutErrors"))
                // 환경 통계
                .avgTemperature(getDoubleValue(envStats, "avgTemperature"))
                .maxTemperature(getDoubleValue(envStats, "maxTemperature"))
                .minTemperature(getDoubleValue(envStats, "minTemperature"))
                .avgHumidity(getDoubleValue(envStats, "avgHumidity"))
                .maxHumidity(getDoubleValue(envStats, "maxHumidity"))
                .minHumidity(getDoubleValue(envStats, "minHumidity"))
                .temperatureWarnings(getIntValue(envStats, "temperatureWarnings"))
                .humidityWarnings(getIntValue(envStats, "humidityWarnings"))
                // 랙 통계
                .totalRacks((int) totalRacks)
                .activeRacks((int) activeRacks)
                // 알람 통계
                .totalAlerts(totalAlerts)
                .criticalAlerts(criticalAlerts)
                .warningAlerts(warningAlerts)
                // 전력 통계
                .totalPowerUsage(getDoubleValue(envStats, "totalPowerUsage"))
                .avgPowerUsagePerRack(totalRacks > 0 ? getDoubleValue(envStats, "totalPowerUsage") / totalRacks : 0.0)
                .build();
    }

    /**
     * 빈 통계 생성 (랙이나 장비가 없는 경우)
     */
    private ServerRoomStatisticsDto createEmptyStatistics(ServerRoom serverRoom, LocalDateTime now) {
        return ServerRoomStatisticsDto.builder()
                .serverRoomId(serverRoom.getId())
                .serverRoomName(serverRoom.getName())
                .timestamp(now)
                .totalEquipments(0)
                .activeEquipments(0)
                .inactiveEquipments(0)
                .totalRacks(0)
                .activeRacks(0)
                .totalAlerts(0)
                .criticalAlerts(0)
                .warningAlerts(0)
                .build();
    }

    // Helper 메서드들
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

        // CPU Critical (>90%)
        Double avgCpu = getDoubleValue(cpuStats, "avgCpuUsage");
        if (avgCpu > 90.0) count++;

        // Memory Critical (>90%)
        Double avgMemory = getDoubleValue(memoryStats, "avgMemoryUsage");
        if (avgMemory > 90.0) count++;

        // Disk Critical (>90%)
        Double avgDisk = getDoubleValue(diskStats, "avgDiskUsage");
        if (avgDisk > 90.0) count++;

        return count;
    }

    private int calculateWarningAlerts(Map<String, Object> cpuStats,
                                       Map<String, Object> memoryStats,
                                       Map<String, Object> diskStats) {
        int count = 0;

        // CPU Warning (70-90%)
        Double avgCpu = getDoubleValue(cpuStats, "avgCpuUsage");
        if (avgCpu > 70.0 && avgCpu <= 90.0) count++;

        // Memory Warning (70-90%)
        Double avgMemory = getDoubleValue(memoryStats, "avgMemoryUsage");
        if (avgMemory > 70.0 && avgMemory <= 90.0) count++;

        // Disk Warning (70-90%)
        Double avgDisk = getDoubleValue(diskStats, "avgDiskUsage");
        if (avgDisk > 70.0 && avgDisk <= 90.0) count++;

        return count;
    }
}