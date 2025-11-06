package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.device.domain.Device;
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
 * Device 히스토리 기록 전담 클래스
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceHistoryRecorder {

    private final HistoryService historyService;

    /**
     * Device 생성 히스토리
     */
    public void recordCreate(Device device, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(device.getDatacenter().getId())
                .dataCenterName(device.getDatacenter().getName())
                .entityType(EntityType.DEVICE)
                .entityId(device.getId())
                .entityName(device.getDeviceName())
                .entityCode(device.getDeviceCode())
                .action(HistoryAction.CREATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .afterValue(buildSnapshot(device))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Device 수정 히스토리
     */
    public void recordUpdate(Device oldDevice, Device newDevice,
                             Member member ) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldDevice);
        Map<String, Object> newSnapshot = buildSnapshot(newDevice);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            return;
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(newDevice.getDatacenter().getId())
                .dataCenterName(newDevice.getDatacenter().getName())
                .entityType(EntityType.DEVICE)
                .entityId(newDevice.getId())
                .entityName(newDevice.getDeviceName())
                .entityCode(newDevice.getDeviceCode())
                .action(HistoryAction.UPDATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(changedFields)
                .beforeValue(oldSnapshot)
                .afterValue(newSnapshot)
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Device 위치 변경 히스토리
     */
    public void recordMove(Device device, String oldPosition, String newPosition,
                           Member member ) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(device.getDatacenter().getId())
                .dataCenterName(device.getDatacenter().getName())
                .entityType(EntityType.DEVICE)
                .entityId(device.getId())
                .entityName(device.getDeviceName())
                .entityCode(device.getDeviceCode())
                .action(HistoryAction.MOVE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("gridX", "gridY", "gridZ", "rotation"))
                .beforeValue(Map.of("position", oldPosition))
                .afterValue(Map.of("position", newPosition))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Device 상태 변경 히스토리
     */
    public void recordStatusChange(Device device, String oldStatus, String newStatus,
                                   Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(device.getDatacenter().getId())
                .dataCenterName(device.getDatacenter().getName())
                .entityType(EntityType.DEVICE)
                .entityId(device.getId())
                .entityName(device.getDeviceName())
                .entityCode(device.getDeviceCode())
                .action(HistoryAction.STATUS_CHANGE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("status"))
                .beforeValue(Map.of("status", oldStatus))
                .afterValue(Map.of("status", newStatus))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Device 삭제 히스토리
     */
    public void recordDelete(Device device, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .dataCenterId(device.getDatacenter().getId())
                .dataCenterName(device.getDatacenter().getName())
                .entityType(EntityType.DEVICE)
                .entityId(device.getId())
                .entityName(device.getDeviceName())
                .entityCode(device.getDeviceCode())
                .action(HistoryAction.DELETE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .beforeValue(buildSnapshot(device))
                .build();

        historyService.recordHistory(request);
    }

    // === Private Helper Methods ===

    private Map<String, Object> buildSnapshot(Device device) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("deviceName", device.getDeviceName());
        snapshot.put("deviceCode", device.getDeviceCode());
        snapshot.put("gridX", device.getGridX());
        snapshot.put("gridY", device.getGridY());
        snapshot.put("gridZ", device.getGridZ());
        snapshot.put("rotation", device.getRotation());
        snapshot.put("status", device.getStatus() != null ? device.getStatus().name() : null);
        snapshot.put("modelName", device.getModelName());
        snapshot.put("manufacturer", device.getManufacturer());
        snapshot.put("serialNumber", device.getSerialNumber());
        snapshot.put("deviceType", device.getDeviceType() != null ? device.getDeviceType().getTypeName() : null);
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
