package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.history.dto.HistoryCreateRequest;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.member.domain.Member;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Equipment 히스토리 기록 전담 클래스
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EquipmentHistoryRecorder {

    private final HistoryService historyService;

    /**
     * Equipment 생성 히스토리
     */
    public void recordCreate(Equipment equipment, Member member, String ipAddress) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(equipment.getRack().getDatacenter().getId())
                .dataCenterName(equipment.getRack().getDatacenter().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(equipment.getId())
                .entityName(equipment.getName())
                .entityCode(equipment.getCode())
                .action(HistoryAction.CREATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .afterValue(buildSnapshot(equipment))
                .ipAddress(ipAddress)
                .metadata(Map.of("rackName", equipment.getRack().getRackName()))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Equipment 수정 히스토리
     */
    public void recordUpdate(Equipment oldEquipment, Equipment newEquipment,
                             Member member, String reason, String ipAddress) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldEquipment);
        Map<String, Object> newSnapshot = buildSnapshot(newEquipment);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            return;
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(newEquipment.getRack().getDatacenter().getId())
                .dataCenterName(newEquipment.getRack().getDatacenter().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(newEquipment.getId())
                .entityName(newEquipment.getName())
                .entityCode(newEquipment.getCode())
                .action(HistoryAction.UPDATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(changedFields)
                .beforeValue(oldSnapshot)
                .afterValue(newSnapshot)
                .reason(reason)
                .ipAddress(ipAddress)
                .metadata(Map.of("rackName", newEquipment.getRack().getRackName()))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Equipment 이동 히스토리
     */
    public void recordMove(Equipment equipment, String oldLocation, String newLocation,
                           Member member, String reason, String ipAddress) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(equipment.getRack().getDatacenter().getId())
                .dataCenterName(equipment.getRack().getDatacenter().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(equipment.getId())
                .entityName(equipment.getName())
                .entityCode(equipment.getCode())
                .action(HistoryAction.MOVE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("location", "rack", "startUnit"))
                .beforeValue(Map.of("location", oldLocation))
                .afterValue(Map.of("location", newLocation))
                .reason(reason)
                .ipAddress(ipAddress)
                .metadata(Map.of("rackName", equipment.getRack().getRackName()))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Equipment 상태 변경 히스토리
     */
    public void recordStatusChange(Equipment equipment, String oldStatus, String newStatus,
                                   Member member, String reason, String ipAddress) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(equipment.getRack().getDatacenter().getId())
                .dataCenterName(equipment.getRack().getDatacenter().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(equipment.getId())
                .entityName(equipment.getName())
                .entityCode(equipment.getCode())
                .action(HistoryAction.STATUS_CHANGE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("status"))
                .beforeValue(Map.of("status", oldStatus))
                .afterValue(Map.of("status", newStatus))
                .reason(reason)
                .ipAddress(ipAddress)
                .metadata(Map.of("rackName", equipment.getRack().getRackName()))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Equipment 삭제 히스토리
     */
    public void recordDelete(Equipment equipment, Member member, String reason, String ipAddress) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(equipment.getRack().getDatacenter().getId())
                .dataCenterName(equipment.getRack().getDatacenter().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(equipment.getId())
                .entityName(equipment.getName())
                .entityCode(equipment.getCode())
                .action(HistoryAction.DELETE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .beforeValue(buildSnapshot(equipment))
                .reason(reason)
                .ipAddress(ipAddress)
                .metadata(Map.of("rackName", equipment.getRack().getRackName()))
                .build();

        historyService.recordHistory(request);
    }

    // === Private Helper Methods ===

    private Map<String, Object> buildSnapshot(Equipment equipment) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name", equipment.getName());
        snapshot.put("code", equipment.getCode());
        snapshot.put("type", equipment.getType() != null ? equipment.getType().name() : null);
        snapshot.put("rackId", equipment.getRack() != null ? equipment.getRack().getId() : null);
        snapshot.put("rackName", equipment.getRack() != null ? equipment.getRack().getRackName() : null);
        snapshot.put("startUnit", equipment.getStartUnit());
        snapshot.put("unitSize", equipment.getUnitSize());
        snapshot.put("status", equipment.getStatus() != null ? equipment.getStatus().name() : null);
        snapshot.put("ipAddress", equipment.getIpAddress());
        snapshot.put("modelName", equipment.getModelName());
        snapshot.put("manufacturer", equipment.getManufacturer());
        snapshot.put("serialNumber", equipment.getSerialNumber());
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