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
 * 실장도(Elevation View): 랙의 1U~42U 유닛을 시각적으로 표시
 */
@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
@Validated
public class RackElevationController {

    private final RackElevationService rackElevationService;

    /**
     * 랙의 실장도(배치도)를 조회하는 기능
     * 랙 안에 어떤 장비가 몇 번 유닛에 설치되어 있는지 시각적으로 보여줌
     * 앞면(FRONT) 또는 뒷면(REAR) 뷰 선택 가능
     * 권한: 모든 사용자 접근 가능
     */
    @GetMapping("/{id}/elevation")
    public ResponseEntity<CommonResDto> getRackElevation(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @RequestParam(required = false, defaultValue = "FRONT") String view) {

        RackElevationResponse elevation = rackElevationService.getRackElevation(id, view);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 실장도 조회 완료", elevation));
    }

    /**
     * 랙에 장비를 배치하는 기능
     * 특정 장비를 랙의 원하는 유닛 위치에 설치
     * 시작 유닛 위치와 장비가 차지할 유닛 개수를 지정
     * 권한: ADMIN 또는 OPERATOR만 가능
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
     * 랙 내에서 장비의 위치를 이동시키는 기능
     * 이미 설치된 장비를 다른 유닛 위치로 옮김
     * 예: 10번 유닛에 있던 장비를 20번 유닛으로 이동
     * 권한: ADMIN 또는 OPERATOR만 가능
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
     * 장비 배치가 가능한지 미리 검증하는 기능
     * 실제로 배치하기 전에 해당 위치에 공간이 충분한지, 다른 장비와 겹치지 않는지 확인
     * 배치 가능 여부와 문제점을 미리 알려줌
     * 권한: ADMIN 또는 OPERATOR만 가능
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
     * 랙의 사용률 정보를 조회하는 기능
     * 전체 유닛 중 몇 개가 사용 중인지, 몇 개가 비어있는지 등의 통계 정보 제공
     * 예: 42U 중 30U 사용 중 (71% 사용률)
     * 권한: 모든 사용자 접근 가능
     */
    @GetMapping("/{id}/utilization")
    public ResponseEntity<CommonResDto> getRackUtilization(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id) {

        RackUtilizationResponse utilization = rackElevationService.getRackUtilization(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 사용률 조회 완료", utilization));
    }
}