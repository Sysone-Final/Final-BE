package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.prometheus.service.EquipmentMappingService;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackElevationService {

    private final RackRepository rackRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentMappingService equipmentMappingService; // ✅ 추가

    public RackElevationResponse getRackElevation(Long id, String view) {
        log.debug("Fetching rack elevation for id: {}, view: {}", id, view);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        List<Equipment> equipments = equipmentRepository.findByRackIdAndDelYn(id, DelYN.N);

        return RackElevationResponse.from(rack, equipments, view);
    }

    @Transactional
    public void placeEquipment(Long rackId, Long equipmentId, EquipmentPlacementRequest request) {
        log.info("Placing equipment {} on rack {} at unit {}", equipmentId, rackId, request.startUnit());

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        Equipment equipment = equipmentRepository.findActiveById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

        // 배치 검증
        Map<String, Object> validation = validateEquipmentPlacement(rackId, request);
        if (!(Boolean) validation.get("isValid")) {
            throw new BusinessException((String) validation.get("message"));
        }

        // 유닛 점유
        rack.occupyUnits(request.unitSize());

        // 전력 사용량 추가
        if (request.powerConsumption() != null) {
            rack.addPowerUsage(request.powerConsumption());
        }

        // 장비 정보 업데이트
        equipment.setRack(rack);
        equipment.setStartUnit(request.startUnit());
        equipment.setUnitSize(request.unitSize());

        // ✅ 추가: 랙에 배치되면 메트릭 수집 시작
        equipmentMappingService.addEquipmentMapping(equipment);
        log.info("✅ Equipment placed successfully - 메트릭 수집 시작됨");
    }

    @Transactional
    public void moveEquipment(Long rackId, Long equipmentId, EquipmentMoveRequest request) {
        log.info("Moving equipment {} on rack {} from unit {} to unit {}",
                equipmentId, rackId, request.fromUnit(), request.toUnit());

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        Equipment equipment = equipmentRepository.findActiveById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

        // 새 위치 배치 검증
        EquipmentPlacementRequest placementRequest = EquipmentPlacementRequest.builder()
                .startUnit(request.toUnit())
                .unitSize(equipment.getUnitSize())
                .powerConsumption(equipment.getPowerConsumption())
                .build();

        Map<String, Object> validation = validateEquipmentPlacement(rackId, placementRequest);
        if (!(Boolean) validation.get("isValid")) {
            throw new BusinessException((String) validation.get("message"));
        }

        // 장비 이동 (시작 유닛 업데이트)
        equipment.setStartUnit(request.toUnit());

        log.info("Equipment moved successfully");
    }

    public Map<String, Object> validateEquipmentPlacement(Long rackId, EquipmentPlacementRequest request) {
        Map<String, Object> result = new HashMap<>();

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

        result.put("isValid", true);
        result.put("message", "배치 가능합니다.");
        return result;
    }

    public RackUtilizationResponse getRackUtilization(Long id) {
        log.debug("Fetching rack utilization for id: {}", id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        return RackUtilizationResponse.from(rack);
    }
}