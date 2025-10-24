package org.example.finalbe.domains.equipment.controller;

import jakarta.validation.Valid;
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

import java.util.List;

/**
 * 장비 CRUD 컨트롤러
 */
@RestController
@RequestMapping("/equipments")
@RequiredArgsConstructor
@Validated
public class EquipmentController {

    private final EquipmentService equipmentService;

    /**
     * 특정 랙에 설치된 장비 목록을 조회하는 기능
     * 상태, 타입, 정렬 기준으로 필터링 가능
     * 예: 특정 랙의 모든 서버 장비만 보기
     * 권한: 모든 사용자 접근 가능
     */
    @GetMapping("/rack/{rackId}")
    public ResponseEntity<CommonResDto> getEquipmentsByRack(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "name") String sortBy) {

        List<EquipmentListResponse> equipments = equipmentService.getEquipmentsByRack(
                rackId, status, type, sortBy);

        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 목록 조회 완료", equipments));
    }

    /**
     * 특정 장비의 상세 정보를 조회하는 기능
     * 장비의 모델명, 시리얼 번호, 설치 위치, 상태 등 모든 정보 제공
     * 권한: 모든 사용자 접근 가능
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getEquipmentById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id) {

        EquipmentDetailResponse equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 조회 완료", equipment));
    }

    /**
     * 새로운 장비를 등록하는 기능
     * 장비 이름, 모델, 타입, 시리얼 번호 등의 정보를 입력받아 생성
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createEquipment(@Valid @RequestBody EquipmentCreateRequest request) {
        EquipmentDetailResponse equipment = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "장비 생성 완료", equipment));
    }

    /**
     * 기존 장비의 정보를 수정하는 기능
     * 장비의 이름, 모델, 담당자 등을 변경
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id,
            @Valid @RequestBody EquipmentUpdateRequest request) {

        EquipmentDetailResponse equipment = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 수정 완료", equipment));
    }

    /**
     * 장비를 삭제하는 기능 (소프트 삭제)
     * 실제로 데이터를 지우지 않고 삭제 표시만 함
     * 권한: ADMIN만 가능
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id) {

        equipmentService.deleteEquipment(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 삭제 완료", null));
    }

    /**
     * 장비의 상태를 변경하는 기능
     * 예: NORMAL → MAINTENANCE, MAINTENANCE → ERROR 등
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeEquipmentStatus(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id,
            @Valid @RequestBody EquipmentStatusChangeRequest request) {

        EquipmentDetailResponse equipment = equipmentService.changeEquipmentStatus(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 상태 변경 완료", equipment));
    }
}