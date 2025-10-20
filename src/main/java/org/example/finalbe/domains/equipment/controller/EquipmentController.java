package org.example.finalbe.domains.equipment.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.equipment.dto.*;
import org.example.finalbe.domains.equipment.service.EquipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 장비 CRUD 컨트롤러
 */
@RestController
@RequestMapping("/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    /**
     * 랙별 장비 목록 조회
     */
    @GetMapping("/rack/{rackId}")
    public ResponseEntity<CommonResDto> getEquipmentsByRack(
            @PathVariable Long rackId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "name") String sortBy) {

        List<EquipmentListResponse> equipments = equipmentService.getEquipmentsByRack(
                rackId, status, type, sortBy);

        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 목록 조회 완료", equipments));
    }

    /**
     * 장비 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getEquipmentById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        EquipmentDetailResponse equipment = equipmentService.getEquipmentById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 조회 완료", equipment));
    }

    /**
     * 장비 생성
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createEquipment(@RequestBody EquipmentCreateRequest request) {
        EquipmentDetailResponse equipment = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "장비 생성 완료", equipment));
    }

    /**
     * 장비 수정
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateEquipment(
            @PathVariable Long id,
            @RequestBody EquipmentUpdateRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        EquipmentDetailResponse equipment = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 수정 완료", equipment));
    }

    /**
     * 장비 삭제
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteEquipment(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        equipmentService.deleteEquipment(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 삭제 완료", null));
    }

    /**
     * 장비 상태 변경
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeEquipmentStatus(
            @PathVariable Long id,
            @RequestBody EquipmentStatusChangeRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        EquipmentDetailResponse equipment = equipmentService.changeEquipmentStatus(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 상태 변경 완료", equipment));
    }

    /**
     * 전산실별 장비 목록 조회
     */
    @GetMapping("/datacenter/{datacenterId}")
    public ResponseEntity<CommonResDto> getEquipmentsByDatacenter(
            @PathVariable Long datacenterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        List<EquipmentListResponse> equipments = equipmentService.getEquipmentsByDatacenter(
                datacenterId, status, type);

        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "전산실 장비 목록 조회 완료", equipments));
    }

    /**
     * 장비 검색
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchEquipments(
            @RequestParam String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {

        List<EquipmentListResponse> equipments = equipmentService.searchEquipments(keyword, type, status);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 검색 완료", equipments));
    }
}