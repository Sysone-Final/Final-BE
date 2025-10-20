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
     * 특정 랙에 설치된 장비 목록을 조회하는 기능
     * 상태, 타입, 정렬 기준으로 필터링 가능
     * 예: 특정 랙의 모든 서버 장비만 보기
     * 권한: 모든 사용자 접근 가능
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
     * 특정 장비의 상세 정보를 조회하는 기능
     * 장비의 모델명, 시리얼 번호, 설치 위치, 상태 등 모든 정보 제공
     * 권한: 모든 사용자 접근 가능
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
     * 새로운 장비를 등록하는 기능
     * 장비 이름, 모델, 타입, 시리얼 번호 등의 정보를 입력받아 생성
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createEquipment(@RequestBody EquipmentCreateRequest request) {
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
            @PathVariable Long id,
            @RequestBody EquipmentUpdateRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        EquipmentDetailResponse equipment = equipmentService.updateEquipment(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "장비 수정 완료", equipment));
    }

    /**
     * 장비를 삭제하는 기능
     * 장비 정보를 시스템에서 제거
     * 권한: ADMIN 또는 OPERATOR만 가능
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
     * 장비의 상태를 변경하는 기능
     * 예: 정상 운영 중 -> 점검 중, 점검 중 -> 고장 등
     * 권한: ADMIN 또는 OPERATOR만 가능
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
     * 특정 데이터센터에 있는 모든 장비를 조회하는 기능
     * 해당 데이터센터의 모든 랙에 설치된 장비들을 한 번에 확인
     * 상태와 타입으로 필터링 가능
     * 권한: 모든 사용자 접근 가능
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
     * 키워드로 장비를 검색하는 기능
     * 장비 이름, 모델명, 시리얼 번호 등에서 키워드를 찾아서 매칭되는 장비 목록 반환
     * 타입과 상태로 추가 필터링 가능
     * 권한: 모든 사용자 접근 가능
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