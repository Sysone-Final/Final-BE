package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.history.dto.HistoryCreateRequest;


import org.example.finalbe.domains.member.domain.Member;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DataCenter 히스토리 기록 전담 클래스
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataCenterHistoryRecorder {

    private final HistoryService historyService;

    /**
     * DataCenter 생성 히스토리
     */
    public void recordCreate(DataCenter dataCenter, Member member, String ipAddress) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(dataCenter.getId())
                .dataCenterName(dataCenter.getName())
                .entityType(EntityType.DATACENTER)
                .entityId(dataCenter.getId())
                .entityName(dataCenter.getName())
                .entityCode(dataCenter.getCode())
                .action(HistoryAction.CREATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .afterValue(buildSnapshot(dataCenter))
                .ipAddress(ipAddress)
                .build();

        historyService.recordHistory(request);
    }

    /**
     * DataCenter 수정 히스토리
     */
    public void recordUpdate(DataCenter oldDataCenter, DataCenter newDataCenter,
                             Member member, String reason, String ipAddress) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldDataCenter);
        Map<String, Object> newSnapshot = buildSnapshot(newDataCenter);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            return;
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(newDataCenter.getId())
                .dataCenterName(newDataCenter.getName())
                .entityType(EntityType.DATACENTER)
                .entityId(newDataCenter.getId())
                .entityName(newDataCenter.getName())
                .entityCode(newDataCenter.getCode())
                .action(HistoryAction.UPDATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(changedFields)
                .beforeValue(oldSnapshot)
                .afterValue(newSnapshot)
                .reason(reason)
                .ipAddress(ipAddress)
                .build();

        historyService.recordHistory(request);
    }

    /**
     * DataCenter 상태 변경 히스토리
     */
    public void recordStatusChange(DataCenter dataCenter, String oldStatus, String newStatus,
                                   Member member, String reason, String ipAddress) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(dataCenter.getId())
                .dataCenterName(dataCenter.getName())
                .entityType(EntityType.DATACENTER)
                .entityId(dataCenter.getId())
                .entityName(dataCenter.getName())
                .entityCode(dataCenter.getCode())
                .action(HistoryAction.STATUS_CHANGE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("status"))
                .beforeValue(Map.of("status", oldStatus))
                .afterValue(Map.of("status", newStatus))
                .reason(reason)
                .ipAddress(ipAddress)
                .build();

        historyService.recordHistory(request);
    }

    /**
     * DataCenter 삭제 히스토리
     */
    public void recordDelete(DataCenter dataCenter, Member member, String reason, String ipAddress) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(dataCenter.getId())
                .dataCenterName(dataCenter.getName())
                .entityType(EntityType.DATACENTER)
                .entityId(dataCenter.getId())
                .entityName(dataCenter.getName())
                .entityCode(dataCenter.getCode())
                .action(HistoryAction.DELETE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .beforeValue(buildSnapshot(dataCenter))
                .reason(reason)
                .ipAddress(ipAddress)
                .build();

        historyService.recordHistory(request);
    }

    // === Private Helper Methods ===

    private Map<String, Object> buildSnapshot(DataCenter dataCenter) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name", dataCenter.getName());
        snapshot.put("code", dataCenter.getCode());
        snapshot.put("location", dataCenter.getLocation());
        snapshot.put("floor", dataCenter.getFloor());
        snapshot.put("rows", dataCenter.getRows());
        snapshot.put("columns", dataCenter.getColumns());
        snapshot.put("status", dataCenter.getStatus() != null ? dataCenter.getStatus().name() : null);
        snapshot.put("description", dataCenter.getDescription());
        snapshot.put("totalArea", dataCenter.getTotalArea());
        snapshot.put("totalPowerCapacity", dataCenter.getTotalPowerCapacity());
        snapshot.put("totalCoolingCapacity", dataCenter.getTotalCoolingCapacity());
        snapshot.put("maxRackCount", dataCenter.getMaxRackCount());
        snapshot.put("currentRackCount", dataCenter.getCurrentRackCount());
        snapshot.put("temperatureMin", dataCenter.getTemperatureMin());
        snapshot.put("temperatureMax", dataCenter.getTemperatureMax());
        snapshot.put("humidityMin", dataCenter.getHumidityMin());
        snapshot.put("humidityMax", dataCenter.getHumidityMax());
        snapshot.put("managerId", dataCenter.getManager() != null ? dataCenter.getManager().getId() : null);
        snapshot.put("managerName", dataCenter.getManager() != null ? dataCenter.getManager().getName() : null);
        return snapshot;
    }

    private List<String> detectChangedFields(Map<String, Object> oldSnapshot, Map<String, Object> newSnapshot) {
        List<String> changedFields = new ArrayList<>();

        for (String key : newSnapshot.keySet()) {
            Object oldValue = oldSnapshot.get(key);
            Object newValue = newSnapshot.get(key);

            if (oldValue == null && newValue != null) {
                changedFields.add(key);
            } else if (oldValue != null && !oldValue.equals(newValue)) {
                changedFields.add(key);
            }
        }

        return changedFields;
    }
}