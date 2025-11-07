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
 * DataCenter 히스토리 기록 전담 클래스 (개선 버전)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataCenterHistoryRecorder {

    private final HistoryService historyService;

    /**
     * DataCenter 생성 히스토리
     */
    public void recordCreate(DataCenter dataCenter, Member member) {
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
                .build();

        historyService.recordHistory(request);
    }

    /**
     * DataCenter 수정 히스토리 (상세 변경 내역 포함)
     */
    public void recordUpdate(DataCenter oldDataCenter, DataCenter newDataCenter, Member member) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldDataCenter);
        Map<String, Object> newSnapshot = buildSnapshot(newDataCenter);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            log.info("No changes detected for datacenter id: {}", newDataCenter.getId());
            return;
        }

        // 변경 내역 상세 정보 구성
        Map<String, Object> changeDetails = buildChangeDetails(oldSnapshot, newSnapshot, changedFields);

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
                .metadata(Map.of("changeDetails", changeDetails))
                .build();

        historyService.recordHistory(request);
        log.info("DataCenter update history recorded: {} fields changed", changedFields.size());
    }

    /**
     * DataCenter 상태 변경 히스토리
     */
    public void recordStatusChange(DataCenter dataCenter, String oldStatus, String newStatus, Member member) {
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
                .metadata(Map.of("statusChange", String.format("%s → %s",
                        translateStatus(oldStatus), translateStatus(newStatus))))
                .build();

        historyService.recordHistory(request);
        log.info("DataCenter status change history recorded: {} -> {}", oldStatus, newStatus);
    }

    /**
     * DataCenter 삭제 히스토리
     */
    public void recordDelete(DataCenter dataCenter, Member member) {
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
        snapshot.put("currentRackCount", dataCenter.getCurrentRackCount());
        snapshot.put("temperatureMin", dataCenter.getTemperatureMin());
        snapshot.put("temperatureMax", dataCenter.getTemperatureMax());
        snapshot.put("humidityMin", dataCenter.getHumidityMin());
        snapshot.put("humidityMax", dataCenter.getHumidityMax());
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

    /**
     * 변경 내역 상세 정보 구성
     */
    private Map<String, Object> buildChangeDetails(
            Map<String, Object> oldSnapshot,
            Map<String, Object> newSnapshot,
            List<String> changedFields) {

        Map<String, Object> changeDetails = new HashMap<>();

        for (String field : changedFields) {
            Object oldValue = oldSnapshot.get(field);
            Object newValue = newSnapshot.get(field);

            String fieldLabel = getFieldLabel(field);
            String oldValueStr = formatValue(field, oldValue);
            String newValueStr = formatValue(field, newValue);

            changeDetails.put(field, Map.of(
                    "fieldLabel", fieldLabel,
                    "oldValue", oldValueStr,
                    "newValue", newValueStr,
                    "changeDescription", String.format("%s: %s → %s", fieldLabel, oldValueStr, newValueStr)
            ));
        }

        return changeDetails;
    }

    private String getFieldLabel(String field) {
        return switch (field) {
            case "name" -> "전산실 이름";
            case "code" -> "전산실 코드";
            case "location" -> "위치";
            case "floor" -> "층";
            case "rows" -> "행 수";
            case "columns" -> "열 수";
            case "status" -> "상태";
            case "description" -> "설명";
            case "totalArea" -> "총 면적";
            case "totalPowerCapacity" -> "총 전력 용량";
            case "totalCoolingCapacity" -> "총 냉각 용량";
            case "maxRackCount" -> "최대 랙 수";
            case "currentRackCount" -> "현재 랙 수";
            case "temperatureMin" -> "최저 온도";
            case "temperatureMax" -> "최고 온도";
            case "humidityMin" -> "최저 습도";
            case "humidityMax" -> "최고 습도";
            case "managerName" -> "담당자";
            default -> field;
        };
    }

    private String formatValue(String field, Object value) {
        if (value == null) {
            return "(없음)";
        }

        return switch (field) {
            case "totalArea" -> value + " ㎡";
            case "totalPowerCapacity" -> value + " kW";
            case "totalCoolingCapacity" -> value + " RT";
            case "temperatureMin", "temperatureMax" -> value + " ℃";
            case "humidityMin", "humidityMax" -> value + " %";
            case "status" -> translateStatus(value.toString());
            default -> value.toString();
        };
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "ACTIVE" -> "활성";
            case "INACTIVE" -> "비활성";
            case "MAINTENANCE" -> "점검중";
            case "CONSTRUCTION" -> "공사중";
            default -> status;
        };
    }
}