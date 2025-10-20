package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 랙 실장도 관리 서비스
 * 실장도(Elevation View): 랙의 1U~42U 유닛을 시각적으로 표시하고 장비를 배치/이동
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackElevationService {

    private final RackRepository rackRepository;
    private final EquipmentRepository equipmentRepository;

    /**
     * 랙 실장도 조회 (Elevation View)
     */
    public RackElevationResponse getRackElevation(Long id, String view) {
        log.info("Fetching rack elevation for id: {}, view: {}", id, view);

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        // 랙에 장착된 장비 목록 조회
        List<Equipment> equipments = equipmentRepository.findByRackIdAndDelYn(id, DelYN.N);

        return RackElevationResponse.from(rack, equipments, view);
    }

    /**
     * 장비 배치 (드래그 앤 드롭)
     */
    // RackElevationService.java
    @Transactional
    public void placeEquipment(Long rackId, Long equipmentId, EquipmentPlacementRequest request) {
        log.info("Placing equipment {} on rack {} at unit {}",
                equipmentId, rackId, request.startUnit());

        if (rackId == null || rackId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));


        if (request.powerConsumption() != null) {
            equipment.setPowerConsumption(request.powerConsumption());
        }
        if (request.weight() != null) {
            equipment.setWeight(request.weight());
        }

        // 배치 검증
        Map<String, Object> validation = validateEquipmentPlacement(rackId, request);
        if (!(Boolean) validation.get("isValid")) {
            throw new BusinessException((String) validation.get("message"));
        }

        // 장비 배치
        rack.placeEquipment(equipment, request.startUnit(), request.unitSize());

        log.info("Equipment placed successfully with power: {}W, weight: {}kg",
                equipment.getPowerConsumption(), equipment.getWeight());
    }

    /**
     * 장비 이동
     */
    @Transactional
    public void moveEquipment(Long rackId, Long equipmentId, EquipmentMoveRequest request) {
        log.info("Moving equipment {} on rack {} from unit {} to {}",
                equipmentId, rackId, request.fromUnit(), request.toUnit());

        if (rackId == null || rackId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

        // 이동 가능 여부 검증
        EquipmentPlacementRequest placementRequest = EquipmentPlacementRequest.builder()
                .startUnit(request.toUnit())
                .unitSize(equipment.getUnitSize())
                .powerConsumption(equipment.getPowerConsumption())
                .weight(equipment.getWeight())
                .build();

        // 기존 장비 제거 후 검증
        rack.removeEquipment(equipment);
        Map<String, Object> validation = validateEquipmentPlacement(rackId, placementRequest);

        if (!(Boolean) validation.get("isValid")) {
            // 검증 실패 시 원래 위치로 복구
            rack.placeEquipment(equipment, request.fromUnit(), equipment.getUnitSize());
            throw new BusinessException((String) validation.get("message"));
        }

        // 장비 이동
        rack.moveEquipment(equipment, request.fromUnit(), request.toUnit());

        log.info("Equipment moved successfully");
    }

    /**
     * 장비 배치 검증
     */
    public Map<String, Object> validateEquipmentPlacement(Long rackId, EquipmentPlacementRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (rackId == null || rackId <= 0) {
            result.put("isValid", false);
            result.put("message", "유효하지 않은 랙 ID입니다.");
            return result;
        }

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        // 유닛 범위 검증
        if (request.startUnit() < 1 || request.startUnit() > rack.getTotalUnits()) {
            result.put("isValid", false);
            result.put("message", "시작 유닛이 랙 범위를 벗어났습니다.");
            return result;
        }

        int endUnit = request.startUnit() + request.unitSize() - 1;
        if (endUnit > rack.getTotalUnits()) {
            result.put("isValid", false);
            result.put("message", "장비 크기가 랙 범위를 벗어났습니다.");
            return result;
        }

        // 유닛 겹침 검증
        List<Equipment> existingEquipments = equipmentRepository.findByRackIdAndDelYn(rackId, DelYN.N);
        for (Equipment eq : existingEquipments) {
            int eqEnd = eq.getStartUnit() + eq.getUnitSize() - 1;
            if (!(endUnit < eq.getStartUnit() || request.startUnit() > eqEnd)) {
                result.put("isValid", false);
                result.put("message", "해당 유닛에 이미 장비가 배치되어 있습니다.");
                result.put("conflictingEquipment", eq.getName());
                return result;
            }
        }

        // 전력 용량 검증
        if (rack.getMaxPowerCapacity() != null && request.powerConsumption() != null) {
            BigDecimal newPowerUsage = rack.getCurrentPowerUsage().add(request.powerConsumption());
            if (newPowerUsage.compareTo(rack.getMaxPowerCapacity()) > 0) {
                result.put("isValid", false);
                result.put("message", String.format("랙의 최대 전력 용량을 초과합니다. (현재: %.2fW, 추가: %.2fW, 최대: %.2fW)",
                        rack.getCurrentPowerUsage(), request.powerConsumption(), rack.getMaxPowerCapacity()));
                return result;
            }
        }

// 무게 용량 검증
        if (rack.getMaxWeightCapacity() != null && request.weight() != null) {
            BigDecimal newWeight = rack.getCurrentWeight().add(request.weight());
            if (newWeight.compareTo(rack.getMaxWeightCapacity()) > 0) {
                result.put("isValid", false);
                result.put("message", String.format("랙의 최대 무게 용량을 초과합니다. (현재: %.2fkg, 추가: %.2fkg, 최대: %.2fkg)",
                        rack.getCurrentWeight(), request.weight(), rack.getMaxWeightCapacity()));
                return result;
            }
        }

        result.put("isValid", true);
        result.put("message", "배치 가능합니다.");
        result.put("availablePower", rack.getMaxPowerCapacity() != null && rack.getCurrentPowerUsage() != null
                ? rack.getMaxPowerCapacity().subtract(rack.getCurrentPowerUsage())
                : null);
        result.put("availableWeight", rack.getMaxWeightCapacity() != null && rack.getCurrentWeight() != null
                ? rack.getMaxWeightCapacity().subtract(rack.getCurrentWeight())
                : null);
        return result;
    }

    /**
     * 랙 사용률 조회
     */
    public RackUtilizationResponse getRackUtilization(Long id) {
        log.info("Fetching rack utilization for id: {}", id);

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        return RackUtilizationResponse.from(rack);
    }
}