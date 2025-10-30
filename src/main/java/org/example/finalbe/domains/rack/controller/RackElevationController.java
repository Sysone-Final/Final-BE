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

/**
 * 랙 실장도 관리 컨트롤러
 * 장비 배치 및 이동 API 제공
 */
@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
@Validated
public class RackElevationController {

    private final RackElevationService rackElevationService;

    /**
     * 랙 실장도 조회
     * GET /api/racks/{id}/elevation
     *
     * @param id 랙 ID
     * @param view 뷰 타입 (FRONT/REAR, 기본값: FRONT)
     * @return 랙 실장도 정보 (유닛별 장비 배치 현황)
     */
    @GetMapping("/{id}/elevation")
    public ResponseEntity<CommonResDto> getRackElevation(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @RequestParam(required = false, defaultValue = "FRONT") String view) {

        RackElevationResponse elevation = rackElevationService.getRackElevation(id, view);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 실장도 조회 완료", elevation));
    }

    /**
     * 장비 배치
     * POST /api/racks/{id}/equipment/{equipmentId}/place
     *
     * @param id 랙 ID
     * @param equipmentId 장비 ID
     * @param request 배치 요청 DTO (시작 유닛, 유닛 크기 등)
     * @return 배치 완료 메시지
     */
    @PostMapping("/{id}/equipment/{equipmentId}/place")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> placeEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @Valid @RequestBody EquipmentPlacementRequest request) {

        rackElevationService.placeEquipment(id, equipmentId, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 배치 완료", null));
    }

    /**
     * 장비 이동
     * PUT /api/racks/{id}/equipment/{equipmentId}/move
     *
     * @param id 랙 ID
     * @param equipmentId 장비 ID
     * @param request 이동 요청 DTO (이전 유닛, 이동할 유닛)
     * @return 이동 완료 메시지
     */
    @PutMapping("/{id}/equipment/{equipmentId}/move")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> moveEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @Valid @RequestBody EquipmentMoveRequest request) {

        rackElevationService.moveEquipment(id, equipmentId, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 이동 완료", null));
    }

    /**
     * 장비 배치 검증
     * POST /api/racks/{id}/validate-placement
     *
     * @param id 랙 ID
     * @param request 배치 요청 DTO
     * @return 배치 가능 여부 및 검증 결과
     */
    @PostMapping("/{id}/validate-placement")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> validateEquipmentPlacement(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @Valid @RequestBody EquipmentPlacementRequest request) {

        Map<String, Object> validationResult = rackElevationService.validateEquipmentPlacement(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "배치 검증 완료", validationResult));
    }

    /**
     * 랙 사용률 조회
     * GET /api/racks/{id}/utilization
     *
     * @param id 랙 ID
     * @return 랙 사용률 정보 (유닛, 전력, 중량 사용률)
     */
    @GetMapping("/{id}/utilization")
    public ResponseEntity<CommonResDto> getRackUtilization(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id) {

        RackUtilizationResponse utilization = rackElevationService.getRackUtilization(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 사용률 조회 완료", utilization));
    }
}