/**
 * 작성자: 황요한
 * 랙 실장도 및 장비 배치 서비스
 */
package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.service.ServerRoomDataSimulator;
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
    private final EquipmentMappingService equipmentMappingService;
    private final ServerRoomDataSimulator serverRoomDataSimulator;

    // 랙 실장도 조회
    public RackElevationResponse getRackElevation(Long id, String view) {
        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        List<Equipment> equipments = equipmentRepository.findByRackIdAndDelYn(id, DelYN.N);

        return RackElevationResponse.from(rack, equipments, view);
    }

    // 장비 배치
    @Transactional
    public void placeEquipment(Long rackId, Long equipmentId, EquipmentPlacementRequest request) {
        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        Equipment equipment = equipmentRepository.findActiveById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

        Map<String, Object> validation = validateEquipmentPlacement(rackId, request);
        if (!(Boolean) validation.get("isValid")) {
            throw new BusinessException((String) validation.get("message"));
        }

        rack.occupyUnits(request.unitSize());

        if (request.powerConsumption() != null) {
            rack.addPowerUsage(request.powerConsumption());
        }

        equipment.setRack(rack);
        equipment.setStartUnit(request.startUnit());
        equipment.setUnitSize(request.unitSize());

        equipmentMappingService.addEquipmentMapping(equipment);

        if (equipment.getType() == EquipmentType.SERVER || equipment.getType() == EquipmentType.STORAGE) {
            try {
                serverRoomDataSimulator.addEquipment(equipment);
            } catch (Exception ignored) {}
        }
    }

    // 장비 이동
    @Transactional
    public void moveEquipment(Long rackId, Long equipmentId, EquipmentMoveRequest request) {
        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        Equipment equipment = equipmentRepository.findActiveById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

        EquipmentPlacementRequest placementRequest = EquipmentPlacementRequest.builder()
                .startUnit(request.toUnit())
                .unitSize(equipment.getUnitSize())
                .build();

        Map<String, Object> validation = validateEquipmentPlacement(rackId, placementRequest);
        if (!(Boolean) validation.get("isValid")) {
            throw new BusinessException((String) validation.get("message"));
        }

        equipment.setStartUnit(request.toUnit());
    }

    // 장비 배치 검증
    public Map<String, Object> validateEquipmentPlacement(Long rackId, EquipmentPlacementRequest request) {
        Map<String, Object> result = new HashMap<>();

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

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

        List<Equipment> existing = equipmentRepository.findByRackIdAndDelYn(rackId, DelYN.N);
        for (Equipment eq : existing) {
            int eqEnd = eq.getStartUnit() + eq.getUnitSize() - 1;
            boolean overlap = !(endUnit < eq.getStartUnit() || request.startUnit() > eqEnd);
            if (overlap) {
                result.put("isValid", false);
                result.put("message", "해당 유닛에 이미 장비가 배치되어 있습니다.");
                result.put("conflictingEquipment", eq.getName());
                return result;
            }
        }

        if (rack.getMaxPowerCapacity() != null && request.powerConsumption() != null) {
            BigDecimal newPower = rack.getCurrentPowerUsage().add(request.powerConsumption());
            if (newPower.compareTo(rack.getMaxPowerCapacity()) > 0) {
                result.put("isValid", false);
                result.put("message", "랙의 최대 전력 용량을 초과합니다.");
                return result;
            }
        }

        result.put("isValid", true);
        result.put("message", "배치 가능합니다.");
        return result;
    }

    // 랙 사용률 조회
    public RackUtilizationResponse getRackUtilization(Long id) {
        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        return RackUtilizationResponse.from(rack);
    }
}
