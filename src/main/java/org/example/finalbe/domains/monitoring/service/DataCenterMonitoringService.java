package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.monitoring.dto.DataCenterStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.ServerRoomStatisticsDto;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 데이터센터 레벨 모니터링 집계 서비스
 * 데이터센터 내 모든 서버실의 메트릭을 집계하여 통계 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataCenterMonitoringService {

    private final DataCenterRepository dataCenterRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final ServerRoomMonitoringService serverRoomMonitoringService;

    /**
     * 데이터센터 실시간 통계 계산
     */
    public DataCenterStatisticsDto calculateDataCenterStatistics(Long dataCenterId) {
        log.debug("📊 데이터센터 통계 계산 시작: dataCenterId={}", dataCenterId);

        DataCenter dataCenter = dataCenterRepository.findById(dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("데이터센터를 찾을 수 없습니다: " + dataCenterId));

        LocalDateTime now = LocalDateTime.now();

        // 1. 데이터센터 내 모든 서버실 조회
        List<ServerRoom> serverRooms = serverRoomRepository.findByDataCenterIdAndDelYn(dataCenterId, DelYN.N);

        if (serverRooms.isEmpty()) {
            log.warn("⚠️ 데이터센터에 서버실이 없습니다: dataCenterId={}", dataCenterId);
            return createEmptyStatistics(dataCenter, now);
        }

        // 2. 각 서버실의 통계 계산
        List<ServerRoomStatisticsDto> serverRoomStats = serverRooms.stream()
                .map(serverRoom -> {
                    try {
                        return serverRoomMonitoringService.calculateServerRoomStatistics(serverRoom.getId());
                    } catch (Exception e) {
                        log.error("서버실 통계 계산 실패: serverRoomId={}", serverRoom.getId(), e);
                        return null;
                    }
                })
                .filter(stats -> stats != null)
                .collect(Collectors.toList());

        if (serverRoomStats.isEmpty()) {
            log.warn("⚠️ 유효한 서버실 통계가 없습니다: dataCenterId={}", dataCenterId);
            return createEmptyStatistics(dataCenter, now);
        }

        // 3. 서버실별 통계 집계
        int totalServerRooms = serverRooms.size();
        int activeServerRooms = (int) serverRoomStats.stream()
                .filter(stats -> stats.getActiveEquipments() > 0)
                .count();

        int totalRacks = serverRoomStats.stream()
                .mapToInt(ServerRoomStatisticsDto::getTotalRacks)
                .sum();

        int activeRacks = serverRoomStats.stream()
                .mapToInt(ServerRoomStatisticsDto::getActiveRacks)
                .sum();

        int totalEquipments = serverRoomStats.stream()
                .mapToInt(ServerRoomStatisticsDto::getTotalEquipments)
                .sum();

        int activeEquipments = serverRoomStats.stream()
                .mapToInt(ServerRoomStatisticsDto::getActiveEquipments)
                .sum();

        int inactiveEquipments = serverRoomStats.stream()
                .mapToInt(ServerRoomStatisticsDto::getInactiveEquipments)
                .sum();

        // 4. CPU 통계 집계 (평균)
        Double avgCpuUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgCpuUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double maxCpuUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMaxCpuUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        Double minCpuUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMinCpuUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        Double avgLoadAvg1 = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgLoadAvg1)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // 5. 메모리 통계 집계
        Double avgMemoryUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgMemoryUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double maxMemoryUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMaxMemoryUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        Double minMemoryUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMinMemoryUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        Long totalMemoryBytes = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getTotalMemoryBytes)
                .filter(val -> val != null)
                .mapToLong(Long::longValue)
                .sum();

        Long usedMemoryBytes = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getUsedMemoryBytes)
                .filter(val -> val != null)
                .mapToLong(Long::longValue)
                .sum();

        Double avgSwapUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgSwapUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // 6. 디스크 통계 집계
        Double avgDiskUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgDiskUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double maxDiskUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMaxDiskUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        Double minDiskUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMinDiskUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        Long totalDiskBytes = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getTotalDiskBytes)
                .filter(val -> val != null)
                .mapToLong(Long::longValue)
                .sum();

        Long usedDiskBytes = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getUsedDiskBytes)
                .filter(val -> val != null)
                .mapToLong(Long::longValue)
                .sum();

        Double avgDiskIoUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgDiskIoUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // 7. 네트워크 통계 집계
        Double totalInBps = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getTotalInBps)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        Double totalOutBps = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getTotalOutBps)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .sum();

        Double avgRxUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgRxUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double avgTxUsage = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgTxUsage)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Long totalInErrors = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getTotalInErrors)
                .filter(val -> val != null)
                .mapToLong(Long::longValue)
                .sum();

        Long totalOutErrors = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getTotalOutErrors)
                .filter(val -> val != null)
                .mapToLong(Long::longValue)
                .sum();

        // 8. 환경 통계 집계
        Double avgTemperature = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgTemperature)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double maxTemperature = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMaxTemperature)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        Double minTemperature = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMinTemperature)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        Double avgHumidity = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getAvgHumidity)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double maxHumidity = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMaxHumidity)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        Double minHumidity = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getMinHumidity)
                .filter(val -> val != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);

        Integer temperatureWarnings = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getTemperatureWarnings)
                .filter(val -> val != null)
                .mapToInt(Integer::intValue)
                .sum();

        Integer humidityWarnings = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getHumidityWarnings)
                .filter(val -> val != null)
                .mapToInt(Integer::intValue)
                .sum();

        // 9. 알람 통계 집계
        Integer totalAlerts = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getTotalAlerts)
                .filter(val -> val != null)
                .mapToInt(Integer::intValue)
                .sum();

        Integer criticalAlerts = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getCriticalAlerts)
                .filter(val -> val != null)
                .mapToInt(Integer::intValue)
                .sum();

        Integer warningAlerts = serverRoomStats.stream()
                .map(ServerRoomStatisticsDto::getWarningAlerts)
                .filter(val -> val != null)
                .mapToInt(Integer::intValue)
                .sum();

        // ❌ 10. 전력 통계 집계 - 삭제됨

        // 11. 서버실별 요약 생성
        List<DataCenterStatisticsDto.ServerRoomSummaryDto> serverRoomSummaries = serverRoomStats.stream()
                .map(stats -> DataCenterStatisticsDto.ServerRoomSummaryDto.builder()
                        .serverRoomId(stats.getServerRoomId())
                        .serverRoomName(stats.getServerRoomName())
                        .equipmentCount(stats.getTotalEquipments())
                        .avgCpuUsage(stats.getAvgCpuUsage())
                        .avgMemoryUsage(stats.getAvgMemoryUsage())
                        .avgDiskUsage(stats.getAvgDiskUsage())
                        .avgTemperature(stats.getAvgTemperature())
                        .alertCount(stats.getTotalAlerts())
                        .build())
                .collect(Collectors.toList());

        // 12. 통계 DTO 생성
        return DataCenterStatisticsDto.builder()
                .dataCenterId(dataCenterId)
                .dataCenterName(dataCenter.getName())
                .timestamp(now)
                // 서버실 통계
                .totalServerRooms(totalServerRooms)
                .activeServerRooms(activeServerRooms)
                // 랙 통계
                .totalRacks(totalRacks)
                .activeRacks(activeRacks)
                // 장비 통계
                .totalEquipments(totalEquipments)
                .activeEquipments(activeEquipments)
                .inactiveEquipments(inactiveEquipments)
                // CPU 통계
                .avgCpuUsage(avgCpuUsage)
                .maxCpuUsage(maxCpuUsage)
                .minCpuUsage(minCpuUsage)
                .avgLoadAvg1(avgLoadAvg1)
                // 메모리 통계
                .avgMemoryUsage(avgMemoryUsage)
                .maxMemoryUsage(maxMemoryUsage)
                .minMemoryUsage(minMemoryUsage)
                .totalMemoryBytes(totalMemoryBytes)
                .usedMemoryBytes(usedMemoryBytes)
                .avgSwapUsage(avgSwapUsage)
                // 디스크 통계
                .avgDiskUsage(avgDiskUsage)
                .maxDiskUsage(maxDiskUsage)
                .minDiskUsage(minDiskUsage)
                .totalDiskBytes(totalDiskBytes)
                .usedDiskBytes(usedDiskBytes)
                .avgDiskIoUsage(avgDiskIoUsage)
                // 네트워크 통계
                .totalInBps(totalInBps)
                .totalOutBps(totalOutBps)
                .avgRxUsage(avgRxUsage)
                .avgTxUsage(avgTxUsage)
                .totalInErrors(totalInErrors)
                .totalOutErrors(totalOutErrors)
                // 환경 통계
                .avgTemperature(avgTemperature)
                .maxTemperature(maxTemperature)
                .minTemperature(minTemperature)
                .avgHumidity(avgHumidity)
                .maxHumidity(maxHumidity)
                .minHumidity(minHumidity)
                .temperatureWarnings(temperatureWarnings)
                .humidityWarnings(humidityWarnings)
                // 알람 통계
                .totalAlerts(totalAlerts)
                .criticalAlerts(criticalAlerts)
                .warningAlerts(warningAlerts)
                // ❌ 전력 통계 삭제됨
                // .totalPowerUsage(totalPowerUsage)
                // .avgPowerUsagePerRack(avgPowerUsagePerRack)
                // 서버실별 요약
                .serverRoomSummaries(serverRoomSummaries)
                .build();
    }

    /**
     * 빈 통계 생성 (서버실이 없는 경우)
     */
    private DataCenterStatisticsDto createEmptyStatistics(DataCenter dataCenter, LocalDateTime now) {
        return DataCenterStatisticsDto.builder()
                .dataCenterId(dataCenter.getId())
                .dataCenterName(dataCenter.getName())
                .timestamp(now)
                .totalServerRooms(0)
                .activeServerRooms(0)
                .totalRacks(0)
                .activeRacks(0)
                .totalEquipments(0)
                .activeEquipments(0)
                .inactiveEquipments(0)
                .totalAlerts(0)
                .criticalAlerts(0)
                .warningAlerts(0)
                .build();
    }
}