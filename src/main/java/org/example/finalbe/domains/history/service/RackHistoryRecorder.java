package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.history.dto.HistoryCreateRequest;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.rack.domain.Rack;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rack 히스토리 기록 전담 클래스
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RackHistoryRecorder {

    private final HistoryService historyService;

    /**
     * Rack 생성 히스토리
     */
    public void recordCreate(Rack rack, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(rack.getDatacenter().getId())
                .dataCenterName(rack.getDatacenter().getName())
                .entityType(EntityType.RACK)
                .entityId(rack.getId())
                .entityName(rack.getRackName())
                .entityCode(rack.getRackName())
                .action(HistoryAction.CREATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .afterValue(buildSnapshot(rack))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Rack 수정 히스토리
     */
    public void recordUpdate(Rack oldRack, Rack newRack, Member member, String reason ) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldRack);
        Map<String, Object> newSnapshot = buildSnapshot(newRack);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            return;
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(newRack.getDatacenter().getId())
                .dataCenterName(newRack.getDatacenter().getName())
                .entityType(EntityType.RACK)
                .entityId(newRack.getId())
                .entityName(newRack.getRackName())
                .entityCode(newRack.getRackName())
                .action(HistoryAction.UPDATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(changedFields)
                .beforeValue(oldSnapshot)
                .afterValue(newSnapshot)
                .reason(reason)
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Rack 상태 변경 히스토리
     */
    public void recordStatusChange(Rack rack, String oldStatus, String newStatus,
                                   Member member, String reason ) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(rack.getDatacenter().getId())
                .dataCenterName(rack.getDatacenter().getName())
                .entityType(EntityType.RACK)
                .entityId(rack.getId())
                .entityName(rack.getRackName())
                .entityCode(rack.getRackName())
                .action(HistoryAction.STATUS_CHANGE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("status"))
                .beforeValue(Map.of("status", oldStatus))
                .afterValue(Map.of("status", newStatus))
                .reason(reason)
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Rack 삭제 히스토리
     */
    public void recordDelete(Rack rack, Member member, String reason ) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(rack.getDatacenter().getId())
                .dataCenterName(rack.getDatacenter().getName())
                .entityType(EntityType.RACK)
                .entityId(rack.getId())
                .entityName(rack.getRackName())
                .entityCode(rack.getRackName())
                .action(HistoryAction.DELETE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .beforeValue(buildSnapshot(rack))
                .reason(reason)
                .build();

        historyService.recordHistory(request);
    }

    // === Private Helper Methods ===

    private Map<String, Object> buildSnapshot(Rack rack) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("rackName", rack.getRackName());
        snapshot.put("groupNumber", rack.getGroupNumber());
        snapshot.put("rackLocation", rack.getRackLocation());
        snapshot.put("totalUnits", rack.getTotalUnits());
        snapshot.put("usedUnits", rack.getUsedUnits());
        snapshot.put("availableUnits", rack.getAvailableUnits());
        snapshot.put("status", rack.getStatus() != null ? rack.getStatus().name() : null);
        snapshot.put("rackType", rack.getRackType() != null ? rack.getRackType().name() : null);
        snapshot.put("doorDirection", rack.getDoorDirection() != null ? rack.getDoorDirection().name() : null);
        snapshot.put("zoneDirection", rack.getZoneDirection() != null ? rack.getZoneDirection().name() : null);
        snapshot.put("maxPowerCapacity", rack.getMaxPowerCapacity());
        snapshot.put("currentPowerUsage", rack.getCurrentPowerUsage());
        snapshot.put("maxWeightCapacity", rack.getMaxWeightCapacity());
        snapshot.put("currentWeight", rack.getCurrentWeight());
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