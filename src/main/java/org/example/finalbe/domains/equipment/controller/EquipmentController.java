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
 * 장비 관리 컨트롤러
 * 랙 내 장비의 생성, 조회, 수정, 삭제 API 제공
 */
@RestController
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
@Validated
public class EquipmentController {

    private final EquipmentService equipmentService;

    /**
     * 랙별 장비 목록 조회
     * GET /api/equipments/rack/{rackId}
     *
     * @param rackId 랙 ID
     * @param status 상태 필터 (선택)
     * @param type 타입 필터 (선택)
     * @param sortBy 정렬 기준 (선택)
     * @return 장비 목록
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
     * 장비 상세 조회
     * GET /api/equipments/{id}
     *
     * @param id 장비 ID
     * @return 장비 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getEquipmentById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id) {

        EquipmentDetailResponse equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 조회 완료", equipment));
    }

    /**
     * 전산실별 장비 목록 조회
     * GET /api/equipments/datacenter/{datacenterId}
     *
     * @param datacenterId 전산실 ID
     * @param status 상태 필터 (선택)
     * @param type 타입 필터 (선택)
     * @return 장비 목록
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
     *
     * @param type 타입 필터 (선택)
     * @param status 상태 필터 (선택)
     * @param keyword 검색 키워드 (선택)
     * @return 장비 목록
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
     *
     * @param request 장비 생성 요청 DTO
     * @return 생성된 장비 정보
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createEquipment(@Valid @RequestBody EquipmentCreateRequest request) {
        EquipmentDetailResponse equipment = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "장비 생성 완료", equipment));
    }

    /**
     * 장비 수정
     * PUT /api/equipments/{id}
     *
     * @param id 장비 ID
     * @param request 장비 수정 요청 DTO
     * @return 수정된 장비 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id,
            @Valid @RequestBody EquipmentUpdateRequest request) {

        EquipmentDetailResponse equipment = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 수정 완료", equipment));
    }

    /**
     * 장비 삭제
     * DELETE /api/equipments/{id}
     *
     * @param id 장비 ID
     * @return 삭제 완료 메시지
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
     * 장비 상태 변경
     * PUT /api/equipments/{id}/status
     *
     * @param id 장비 ID
     * @param request 상태 변경 요청 DTO
     * @return 상태 변경된 장비 정보
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeEquipmentStatus(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id,
            @Valid @RequestBody EquipmentStatusChangeRequest request) {

        EquipmentDetailResponse equipment = equipmentService.changeEquipmentStatus(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 상태 변경 완료", equipment));
    }
}