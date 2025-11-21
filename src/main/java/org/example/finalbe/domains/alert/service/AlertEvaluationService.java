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
import org.example.finalbe.domains.common.enumdir.AlertStatus;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.domain.*;
import org.example.finalbe.domains.monitoring.dto.DataCenterStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.ServerRoomStatisticsDto;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEvaluationService {

    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final DataCenterRepository dataCenterRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertViolationTrackerRepository violationTrackerRepository;
    private final AlertSettingsRepository alertSettingsRepository;
    private final AlertNotificationService alertNotificationService;

    /**
     * System 메트릭 평가 (CPU, Memory)
     */
    @Async("alertExecutor")
    @Transactional
    public void evaluateSystemMetric(SystemMetric metric) {
        if (metric == null || metric.getEquipmentId() == null) {
            return;
        }

        try {
            Equipment equipment = equipmentRepository.findById(metric.getEquipmentId())
                    .orElse(null);

            if (equipment == null || !Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
                return;
            }

            // ✅ CPU 평가 - cpuIdle 기반으로 사용률 계산
            if (equipment.getCpuThresholdWarning() != null && metric.getCpuIdle() != null) {
                Double cpuUsage = 100.0 - metric.getCpuIdle();

                evaluateMetric(
                        TargetType.EQUIPMENT,
                        equipment.getId(),
                        equipment.getName(),
                        MetricType.CPU,
                        "cpu_usage_percent",
                        cpuUsage,
                        equipment.getCpuThresholdWarning().doubleValue(),
                        equipment.getCpuThresholdCritical() != null ?
                                equipment.getCpuThresholdCritical().doubleValue() : null,
                        metric.getGenerateTime()
                );
            }

            // ✅ Memory 평가 - usedMemoryPercentage 사용
            if (equipment.getMemoryThresholdWarning() != null && metric.getUsedMemoryPercentage() != null) {
                evaluateMetric(
                        TargetType.EQUIPMENT,
                        equipment.getId(),
                        equipment.getName(),
                        MetricType.MEMORY,
                        "memory_usage_percent",
                        metric.getUsedMemoryPercentage(),
                        equipment.getMemoryThresholdWarning().doubleValue(),
                        equipment.getMemoryThresholdCritical() != null ?
                                equipment.getMemoryThresholdCritical().doubleValue() : null,
                        metric.getGenerateTime()
                );
            }

        } catch (Exception e) {
            log.error("❌ System 메트릭 알림 평가 실패: equipmentId={}", metric.getEquipmentId(), e);
        }
    }

    /**
     * Disk 메트릭 평가
     */
    @Async("alertExecutor")
    @Transactional
    public void evaluateDiskMetric(DiskMetric metric) {
        if (metric == null || metric.getEquipmentId() == null) {
            return;
        }

        try {
            Equipment equipment = equipmentRepository.findById(metric.getEquipmentId())
                    .orElse(null);

            if (equipment == null || !Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
                return;
            }

            if (equipment.getDiskThresholdWarning() != null && metric.getUsedPercentage() != null) {
                evaluateMetric(
                        TargetType.EQUIPMENT,
                        equipment.getId(),
                        equipment.getName(),
                        MetricType.DISK,
                        "disk_usage_percent",
                        metric.getUsedPercentage(),
                        equipment.getDiskThresholdWarning().doubleValue(),
                        equipment.getDiskThresholdCritical() != null ?
                                equipment.getDiskThresholdCritical().doubleValue() : null,
                        metric.getGenerateTime()
                );
            }

        } catch (Exception e) {
            log.error("❌ Disk 메트릭 알림 평가 실패: equipmentId={}", metric.getEquipmentId(), e);
        }
    }

    /**
     * Network 메트릭 평가
     */
    @Async("alertExecutor")
    @Transactional
    public void evaluateNetworkMetric(NetworkMetric metric) {
        if (metric == null || metric.getEquipmentId() == null) {
            return;
        }

        try {
            Equipment equipment = equipmentRepository.findById(metric.getEquipmentId())
                    .orElse(null);

            if (equipment == null || !Boolean.TRUE.equals(equipment.getMonitoringEnabled())) {
                return;
            }

            AlertSettingsDto settings = getAlertSettings();

            // RX 사용률 평가
            if (metric.getRxUsage() != null) {
                evaluateNetworkUsage(
                        equipment,
                        "rx_usage",
                        metric.getRxUsage(),
                        metric.getNicName(),
                        metric.getGenerateTime(),
                        settings
                );
            }

            // TX 사용률 평가
            if (metric.getTxUsage() != null) {
                evaluateNetworkUsage(
                        equipment,
                        "tx_usage",
                        metric.getTxUsage(),
                        metric.getNicName(),
                        metric.getGenerateTime(),
                        settings
                );
            }

            // 에러율 평가 (RX)
            if (metric.getInErrorPktsTot() != null && metric.getInPktsTot() != null &&
                    metric.getInPktsTot() > 0) {
                double errorRate = (metric.getInErrorPktsTot() * 100.0) / metric.getInPktsTot();
                evaluateNetworkErrorRate(
                        equipment,
                        "rx_error_rate",
                        errorRate,
                        metric.getNicName(),
                        metric.getGenerateTime(),
                        settings
                );
            }

            // 에러율 평가 (TX)
            if (metric.getOutErrorPktsTot() != null && metric.getOutPktsTot() != null &&
                    metric.getOutPktsTot() > 0) {
                double errorRate = (metric.getOutErrorPktsTot() * 100.0) / metric.getOutPktsTot();
                evaluateNetworkErrorRate(
                        equipment,
                        "tx_error_rate",
                        errorRate,
                        metric.getNicName(),
                        metric.getGenerateTime(),
                        settings
                );
            }

            // 드롭율 평가 (RX)
            if (metric.getInDiscardPktsTot() != null && metric.getInPktsTot() != null &&
                    metric.getInPktsTot() > 0) {
                double dropRate = (metric.getInDiscardPktsTot() * 100.0) / metric.getInPktsTot();
                evaluateNetworkDropRate(
                        equipment,
                        "rx_drop_rate",
                        dropRate,
                        metric.getNicName(),
                        metric.getGenerateTime(),
                        settings
                );
            }

            // 드롭율 평가 (TX)
            if (metric.getOutDiscardPktsTot() != null && metric.getOutPktsTot() != null &&
                    metric.getOutPktsTot() > 0) {
                double dropRate = (metric.getOutDiscardPktsTot() * 100.0) / metric.getOutPktsTot();
                evaluateNetworkDropRate(
                        equipment,
                        "tx_drop_rate",
                        dropRate,
                        metric.getNicName(),
                        metric.getGenerateTime(),
                        settings
                );
            }

        } catch (Exception e) {
            log.error("❌ Network 메트릭 알림 평가 실패: equipmentId={}, nic={}",
                    metric.getEquipmentId(), metric.getNicName(), e);
        }
    }

    /**
     * 네트워크 사용률 평가
     */
    private void evaluateNetworkUsage(
            Equipment equipment,
            String metricName,
            Double usage,
            String nicName,
            LocalDateTime generateTime,
            AlertSettingsDto settings) {

        // 임계값이 설정되지 않은 경우 기본값 사용 (80% 경고, 90% 위험)
        Double warningThreshold = 80.0;
        Double criticalThreshold = 90.0;

        evaluateMetric(
                TargetType.EQUIPMENT,
                equipment.getId(),
                equipment.getName() + " [" + nicName + "]",
                MetricType.NETWORK,
                metricName,
                usage,
                warningThreshold,
                criticalThreshold,
                generateTime
        );
    }

    /**
     * 네트워크 에러율 평가
     */
    private void evaluateNetworkErrorRate(
            Equipment equipment,
            String metricName,
            Double errorRate,
            String nicName,
            LocalDateTime generateTime,
            AlertSettingsDto settings) {

        evaluateMetric(
                TargetType.EQUIPMENT,
                equipment.getId(),
                equipment.getName() + " [" + nicName + "]",
                MetricType.NETWORK,
                metricName,
                errorRate,
                settings.networkErrorRateWarning(),
                settings.networkErrorRateCritical(),
                generateTime
        );
    }

    /**
     * 네트워크 드롭율 평가
     */
    private void evaluateNetworkDropRate(
            Equipment equipment,
            String metricName,
            Double dropRate,
            String nicName,
            LocalDateTime generateTime,
            AlertSettingsDto settings) {

        evaluateMetric(
                TargetType.EQUIPMENT,
                equipment.getId(),
                equipment.getName() + " [" + nicName + "]",
                MetricType.NETWORK,
                metricName,
                dropRate,
                settings.networkDropRateWarning(),
                settings.networkDropRateCritical(),
                generateTime
        );
    }

    /**
     * Environment 메트릭 평가 (Rack)
     */
    @Async("alertExecutor")
    @Transactional
    public void evaluateEnvironmentMetric(EnvironmentMetric metric) {
        if (metric == null || metric.getRackId() == null) {
            return;
        }

        try {
            Rack rack = rackRepository.findById(metric.getRackId())
                    .orElse(null);

            if (rack == null || !Boolean.TRUE.equals(rack.getMonitoringEnabled())) {
                return;
            }

            // 온도 평가
            if (rack.getTemperatureThresholdWarning() != null && metric.getTemperature() != null) {
                evaluateMetric(
                        TargetType.RACK,
                        rack.getId(),
                        rack.getRackName(),
                        MetricType.TEMPERATURE,
                        "temperature",
                        metric.getTemperature(),
                        rack.getTemperatureThresholdWarning().doubleValue(),
                        rack.getTemperatureThresholdCritical() != null ?
                                rack.getTemperatureThresholdCritical().doubleValue() : null,
                        metric.getGenerateTime()
                );
            }

            // 습도 평가
            if (metric.getHumidity() != null) {
                double humidity = metric.getHumidity();

                // 습도 최소값 체크
                if (rack.getHumidityThresholdMinWarning() != null &&
                        humidity < rack.getHumidityThresholdMinWarning()) {
                    AlertLevel level = AlertLevel.WARNING;
                    Double threshold = rack.getHumidityThresholdMinWarning().doubleValue();

                    if (rack.getHumidityThresholdMinCritical() != null &&
                            humidity < rack.getHumidityThresholdMinCritical()) {
                        level = AlertLevel.CRITICAL;
                        threshold = rack.getHumidityThresholdMinCritical().doubleValue();
                    }

                    handleViolationDirect(
                            TargetType.RACK, rack.getId(), rack.getRackName(),
                            MetricType.HUMIDITY, "humidity_min", level,
                            humidity, threshold, metric.getGenerateTime()
                    );
                }

                // 습도 최대값 체크
                if (rack.getHumidityThresholdMaxWarning() != null &&
                        humidity > rack.getHumidityThresholdMaxWarning()) {
                    AlertLevel level = AlertLevel.WARNING;
                    Double threshold = rack.getHumidityThresholdMaxWarning().doubleValue();

                    if (rack.getHumidityThresholdMaxCritical() != null &&
                            humidity > rack.getHumidityThresholdMaxCritical()) {
                        level = AlertLevel.CRITICAL;
                        threshold = rack.getHumidityThresholdMaxCritical().doubleValue();
                    }

                    handleViolationDirect(
                            TargetType.RACK, rack.getId(), rack.getRackName(),
                            MetricType.HUMIDITY, "humidity_max", level,
                            humidity, threshold, metric.getGenerateTime()
                    );
                }
            }

        } catch (Exception e) {
            log.error("❌ Environment 메트릭 알림 평가 실패: rackId={}", metric.getRackId(), e);
        }
    }

    /**
     * ServerRoom 통계 평가
     */
    @Async("alertExecutor")
    @Transactional
    public void evaluateServerRoomStatistics(ServerRoomStatisticsDto stats) {
        if (stats == null || stats.getServerRoomId() == null) {
            return;
        }

        try {
            ServerRoom serverRoom = serverRoomRepository.findById(stats.getServerRoomId())
                    .orElse(null);

            if (serverRoom == null || !Boolean.TRUE.equals(serverRoom.getMonitoringEnabled())) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            // 평균 CPU 평가
            if (serverRoom.getAvgCpuThresholdWarning() != null && stats.getAvgCpuUsage() != null) {
                evaluateMetric(
                        TargetType.SERVER_ROOM, serverRoom.getId(), serverRoom.getName(),
                        MetricType.CPU, "avg_cpu_usage", stats.getAvgCpuUsage(),
                        serverRoom.getAvgCpuThresholdWarning().doubleValue(),
                        serverRoom.getAvgCpuThresholdCritical() != null ?
                                serverRoom.getAvgCpuThresholdCritical().doubleValue() : null,
                        now
                );
            }

            // 평균 Memory 평가
            if (serverRoom.getAvgMemoryThresholdWarning() != null && stats.getAvgMemoryUsage() != null) {
                evaluateMetric(
                        TargetType.SERVER_ROOM, serverRoom.getId(), serverRoom.getName(),
                        MetricType.MEMORY, "avg_memory_usage", stats.getAvgMemoryUsage(),
                        serverRoom.getAvgMemoryThresholdWarning().doubleValue(),
                        serverRoom.getAvgMemoryThresholdCritical() != null ?
                                serverRoom.getAvgMemoryThresholdCritical().doubleValue() : null,
                        now
                );
            }

        } catch (Exception e) {
            log.error("❌ ServerRoom 통계 알림 평가 실패: serverRoomId={}", stats.getServerRoomId(), e);
        }
    }

    /**
     * DataCenter 통계 평가
     */
    @Async("alertExecutor")
    @Transactional
    public void evaluateDataCenterStatistics(DataCenterStatisticsDto stats) {
        if (stats == null || stats.getDataCenterId() == null) {
            return;
        }

        try {
            DataCenter dataCenter = dataCenterRepository.findById(stats.getDataCenterId())
                    .orElse(null);

            if (dataCenter == null || !Boolean.TRUE.equals(dataCenter.getMonitoringEnabled())) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            // 평균 CPU 평가
            if (dataCenter.getAvgCpuThresholdWarning() != null && stats.getAvgCpuUsage() != null) {
                evaluateMetric(
                        TargetType.DATA_CENTER, dataCenter.getId(), dataCenter.getName(),
                        MetricType.CPU, "avg_cpu_usage", stats.getAvgCpuUsage(),
                        dataCenter.getAvgCpuThresholdWarning().doubleValue(),
                        dataCenter.getAvgCpuThresholdCritical() != null ?
                                dataCenter.getAvgCpuThresholdCritical().doubleValue() : null,
                        now
                );
            }

        } catch (Exception e) {
            log.error("❌ DataCenter 통계 알림 평가 실패: dataCenterId={}", stats.getDataCenterId(), e);
        }
    }

    // ========== 핵심 평가 로직 ==========

    private void evaluateMetric(
            TargetType targetType, Long targetId, String targetName,
            MetricType metricType, String metricName,
            Double measuredValue, Double warningThreshold, Double criticalThreshold,
            LocalDateTime metricTime) {

        if (measuredValue == null) return;

        AlertLevel violationLevel = checkViolation(measuredValue, warningThreshold, criticalThreshold);
        AlertViolationTracker tracker = getOrCreateTracker(targetType, targetId, metricType, metricName);

        if (violationLevel != null) {
            handleViolation(targetType, targetId, targetName, tracker, violationLevel,
                    metricType, metricName, measuredValue,
                    getThresholdValue(violationLevel, warningThreshold, criticalThreshold),
                    metricTime);
        } else {
            handleRecovery(targetType, targetId, targetName, tracker, metricType, metricName);
        }
    }

    private AlertLevel checkViolation(Double measuredValue, Double warningThreshold, Double criticalThreshold) {
        if (criticalThreshold != null && measuredValue >= criticalThreshold) {
            return AlertLevel.CRITICAL;
        }
        if (measuredValue >= warningThreshold) {
            return AlertLevel.WARNING;
        }
        return null;
    }

    private void handleViolation(
            TargetType targetType, Long targetId, String targetName,
            AlertViolationTracker tracker, AlertLevel level,
            MetricType metricType, String metricName,
            Double measuredValue, Double thresholdValue, LocalDateTime metricTime) {

        tracker.setConsecutiveViolations(tracker.getConsecutiveViolations() + 1);
        tracker.setLastViolationTime(metricTime);
        tracker.setLastMeasuredValue(measuredValue);
        tracker.setUpdatedAt(LocalDateTime.now());
        violationTrackerRepository.save(tracker);

        AlertSettingsDto settings = getAlertSettings();

        // ✅ Record getter: defaultConsecutiveCount()
        if (tracker.getConsecutiveViolations() >= settings.defaultConsecutiveCount()) {
            if (shouldSendAlert(tracker, settings)) {
                sendAlert(targetType, targetId, targetName, level, metricType, metricName,
                        measuredValue, thresholdValue, metricTime);
                tracker.setLastAlertSentAt(metricTime);
                violationTrackerRepository.save(tracker);
            }
        }
    }

    private void handleViolationDirect(
            TargetType targetType, Long targetId, String targetName,
            MetricType metricType, String metricName, AlertLevel level,
            Double measuredValue, Double thresholdValue, LocalDateTime metricTime) {

        AlertViolationTracker tracker = getOrCreateTracker(targetType, targetId, metricType, metricName);
        handleViolation(targetType, targetId, targetName, tracker, level, metricType, metricName,
                measuredValue, thresholdValue, metricTime);
    }

    private void handleRecovery(
            TargetType targetType, Long targetId, String targetName,
            AlertViolationTracker tracker, MetricType metricType, String metricName) {

        if (tracker.getConsecutiveViolations() > 0) {
            tracker.setConsecutiveViolations(0);
            tracker.setUpdatedAt(LocalDateTime.now());
            violationTrackerRepository.save(tracker);

            resolveActiveAlerts(targetType, targetId, targetName, metricType, metricName);
        }
    }

    private boolean shouldSendAlert(AlertViolationTracker tracker, AlertSettingsDto settings) {
        if (tracker.getLastAlertSentAt() == null) {
            return true;
        }

        // ✅ Record getter: defaultCooldownMinutes()
        LocalDateTime cooldownEnd = tracker.getLastAlertSentAt()
                .plusMinutes(settings.defaultCooldownMinutes());

        return LocalDateTime.now().isAfter(cooldownEnd);
    }

    private void sendAlert(
            TargetType targetType, Long targetId, String targetName,
            AlertLevel level, MetricType metricType, String metricName,
            Double measuredValue, Double thresholdValue, LocalDateTime metricTime) {

        AlertHistory alert = AlertHistory.builder()
                .targetType(targetType)
                .targetName(targetName)
                .metricType(metricType)
                .metricName(metricName)
                .level(level)
                .measuredValue(measuredValue)
                .thresholdValue(thresholdValue)
                .status(AlertStatus.TRIGGERED)
                .triggeredAt(metricTime)
                .message(buildAlertMessage(targetType, targetName, level, metricType,
                        measuredValue, thresholdValue))
                .build();

        switch (targetType) {
            case EQUIPMENT -> alert.setEquipmentId(targetId);
            case RACK -> alert.setRackId(targetId);
            case SERVER_ROOM -> alert.setServerRoomId(targetId);
            case DATA_CENTER -> alert.setDataCenterId(targetId);
        }

        alertHistoryRepository.save(alert);
        alertNotificationService.sendAlert(alert);

        log.warn("🚨 알림 발생 - {} [{}] {}:{} (측정값: {:.1f}, 임계치: {:.0f})",
                level.name(), metricType.name(), targetName, metricName,
                measuredValue, thresholdValue);
    }

    private void resolveActiveAlerts(
            TargetType targetType, Long targetId, String targetName,
            MetricType metricType, String metricName) {

        List<AlertHistory> activeAlerts = switch (targetType) {
            case EQUIPMENT -> alertHistoryRepository.findActiveAlertsByEquipmentIdAndMetric(
                    targetId, metricType, metricName);
            case RACK -> alertHistoryRepository.findActiveAlertsByRackIdAndMetric(
                    targetId, metricType, metricName);
            default -> List.of();
        };

        for (AlertHistory alert : activeAlerts) {
            alert.resolve(null);
            alertHistoryRepository.save(alert);
            alertNotificationService.sendAlertResolved(alert);

            log.info("✅ 알림 자동 해결 - alertId={}, {}:{}",
                    alert.getId(), metricType.name(), metricName);
        }
    }

    private AlertViolationTracker getOrCreateTracker(
            TargetType targetType, Long targetId,
            MetricType metricType, String metricName) {

        Optional<AlertViolationTracker> existing = switch (targetType) {
            case EQUIPMENT -> violationTrackerRepository.findByEquipmentIdAndMetric(
                    targetId, metricType, metricName);
            case RACK -> violationTrackerRepository.findByRackIdAndMetric(
                    targetId, metricType, metricName);
            case SERVER_ROOM -> violationTrackerRepository.findByServerRoomIdAndMetric(
                    targetId, metricType, metricName);
            case DATA_CENTER -> violationTrackerRepository.findByDataCenterIdAndMetric(
                    targetId, metricType, metricName);
        };

        return existing.orElseGet(() -> {
            AlertViolationTracker newTracker = AlertViolationTracker.builder()
                    .targetType(targetType)
                    .metricType(metricType)
                    .metricName(metricName)
                    .consecutiveViolations(0)
                    .lastViolationTime(LocalDateTime.now())
                    .build();

            switch (targetType) {
                case EQUIPMENT -> newTracker.setEquipmentId(targetId);
                case RACK -> newTracker.setRackId(targetId);
                case SERVER_ROOM -> newTracker.setServerRoomId(targetId);
                case DATA_CENTER -> newTracker.setDataCenterId(targetId);
            }

            return violationTrackerRepository.save(newTracker);
        });
    }

    private String buildAlertMessage(
            TargetType targetType, String targetName,
            AlertLevel level, MetricType metricType,
            Double measuredValue, Double thresholdValue) {

        String levelText = level == AlertLevel.CRITICAL ? "위험" : "경고";

        return String.format("[%s] %s %s이(가) %s 임계치 %.0f를 초과했습니다. (현재: %.1f)",
                targetType.getDescription(), targetName, metricType.getDescription(),
                levelText, thresholdValue, measuredValue);
    }

    private Double getThresholdValue(AlertLevel level, Double warningThreshold, Double criticalThreshold) {
        if (level == AlertLevel.CRITICAL && criticalThreshold != null) {
            return criticalThreshold;
        }
        return warningThreshold;
    }

    private AlertSettingsDto getAlertSettings() {
        return alertSettingsRepository.findById(1L)
                .map(AlertSettingsDto::from)
                .orElseGet(AlertSettingsDto::getDefault);
    }
}