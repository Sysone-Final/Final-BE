/**
 * 작성자: 황요한
 * 랙 실장도 조회 및 장비 배치/이동을 처리하는 컨트롤러
 */
package org.example.finalbe.domains.rack.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.service.RackElevationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
@Validated
public class RackElevationController {

    private final RackElevationService rackElevationService;

    // 랙 실장도 조회
    @GetMapping("/{id}/elevation")
    public ResponseEntity<CommonResDto> getRackElevation(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @RequestParam(required = false, defaultValue = "FRONT") String view) {

        RackElevationResponse elevation = rackElevationService.getRackElevation(id, view);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 실장도 조회 완료", elevation));
    }

    // 장비 배치
    @PostMapping("/{id}/equipment/{equipmentId}/place")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> placeEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @Valid @RequestBody EquipmentPlacementRequest request) {

        rackElevationService.placeEquipment(id, equipmentId, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 배치 완료", null));
    }

    // 장비 이동
    @PutMapping("/{id}/equipment/{equipmentId}/move")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> moveEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @Valid @RequestBody EquipmentMoveRequest request) {

        rackElevationService.moveEquipment(id, equipmentId, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 이동 완료", null));
    }

    // 장비 배치 검증
    @PostMapping("/{id}/validate-placement")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> validateEquipmentPlacement(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @Valid @RequestBody EquipmentPlacementRequest request) {

        Map<String, Object> validationResult = rackElevationService.validateEquipmentPlacement(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "배치 검증 완료", validationResult));
    }

    // 랙 사용률 조회
    @GetMapping("/{id}/utilization")
    public ResponseEntity<CommonResDto> getRackUtilization(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id) {

        RackUtilizationResponse utilization = rackElevationService.getRackUtilization(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 사용률 조회 완료", utilization));
    }
}
