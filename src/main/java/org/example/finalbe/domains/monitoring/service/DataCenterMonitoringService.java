/**
 * 작성자: 황요한
 * 데이터센터 내 모든 서버실의 통계를 집계하는 서비스
 */
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataCenterMonitoringService {

    private final DataCenterRepository dataCenterRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final ServerRoomMonitoringService serverRoomMonitoringService;

    /** 데이터센터 실시간 통계 계산 */
    public DataCenterStatisticsDto calculateDataCenterStatistics(Long dataCenterId) {
        log.debug("📊 데이터센터 통계 계산 시작: dataCenterId={}", dataCenterId);

        DataCenter dataCenter = dataCenterRepository.findById(dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("데이터센터를 찾을 수 없습니다: " + dataCenterId));

        LocalDateTime now = LocalDateTime.now();
        List<ServerRoom> serverRooms = serverRoomRepository
                .findByDataCenterIdAndDelYn(dataCenterId, DelYN.N);

        if (serverRooms.isEmpty()) {
            log.warn("⚠️ 데이터센터에 서버실이 없습니다: {}", dataCenterId);
            return createEmptyStatistics(dataCenter, now);
        }

        List<ServerRoomStatisticsDto> serverRoomStats = serverRooms.stream()
                .map(room -> {
                    try {
                        return serverRoomMonitoringService.calculateServerRoomStatistics(room.getId());
                    } catch (Exception e) {
                        log.error("서버실 통계 계산 실패: {}", room.getId(), e);
                        return null;
                    }
                })
                .filter(stats -> stats != null)
                .collect(Collectors.toList());

        if (serverRoomStats.isEmpty()) {
            return createEmptyStatistics(dataCenter, now);
        }

        // 서버실 및 랙/장비 통계
        int totalServerRooms = serverRooms.size();
        int activeServerRooms = (int) serverRoomStats.stream()
                .filter(s -> s.getActiveEquipments() > 0).count();

        int totalRacks = serverRoomStats.stream().mapToInt(ServerRoomStatisticsDto::getTotalRacks).sum();
        int activeRacks = serverRoomStats.stream().mapToInt(ServerRoomStatisticsDto::getActiveRacks).sum();

        int totalEquipments = serverRoomStats.stream().mapToInt(ServerRoomStatisticsDto::getTotalEquipments).sum();
        int activeEquipments = serverRoomStats.stream().mapToInt(ServerRoomStatisticsDto::getActiveEquipments).sum();
        int inactiveEquipments = serverRoomStats.stream().mapToInt(ServerRoomStatisticsDto::getInactiveEquipments).sum();

        // CPU 통계
        Double avgCpuUsage = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgCpuUsage);
        Double maxCpuUsage = max(serverRoomStats, ServerRoomStatisticsDto::getMaxCpuUsage);
        Double minCpuUsage = min(serverRoomStats, ServerRoomStatisticsDto::getMinCpuUsage);

        Double avgLoadAvg1 = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgLoadAvg1);
        Double avgLoadAvg5 = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgLoadAvg5);
        Double avgLoadAvg15 = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgLoadAvg15);

        // 메모리 통계
        Double avgMemoryUsage = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgMemoryUsage);
        Double maxMemoryUsage = max(serverRoomStats, ServerRoomStatisticsDto::getMaxMemoryUsage);
        Double minMemoryUsage = min(serverRoomStats, ServerRoomStatisticsDto::getMinMemoryUsage);

        Long totalMemoryBytes = sumLong(serverRoomStats, ServerRoomStatisticsDto::getTotalMemoryBytes);
        Long usedMemoryBytes = sumLong(serverRoomStats, ServerRoomStatisticsDto::getUsedMemoryBytes);

        Double avgSwapUsage = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgSwapUsage);

        // 디스크 통계
        Double avgDiskUsage = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgDiskUsage);
        Double maxDiskUsage = max(serverRoomStats, ServerRoomStatisticsDto::getMaxDiskUsage);
        Double minDiskUsage = min(serverRoomStats, ServerRoomStatisticsDto::getMinDiskUsage);

        Long totalDiskBytes = sumLong(serverRoomStats, ServerRoomStatisticsDto::getTotalDiskBytes);
        Long usedDiskBytes = sumLong(serverRoomStats, ServerRoomStatisticsDto::getUsedDiskBytes);

        Double avgDiskIoUsage = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgDiskIoUsage);

        // 네트워크 통계
        Double totalInBps = sum(serverRoomStats, ServerRoomStatisticsDto::getTotalInBps);
        Double totalOutBps = sum(serverRoomStats, ServerRoomStatisticsDto::getTotalOutBps);
        Double avgRxUsage = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgRxUsage);
        Double avgTxUsage = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgTxUsage);

        Long totalInErrors = sumLong(serverRoomStats, ServerRoomStatisticsDto::getTotalInErrors);
        Long totalOutErrors = sumLong(serverRoomStats, ServerRoomStatisticsDto::getTotalOutErrors);

        // 환경 통계
        Double avgTemperature = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgTemperature);
        Double maxTemperature = max(serverRoomStats, ServerRoomStatisticsDto::getMaxTemperature);
        Double minTemperature = min(serverRoomStats, ServerRoomStatisticsDto::getMinTemperature);

        Double avgHumidity = avg(serverRoomStats, ServerRoomStatisticsDto::getAvgHumidity);
        Double maxHumidity = max(serverRoomStats, ServerRoomStatisticsDto::getMaxHumidity);
        Double minHumidity = min(serverRoomStats, ServerRoomStatisticsDto::getMinHumidity);

        Integer temperatureWarnings = sumInt(serverRoomStats, ServerRoomStatisticsDto::getTemperatureWarnings);
        Integer humidityWarnings = sumInt(serverRoomStats, ServerRoomStatisticsDto::getHumidityWarnings);

        // 알람 통계
        Integer totalAlerts = sumInt(serverRoomStats, ServerRoomStatisticsDto::getTotalAlerts);
        Integer criticalAlerts = sumInt(serverRoomStats, ServerRoomStatisticsDto::getCriticalAlerts);
        Integer warningAlerts = sumInt(serverRoomStats, ServerRoomStatisticsDto::getWarningAlerts);

        // 서버실 요약 리스트
        List<DataCenterStatisticsDto.ServerRoomSummaryDto> summaries = serverRoomStats.stream()
                .map(s -> DataCenterStatisticsDto.ServerRoomSummaryDto.builder()
                        .serverRoomId(s.getServerRoomId())
                        .serverRoomName(s.getServerRoomName())
                        .equipmentCount(s.getTotalEquipments())
                        .avgCpuUsage(s.getAvgCpuUsage())
                        .avgMemoryUsage(s.getAvgMemoryUsage())
                        .avgDiskUsage(s.getAvgDiskUsage())
                        .avgTemperature(s.getAvgTemperature())
                        .alertCount(s.getTotalAlerts())
                        .build())
                .collect(Collectors.toList());

        // 최종 DTO 구성
        return DataCenterStatisticsDto.builder()
                .dataCenterId(dataCenterId)
                .dataCenterName(dataCenter.getName())
                .timestamp(now)
                .totalServerRooms(totalServerRooms)
                .activeServerRooms(activeServerRooms)
                .totalRacks(totalRacks)
                .activeRacks(activeRacks)
                .totalEquipments(totalEquipments)
                .activeEquipments(activeEquipments)
                .inactiveEquipments(inactiveEquipments)
                .avgCpuUsage(avgCpuUsage)
                .maxCpuUsage(maxCpuUsage)
                .minCpuUsage(minCpuUsage)
                .avgLoadAvg1(avgLoadAvg1)
                .avgLoadAvg5(avgLoadAvg5)
                .avgLoadAvg15(avgLoadAvg15)
                .avgMemoryUsage(avgMemoryUsage)
                .maxMemoryUsage(maxMemoryUsage)
                .minMemoryUsage(minMemoryUsage)
                .totalMemoryBytes(totalMemoryBytes)
                .usedMemoryBytes(usedMemoryBytes)
                .avgSwapUsage(avgSwapUsage)
                .avgDiskUsage(avgDiskUsage)
                .maxDiskUsage(maxDiskUsage)
                .minDiskUsage(minDiskUsage)
                .totalDiskBytes(totalDiskBytes)
                .usedDiskBytes(usedDiskBytes)
                .avgDiskIoUsage(avgDiskIoUsage)
                .totalInBps(totalInBps)
                .totalOutBps(totalOutBps)
                .avgRxUsage(avgRxUsage)
                .avgTxUsage(avgTxUsage)
                .totalInErrors(totalInErrors)
                .totalOutErrors(totalOutErrors)
                .avgTemperature(avgTemperature)
                .maxTemperature(maxTemperature)
                .minTemperature(minTemperature)
                .avgHumidity(avgHumidity)
                .maxHumidity(maxHumidity)
                .minHumidity(minHumidity)
                .temperatureWarnings(temperatureWarnings)
                .humidityWarnings(humidityWarnings)
                .totalAlerts(totalAlerts)
                .criticalAlerts(criticalAlerts)
                .warningAlerts(warningAlerts)
                .serverRoomSummaries(summaries)
                .build();
    }

    /** 서버실이 없는 경우 빈 통계 반환 */
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

    // ===== 공통 계산 유틸 =====
    private Double avg(List<ServerRoomStatisticsDto> list, java.util.function.Function<ServerRoomStatisticsDto, Double> f) {
        return list.stream().map(f).filter(v -> v != null).mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Double max(List<ServerRoomStatisticsDto> list, java.util.function.Function<ServerRoomStatisticsDto, Double> f) {
        return list.stream().map(f).filter(v -> v != null).mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    private Double min(List<ServerRoomStatisticsDto> list, java.util.function.Function<ServerRoomStatisticsDto, Double> f) {
        return list.stream().map(f).filter(v -> v != null).mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    private Double sum(List<ServerRoomStatisticsDto> list, java.util.function.Function<ServerRoomStatisticsDto, Double> f) {
        return list.stream().map(f).filter(v -> v != null).mapToDouble(Double::doubleValue).sum();
    }

    private Long sumLong(List<ServerRoomStatisticsDto> list, java.util.function.Function<ServerRoomStatisticsDto, Long> f) {
        return list.stream().map(f).filter(v -> v != null).mapToLong(Long::longValue).sum();
    }

    private Integer sumInt(List<ServerRoomStatisticsDto> list, java.util.function.Function<ServerRoomStatisticsDto, Integer> f) {
        return list.stream().map(f).filter(v -> v != null).mapToInt(Integer::intValue).sum();
    }
}
