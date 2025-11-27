/**
 * 작성자: 황요한
 * 다양한 모니터링 메트릭을 평가하여 알림을 생성하는 서비스
 */
package org.example.finalbe.domains.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.alert.domain.AlertViolationTracker;
import org.example.finalbe.domains.alert.dto.AlertSettingsDto;
import org.example.finalbe.domains.alert.repository.AlertHistoryRepository;
import org.example.finalbe.domains.alert.repository.AlertSettingsRepository;
import org.example.finalbe.domains.alert.repository.AlertViolationTrackerRepository;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.*;
import org.example.finalbe.domains.monitoring.dto.DataCenterStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.ServerRoomStatisticsDto;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEvaluationService {

    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertViolationTrackerRepository violationTrackerRepository;
    private final AlertSettingsRepository alertSettingsRepository;
    private final AlertNotificationService alertNotificationService;

    // System(CPU, Memory) 메트릭 평가
    @Async("alertExecutor")
    public void evaluateSystemMetric(SystemMetric metric) {
        if (metric == null || metric.getEquipmentId() == null) return;

        try {
            Equipment equipment = equipmentRepository.findById(metric.getEquipmentId()).orElse(null);
            if (equipment == null || !Boolean.TRUE.equals(equipment.getMonitoringEnabled())) return;

            if (equipment.getCpuThresholdWarning() != null && metric.getCpuIdle() != null) {
                double cpuUsage = 100.0 - metric.getCpuIdle();
                evaluateMetric(
                        TargetType.EQUIPMENT, equipment.getId(), equipment.getName(),
                        MetricType.CPU, "cpu_usage_percent",
                        cpuUsage,
                        equipment.getCpuThresholdWarning().doubleValue(),
                        equipment.getCpuThresholdCritical() != null ? equipment.getCpuThresholdCritical().doubleValue() : null,
                        metric.getGenerateTime()
                );
            }

            if (equipment.getMemoryThresholdWarning() != null && metric.getUsedMemoryPercentage() != null) {
                evaluateMetric(
                        TargetType.EQUIPMENT, equipment.getId(), equipment.getName(),
                        MetricType.MEMORY, "memory_usage_percent",
                        metric.getUsedMemoryPercentage(),
                        equipment.getMemoryThresholdWarning().doubleValue(),
                        equipment.getMemoryThresholdCritical() != null ? equipment.getMemoryThresholdCritical().doubleValue() : null,
                        metric.getGenerateTime()
                );
            }

        } catch (Exception e) {
            log.error("System 메트릭 평가 실패: equipmentId={}", metric.getEquipmentId(), e);
        }
    }

    // Disk 메트릭 평가
    @Async("alertExecutor")
    public void evaluateDiskMetric(DiskMetric metric) {
        if (metric == null || metric.getEquipmentId() == null) return;

        try {
            Equipment equipment = equipmentRepository.findById(metric.getEquipmentId()).orElse(null);
            if (equipment == null || !Boolean.TRUE.equals(equipment.getMonitoringEnabled())) return;

            if (equipment.getDiskThresholdWarning() != null && metric.getUsedPercentage() != null) {
                evaluateMetric(
                        TargetType.EQUIPMENT, equipment.getId(), equipment.getName(),
                        MetricType.DISK, "disk_usage_percent",
                        metric.getUsedPercentage(),
                        equipment.getDiskThresholdWarning().doubleValue(),
                        equipment.getDiskThresholdCritical() != null ? equipment.getDiskThresholdCritical().doubleValue() : null,
                        metric.getGenerateTime()
                );
            }

        } catch (Exception e) {
            log.error("Disk 메트릭 평가 실패: equipmentId={}", metric.getEquipmentId(), e);
        }
    }

    // Network 메트릭 평가
    @Async("alertExecutor")
    public void evaluateNetworkMetric(NetworkMetric metric) {
        if (metric == null || metric.getEquipmentId() == null) return;

        try {
            Equipment equipment = equipmentRepository.findById(metric.getEquipmentId()).orElse(null);
            if (equipment == null || !Boolean.TRUE.equals(equipment.getMonitoringEnabled())) return;

            AlertSettingsDto settings = getAlertSettings();

            if (metric.getInErrorPktsTot() != null && metric.getInPktsTot() != null && metric.getInPktsTot() > 0) {
                double errorRate = (metric.getInErrorPktsTot().doubleValue() / metric.getInPktsTot()) * 100.0;
                evaluateNetworkErrorRate(equipment, "rx_error_rate", errorRate, metric.getNicName(), metric.getGenerateTime(), settings);
            }

            if (metric.getOutErrorPktsTot() != null && metric.getOutPktsTot() != null && metric.getOutPktsTot() > 0) {
                double errorRate = (metric.getOutErrorPktsTot().doubleValue() / metric.getOutPktsTot()) * 100.0;
                evaluateNetworkErrorRate(equipment, "tx_error_rate", errorRate, metric.getNicName(), metric.getGenerateTime(), settings);
            }

            if (metric.getInDiscardPktsTot() != null && metric.getInPktsTot() != null && metric.getInPktsTot() > 0) {
                double dropRate = (metric.getInDiscardPktsTot().doubleValue() / metric.getInPktsTot()) * 100.0;
                evaluateNetworkDropRate(equipment, "rx_drop_rate", dropRate, metric.getNicName(), metric.getGenerateTime(), settings);
            }

            if (metric.getOutDiscardPktsTot() != null && metric.getOutPktsTot() != null && metric.getOutPktsTot() > 0) {
                double dropRate = (metric.getOutDiscardPktsTot().doubleValue() / metric.getOutPktsTot()) * 100.0;
                evaluateNetworkDropRate(equipment, "tx_drop_rate", dropRate, metric.getNicName(), metric.getGenerateTime(), settings);
            }

        } catch (Exception e) {
            log.error("Network 메트릭 평가 실패: equipmentId={}", metric.getEquipmentId(), e);
        }
    }

    // 네트워크 에러율
    private void evaluateNetworkErrorRate(
            Equipment equipment, String baseMetricName, Double errorRate,
            String nicName, LocalDateTime time, AlertSettingsDto settings) {

        String metricName = baseMetricName + "_" + nicName;

        evaluateMetric(
                TargetType.EQUIPMENT, equipment.getId(), equipment.getName() + " [" + nicName + "]",
                MetricType.NETWORK, metricName, errorRate,
                settings.networkErrorRateWarning(), settings.networkErrorRateCritical(),
                time
        );
    }

    // 네트워크 드롭율
    private void evaluateNetworkDropRate(
            Equipment equipment, String baseMetricName, Double dropRate,
            String nicName, LocalDateTime time, AlertSettingsDto settings) {

        String metricName = baseMetricName + "_" + nicName;

        evaluateMetric(
                TargetType.EQUIPMENT, equipment.getId(), equipment.getName() + " [" + nicName + "]",
                MetricType.NETWORK, metricName, dropRate,
                settings.networkDropRateWarning(), settings.networkDropRateCritical(),
                time
        );
    }

    // Rack 환경 메트릭 평가
    @Async("alertExecutor")
    public void evaluateEnvironmentMetric(EnvironmentMetric metric) {
        if (metric == null || metric.getRackId() == null) return;

        try {
            Rack rack = rackRepository.findById(metric.getRackId()).orElse(null);
            if (rack == null || !Boolean.TRUE.equals(rack.getMonitoringEnabled())) return;

            if (rack.getTemperatureThresholdWarning() != null && metric.getTemperature() != null) {
                evaluateMetric(
                        TargetType.RACK, rack.getId(), rack.getRackName(),
                        MetricType.TEMPERATURE, "temperature",
                        metric.getTemperature(),
                        rack.getTemperatureThresholdWarning().doubleValue(),
                        rack.getTemperatureThresholdCritical() != null ? rack.getTemperatureThresholdCritical().doubleValue() : null,
                        metric.getGenerateTime()
                );
            }

            if (metric.getHumidity() != null) {
                double humidity = metric.getHumidity();

                if (rack.getHumidityThresholdMinWarning() != null && humidity < rack.getHumidityThresholdMinWarning()) {
                    AlertLevel level = AlertLevel.WARNING;
                    double threshold = rack.getHumidityThresholdMinWarning();

                    if (rack.getHumidityThresholdMinCritical() != null && humidity < rack.getHumidityThresholdMinCritical()) {
                        level = AlertLevel.CRITICAL;
                        threshold = rack.getHumidityThresholdMinCritical();
                    }

                    handleViolationDirect(
                            TargetType.RACK, rack.getId(), rack.getRackName(),
                            MetricType.HUMIDITY, "humidity_min",
                            level, humidity, threshold, metric.getGenerateTime()
                    );
                }

                if (rack.getHumidityThresholdMaxWarning() != null && humidity > rack.getHumidityThresholdMaxWarning()) {
                    AlertLevel level = AlertLevel.WARNING;
                    double threshold = rack.getHumidityThresholdMaxWarning();

                    if (rack.getHumidityThresholdMaxCritical() != null && humidity > rack.getHumidityThresholdMaxCritical()) {
                        level = AlertLevel.CRITICAL;
                        threshold = rack.getHumidityThresholdMaxCritical();
                    }

                    handleViolationDirect(
                            TargetType.RACK, rack.getId(), rack.getRackName(),
                            MetricType.HUMIDITY, "humidity_max",
                            level, humidity, threshold, metric.getGenerateTime()
                    );
                }
            }

        } catch (Exception e) {
            log.error("Environment 메트릭 평가 실패: rackId={}", metric.getRackId(), e);
        }
    }

    // ServerRoom 통계 메트릭 평가
    @Async("alertExecutor")
    public void evaluateServerRoomStatistics(ServerRoomStatisticsDto stats) {
        if (stats == null || stats.getServerRoomId() == null) return;

        try {
            ServerRoom serverRoom = serverRoomRepository.findById(stats.getServerRoomId()).orElse(null);
            if (serverRoom == null || !Boolean.TRUE.equals(serverRoom.getMonitoringEnabled())) return;

            LocalDateTime now = LocalDateTime.now();

            if (serverRoom.getAvgCpuThresholdWarning() != null && stats.getAvgCpuUsage() != null) {
                evaluateMetric(
                        TargetType.SERVER_ROOM, serverRoom.getId(), serverRoom.getName(),
                        MetricType.CPU, "avg_cpu",
                        stats.getAvgCpuUsage(),
                        serverRoom.getAvgCpuThresholdWarning().doubleValue(),
                        serverRoom.getAvgCpuThresholdCritical() != null ? serverRoom.getAvgCpuThresholdCritical().doubleValue() : null,
                        now
                );
            }

            if (serverRoom.getAvgMemoryThresholdWarning() != null && stats.getAvgMemoryUsage() != null) {
                evaluateMetric(
                        TargetType.SERVER_ROOM, serverRoom.getId(), serverRoom.getName(),
                        MetricType.MEMORY, "avg_memory",
                        stats.getAvgMemoryUsage(),
                        serverRoom.getAvgMemoryThresholdWarning().doubleValue(),
                        serverRoom.getAvgMemoryThresholdCritical() != null ? serverRoom.getAvgMemoryThresholdCritical().doubleValue() : null,
                        now
                );
            }

            if (serverRoom.getAvgDiskThresholdWarning() != null && stats.getAvgDiskUsage() != null) {
                evaluateMetric(
                        TargetType.SERVER_ROOM, serverRoom.getId(), serverRoom.getName(),
                        MetricType.DISK, "avg_disk",
                        stats.getAvgDiskUsage(),
                        serverRoom.getAvgDiskThresholdWarning().doubleValue(),
                        serverRoom.getAvgDiskThresholdCritical() != null ? serverRoom.getAvgDiskThresholdCritical().doubleValue() : null,
                        now
                );
            }

            if (serverRoom.getAvgTemperatureThresholdWarning() != null && stats.getAvgTemperature() != null) {
                evaluateMetric(
                        TargetType.SERVER_ROOM, serverRoom.getId(), serverRoom.getName(),
                        MetricType.TEMPERATURE, "avg_temperature",
                        stats.getAvgTemperature(),
                        serverRoom.getAvgTemperatureThresholdWarning().doubleValue(),
                        serverRoom.getAvgTemperatureThresholdCritical() != null ? serverRoom.getAvgTemperatureThresholdCritical().doubleValue() : null,
                        now
                );
            }

        } catch (Exception e) {
            log.error("ServerRoom 통계 평가 실패: serverRoomId={}", stats.getServerRoomId(), e);
        }
    }

    // DataCenter 통계는 현재 비활성화됨
    @Async("alertExecutor")
    public void evaluateDataCenterStatistics(DataCenterStatisticsDto stats) {
        log.debug("DataCenter 알림 평가는 비활성화됨. dataCenterId={}",
                stats != null ? stats.getDataCenterId() : null);
    }

    // 공통 메트릭 평가
    private void evaluateMetric(
            TargetType targetType, Long targetId, String targetName,
            MetricType metricType, String metricName, Double measuredValue,
            Double warningThreshold, Double criticalThreshold,
            LocalDateTime triggerTime) {

        if (measuredValue == null || warningThreshold == null) return;
        if (measuredValue < warningThreshold) return;

        AlertLevel level = (criticalThreshold != null && measuredValue >= criticalThreshold)
                ? AlertLevel.CRITICAL : AlertLevel.WARNING;

        Double thresholdValue = (level == AlertLevel.CRITICAL && criticalThreshold != null)
                ? criticalThreshold : warningThreshold;

        sendAlert(targetType, targetId, targetName,
                level, metricType, metricName,
                measuredValue, thresholdValue, triggerTime);
    }

    // 알림 생성 & 전송
    private void sendAlert(
            TargetType targetType, Long targetId, String targetName,
            AlertLevel level, MetricType metricType, String metricName,
            Double measuredValue, Double thresholdValue, LocalDateTime time) {

        AlertHistory alert = AlertHistory.builder()
                .targetType(targetType)
                .targetName(targetName)
                .metricType(metricType)
                .metricName(metricName)
                .level(level)
                .measuredValue(measuredValue)
                .thresholdValue(thresholdValue)
                .triggeredAt(time)
                .message(buildAlertMessage(targetType, targetName, level, metricType, measuredValue, thresholdValue))
                .build();

        boolean proceed = populateHierarchyIds(alert, targetType, targetId);
        if (!proceed) return;

        alertHistoryRepository.save(alert);
        alertNotificationService.sendAlert(alert);

        log.warn("🚨 알림 발생 - {} [{}] {}:{} (현재: {:.1f}, 임계치: {:.0f})",
                level.name(), metricType.name(), targetName, metricName, measuredValue, thresholdValue);
    }

    // ID 계층 정보 채우기 + 삭제된 서버실 필터링
    private boolean populateHierarchyIds(AlertHistory alert, TargetType targetType, Long targetId) {
        switch (targetType) {
            case EQUIPMENT -> {
                alert.setEquipmentId(targetId);
                equipmentRepository.findByIdWithFullHierarchy(targetId).ifPresent(eq -> {
                    if (eq.getRack() != null) {
                        Rack rack = eq.getRack();
                        alert.setRackId(rack.getId());

                        if (rack.getServerRoom() != null) {
                            ServerRoom sr = rack.getServerRoom();
                            if (sr.getDelYn() == DelYN.Y) return;
                            alert.setServerRoomId(sr.getId());
                            if (sr.getDataCenter() != null) alert.setDataCenterId(sr.getDataCenter().getId());
                        }
                    }
                });

                if (alert.getServerRoomId() != null) {
                    Optional<ServerRoom> room = serverRoomRepository.findById(alert.getServerRoomId());
                    if (room.isPresent() && room.get().getDelYn() == DelYN.Y) return false;
                }
            }
            case RACK -> {
                alert.setRackId(targetId);
                rackRepository.findByIdWithServerRoomAndDataCenter(targetId).ifPresent(rack -> {
                    if (rack.getServerRoom() != null) {
                        ServerRoom sr = rack.getServerRoom();
                        if (sr.getDelYn() == DelYN.Y) return;
                        alert.setServerRoomId(sr.getId());
                        if (sr.getDataCenter() != null) alert.setDataCenterId(sr.getDataCenter().getId());
                    }
                });

                if (alert.getServerRoomId() != null) {
                    Optional<ServerRoom> room = serverRoomRepository.findById(alert.getServerRoomId());
                    if (room.isPresent() && room.get().getDelYn() == DelYN.Y) return false;
                }
            }
            case SERVER_ROOM -> {
                alert.setServerRoomId(targetId);
                Optional<ServerRoom> room = serverRoomRepository.findById(targetId);
                if (room.isEmpty() || room.get().getDelYn() == DelYN.Y) return false;

                room.ifPresent(sr -> {
                    if (sr.getDataCenter() != null) alert.setDataCenterId(sr.getDataCenter().getId());
                });
            }
            case DATA_CENTER -> alert.setDataCenterId(targetId);
        }

        return true;
    }

    // 알림 메시지 생성
    private String buildAlertMessage(TargetType targetType, String targetName,
                                     AlertLevel level, MetricType metricType,
                                     Double measuredValue, Double thresholdValue) {

        String levelText = level == AlertLevel.CRITICAL ? "위험" : "경고";

        return String.format("[%s] %s %s이(가) %s 임계치 %.0f을 초과했습니다. (현재 %.1f)",
                targetType.getDescription(), targetName, metricType.getDescription(),
                levelText, thresholdValue, measuredValue);
    }

    // 설정 조회
    private AlertSettingsDto getAlertSettings() {
        return alertSettingsRepository.findById(1L)
                .map(AlertSettingsDto::from)
                .orElseGet(AlertSettingsDto::getDefault);
    }

    // Tracker 조회 또는 생성
    private AlertViolationTracker getOrCreateTracker(
            TargetType targetType, Long targetId,
            MetricType metricType, String metricName) {

        Optional<AlertViolationTracker> found = switch (targetType) {
            case EQUIPMENT -> violationTrackerRepository.findByEquipmentIdAndMetric(targetId, metricType, metricName);
            case RACK -> violationTrackerRepository.findByRackIdAndMetric(targetId, metricType, metricName);
            case SERVER_ROOM -> violationTrackerRepository.findByServerRoomIdAndMetric(targetId, metricType, metricName);
            case DATA_CENTER -> violationTrackerRepository.findByDataCenterIdAndMetric(targetId, metricType, metricName);
        };

        return found.orElseGet(() -> {
            try {
                AlertViolationTracker tracker = AlertViolationTracker.builder()
                        .targetType(targetType)
                        .metricType(metricType)
                        .metricName(metricName)
                        .consecutiveViolations(0)
                        .lastViolationTime(LocalDateTime.now())
                        .build();

                switch (targetType) {
                    case EQUIPMENT -> tracker.setEquipmentId(targetId);
                    case RACK -> tracker.setRackId(targetId);
                    case SERVER_ROOM -> tracker.setServerRoomId(targetId);
                    case DATA_CENTER -> tracker.setDataCenterId(targetId);
                }

                return violationTrackerRepository.save(tracker);

            } catch (DataIntegrityViolationException e) {
                log.warn("중복 Tracker 감지 → 재조회: targetType={}, targetId={}, metric={}", targetType, targetId, metricName);

                return switch (targetType) {
                    case EQUIPMENT -> violationTrackerRepository
                            .findByEquipmentIdAndMetric(targetId, metricType, metricName).orElseThrow();
                    case RACK -> violationTrackerRepository
                            .findByRackIdAndMetric(targetId, metricType, metricName).orElseThrow();
                    case SERVER_ROOM -> violationTrackerRepository
                            .findByServerRoomIdAndMetric(targetId, metricType, metricName).orElseThrow();
                    case DATA_CENTER -> violationTrackerRepository
                            .findByDataCenterIdAndMetric(targetId, metricType, metricName).orElseThrow();
                };
            }
        });
    }

    // 위반 처리
    private void handleViolation(
            TargetType targetType, Long targetId, String targetName,
            AlertViolationTracker tracker, AlertLevel level,
            MetricType metricType, String metricName,
            Double measuredValue, Double thresholdValue, LocalDateTime time) {

        tracker.setConsecutiveViolations(tracker.getConsecutiveViolations() + 1);
        tracker.setLastViolationTime(time);
        tracker.setLastMeasuredValue(measuredValue);
        tracker.setUpdatedAt(LocalDateTime.now());
        violationTrackerRepository.save(tracker);

        AlertSettingsDto settings = getAlertSettings();

        if (tracker.getConsecutiveViolations() >= settings.defaultConsecutiveCount()) {
            if (shouldSendAlert(tracker, settings)) {
                sendAlert(targetType, targetId, targetName, level, metricType, metricName,
                        measuredValue, thresholdValue, time);
                tracker.setLastAlertSentAt(time);
                violationTrackerRepository.save(tracker);
            }
        }
    }

    // 즉시 위반 처리 (습도 등)
    private void handleViolationDirect(
            TargetType targetType, Long targetId, String targetName,
            MetricType metricType, String metricName, AlertLevel level,
            Double measuredValue, Double thresholdValue, LocalDateTime time) {

        AlertViolationTracker tracker = getOrCreateTracker(targetType, targetId, metricType, metricName);
        handleViolation(targetType, targetId, targetName, tracker,
                level, metricType, metricName, measuredValue, thresholdValue, time);
    }

    // 쿨다운 검사
    private boolean shouldSendAlert(AlertViolationTracker tracker, AlertSettingsDto settings) {
        if (tracker.getLastAlertSentAt() == null) return true;

        LocalDateTime cooldownEnd = tracker.getLastAlertSentAt().plusMinutes(settings.defaultCooldownMinutes());
        return LocalDateTime.now().isAfter(cooldownEnd);
    }
}
