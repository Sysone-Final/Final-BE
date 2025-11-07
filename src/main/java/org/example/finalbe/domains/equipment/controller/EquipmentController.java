package org.example.finalbe.domains.equipment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
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
     * GET /api/equipments?page=0&size=10&keyword=&type=&status=&serverRoomId=
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getEquipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EquipmentType type,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) Long serverRoomId) {

        EquipmentPageResponse response = equipmentService.getEquipmentsWithFilters(
                page, size, keyword, type, status, serverRoomId);

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
     * 서버실별 장비 목록 조회
     * GET /api/equipments/serverroom/{serverRoomId}
     */
    @GetMapping("/serverroom/{serverRoomId}")
    public ResponseEntity<CommonResDto> getEquipmentByServerRoom(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        List<EquipmentListResponse> equipments = equipmentService.getEquipmentsByServerRoom(
                serverRoomId, status, type);
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
     * 장비 생성 - @RequestBody로 변경
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createEquipment(
            @RequestBody @Valid EquipmentCreateRequest request) {

        EquipmentDetailResponse equipment = equipmentService.createEquipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "장비 생성 완료", equipment));
    }


    /**
     * 장비 수정 - @RequestBody로 변경
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateEquipment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long id,
            @RequestBody @Valid EquipmentUpdateRequest request) {

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

    /**
     * 🆕 장비 대량 상태 변경
     * PUT /api/equipments/status
     * Body: {"ids": [1, 2, 3], "status": "MAINTENANCE"}
     */
    @PutMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateMultipleEquipmentStatus(
            @RequestBody EquipmentStatusBulkUpdateRequest request) {

        if (request.ids() == null || request.ids().isEmpty()) {
            throw new IllegalArgumentException("장비 ID 목록이 비어있습니다.");
        }
        if (request.status() == null || request.status().trim().isEmpty()) {
            throw new IllegalArgumentException("변경할 상태를 입력해주세요.");
        }

        EquipmentStatusBulkUpdateResponse response =
                equipmentService.updateMultipleEquipmentStatus(request);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장비 상태 일괄 변경 완료", response));
    }
}