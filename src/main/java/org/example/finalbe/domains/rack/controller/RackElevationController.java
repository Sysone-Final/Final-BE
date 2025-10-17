package org.example.finalbe.domains.rack.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.service.RackElevationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 랙 실장도 관package org.example.finalbe.domains.rack.controller;

 import lombok.RequiredArgsConstructor;
 import org.example.finalbe.domains.common.dto.CommonResDto;
 import org.example.finalbe.domains.rack.dto.*;
 import org.example.finalbe.domains.rack.service.RackElevationService;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseEntity;
 import org.springframework.security.access.prepost.PreAuthorize;
 import org.springframework.web.bind.annotation.*;

 import java.util.Map;

 /**
 * 랙 실장도 관리 컨트롤러
 * 실장도(Elevation View): 랙의 1U~42U 유닛을 시각적으로 표시
 */
@RestController
@RequestMapping("/racks")
@RequiredArgsConstructor
public class RackElevationController {

    private final RackElevationService rackElevationService;

    @GetMapping("/{id}/elevation")
    public ResponseEntity<CommonResDto> getRackElevation(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "FRONT") String view) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        RackElevationResponse elevation = rackElevationService.getRackElevation(id, view);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 실장도 조회 완료", elevation));
    }

    @PostMapping("/{id}/equipment/{equipmentId}/place")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> placeEquipment(
            @PathVariable Long id,
            @PathVariable Long equipmentId,
            @RequestBody EquipmentPlacementRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        rackElevationService.placeEquipment(id, equipmentId, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 배치 완료", null));
    }

    @PatchMapping("/{id}/equipment/{equipmentId}/move")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> moveEquipment(
            @PathVariable Long id,
            @PathVariable Long equipmentId,
            @RequestBody EquipmentMoveRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        rackElevationService.moveEquipment(id, equipmentId, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 이동 완료", null));
    }

    @PostMapping("/{id}/validate-placement")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> validateEquipmentPlacement(
            @PathVariable Long id,
            @RequestBody EquipmentPlacementRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        Map<String, Object> validationResult = rackElevationService.validateEquipmentPlacement(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "배치 검증 완료", validationResult));
    }

    @GetMapping("/{id}/utilization")
    public ResponseEntity<CommonResDto> getRackUtilization(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        RackUtilizationResponse utilization = rackElevationService.getRackUtilization(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 사용률 조회 완료", utilization));
    }
}