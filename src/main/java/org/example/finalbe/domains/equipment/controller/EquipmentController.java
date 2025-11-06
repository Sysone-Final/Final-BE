package org.example.finalbe.domains.equipment.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.equipment.dto.*;
import org.example.finalbe.domains.equipment.service.EquipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 장비 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
@Validated
public class EquipmentController {

    private final EquipmentService equipmentService;

    /**
     * 메인 조회: 페이지네이션 + 전체 필터
     * GET /api/equipments?page=0&size=10&keyword=&type=&status=&datacenterId=
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getEquipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long datacenterId) {

        EquipmentPageResponse response = equipmentService.getEquipmentsWithFilters(
                page, size, keyword, type, status, datacenterId);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 목록 조회 완료", response));
    }

    /**
     * 장비 상세 조회
     * GET /api/equipments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getEquipmentById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id) {

        EquipmentDetailResponse equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 조회 완료", equipment));
    }

    /**
     * 랙별 장비 목록 조회
     * GET /api/equipments/rack/{rackId}
     */
    @GetMapping("/rack/{rackId}")
    public ResponseEntity<CommonResDto> getEquipmentsByRack(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "name") String sortBy) {

        List<EquipmentListResponse> equipments = equipmentService.getEquipmentsByRack(
                rackId, status, type, sortBy);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 목록 조회 완료", equipments));
    }

    /**
     * 전산실별 장비 목록 조회
     * GET /api/equipments/datacenter/{datacenterId}
     */
    @GetMapping("/datacenter/{datacenterId}")
    public ResponseEntity<CommonResDto> getEquipmentByDatacenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long datacenterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        List<EquipmentListResponse> equipments = equipmentService.getEquipmentsByDatacenter(
                datacenterId, status, type);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 목록 조회 완료", equipments));
    }

    /**
     * 장비 검색
     * GET /api/equipments/search
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> getEquipmentsBySearch(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        List<EquipmentListResponse> equipments = equipmentService.searchEquipments(
                keyword, type, status);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "검색 완료", equipments));
    }

    /**
     * 장비 생성
     * POST /api/equipments
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createEquipment(
            @RequestParam String equipmentName,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false) String equipmentType,
            @RequestParam Integer startUnit,
            @RequestParam Integer unitSize,
            @RequestParam(required = false) String positionType,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String macAddress,
            @RequestParam(required = false) String os,
            @RequestParam(required = false) String cpuSpec,
            @RequestParam(required = false) String memorySpec,
            @RequestParam(required = false) String diskSpec,
            @RequestParam(required = false) BigDecimal powerConsumption,
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate installationDate,
            @RequestParam(required = false) String notes,
            @RequestParam Long rackId,
            @RequestParam(required = false) Boolean monitoringEnabled,
            @RequestParam(required = false) Integer cpuThresholdWarning,
            @RequestParam(required = false) Integer cpuThresholdCritical,
            @RequestParam(required = false) Integer memoryThresholdWarning,
            @RequestParam(required = false) Integer memoryThresholdCritical,
            @RequestParam(required = false) Integer diskThresholdWarning,
            @RequestParam(required = false) Integer diskThresholdCritical) {

        EquipmentCreateRequest request = EquipmentCreateRequest.builder()
                .equipmentName(equipmentName)
                .equipmentCode(equipmentCode)
                .equipmentType(equipmentType)
                .startUnit(startUnit)
                .unitSize(unitSize)
                .positionType(positionType)
                .modelName(modelName)
                .manufacturer(manufacturer)
                .serialNumber(serialNumber)
                .ipAddress(ipAddress)
                .macAddress(macAddress)
                .os(os)
                .cpuSpec(cpuSpec)
                .memorySpec(memorySpec)
                .diskSpec(diskSpec)
                .powerConsumption(powerConsumption)
                .weight(weight)
                .status(status)
                .installationDate(installationDate)
                .notes(notes)
                .rackId(rackId)
                .monitoringEnabled(monitoringEnabled)
                .cpuThresholdWarning(cpuThresholdWarning)
                .cpuThresholdCritical(cpuThresholdCritical)
                .memoryThresholdWarning(memoryThresholdWarning)
                .memoryThresholdCritical(memoryThresholdCritical)
                .diskThresholdWarning(diskThresholdWarning)
                .diskThresholdCritical(diskThresholdCritical)
                .build();

        EquipmentDetailResponse equipment = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "장비 생성 완료", equipment));
    }

    /**
     * 장비 수정
     * PUT /api/equipments/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id,
            @RequestParam(required = false) String equipmentName,
            @RequestParam(required = false) String equipmentCode,
            @RequestParam(required = false) String equipmentType,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String macAddress,
            @RequestParam(required = false) String os,
            @RequestParam(required = false) String cpuSpec,
            @RequestParam(required = false) String memorySpec,
            @RequestParam(required = false) String diskSpec,
            @RequestParam(required = false) BigDecimal powerConsumption,
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate installationDate,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Boolean monitoringEnabled,
            @RequestParam(required = false) Integer cpuThresholdWarning,
            @RequestParam(required = false) Integer cpuThresholdCritical,
            @RequestParam(required = false) Integer memoryThresholdWarning,
            @RequestParam(required = false) Integer memoryThresholdCritical,
            @RequestParam(required = false) Integer diskThresholdWarning,
            @RequestParam(required = false) Integer diskThresholdCritical) {

        EquipmentUpdateRequest request = EquipmentUpdateRequest.builder()
                .equipmentName(equipmentName)
                .equipmentCode(equipmentCode)
                .equipmentType(equipmentType)
                .modelName(modelName)
                .manufacturer(manufacturer)
                .serialNumber(serialNumber)
                .ipAddress(ipAddress)
                .macAddress(macAddress)
                .os(os)
                .cpuSpec(cpuSpec)
                .memorySpec(memorySpec)
                .diskSpec(diskSpec)
                .powerConsumption(powerConsumption)
                .weight(weight)
                .status(status)
                .installationDate(installationDate)
                .notes(notes)
                .monitoringEnabled(monitoringEnabled)
                .cpuThresholdWarning(cpuThresholdWarning)
                .cpuThresholdCritical(cpuThresholdCritical)
                .memoryThresholdWarning(memoryThresholdWarning)
                .memoryThresholdCritical(memoryThresholdCritical)
                .diskThresholdWarning(diskThresholdWarning)
                .diskThresholdCritical(diskThresholdCritical)
                .build();

        EquipmentDetailResponse equipment = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 수정 완료", equipment));
    }

    /**
     * 장비 삭제 (단건)
     * DELETE /api/equipments/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id) {

        equipmentService.deleteEquipment(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 삭제 완료", null));
    }

    /**
     * 장비 대량 삭제
     * DELETE /api/equipments
     * Body: {"ids": [1, 2, 3]}
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteMultipleEquipments(
            @RequestBody Map<String, List<Long>> request) {

        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 장비 ID 목록이 비어있습니다.");
        }

        equipmentService.deleteMultipleEquipments(ids);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 대량 삭제 완료", null));
    }
}