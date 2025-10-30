package org.example.finalbe.domains.department.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.department.dto.*;
import org.example.finalbe.domains.department.service.DepartmentService;
import org.example.finalbe.domains.rack.dto.RackListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 부서 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Validated
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 회사별 부서 목록 조회
     * GET /api/departments/company/{companyId}
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<CommonResDto> getDepartmentsByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId) {
        List<DepartmentListResponse> departments = departmentService.getDepartmentsByCompany(companyId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 목록 조회 완료", departments));
    }

    /**
     * 부서 상세 조회
     * GET /api/departments/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getDepartmentById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long id) {
        DepartmentDetailResponse department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 조회 완료", department));
    }

    /**
     * 부서 생성
     * POST /api/departments
     */
    @PostMapping
    public ResponseEntity<CommonResDto> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentDetailResponse department = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "부서 생성 완료", department));
    }

    /**
     * 부서 수정
     * PUT /api/departments/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommonResDto> updateDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {
        DepartmentDetailResponse department = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 수정 완료", department));
    }

    /**
     * 부서 삭제
     * DELETE /api/departments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResDto> deleteDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 삭제 완료", null));
    }

    /**
     * 회원-부서 매핑 생성
     * POST /api/departments/members
     */
    @PostMapping("/members")
    public ResponseEntity<CommonResDto> assignMemberToDepartment(
            @Valid @RequestBody MemberDepartmentCreateRequest request) {
        departmentService.assignMemberToDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "회원-부서 매핑 생성 완료", null));
    }

    /**
     * 회원-부서 매핑 삭제
     * DELETE /api/departments/members/{memberId}/{departmentId}
     */
    @DeleteMapping("/members/{memberId}/{departmentId}")
    public ResponseEntity<CommonResDto> removeMemberFromDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long memberId,
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long departmentId) {
        departmentService.removeMemberFromDepartment(memberId, departmentId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "회원-부서 매핑 삭제 완료", null));
    }

    /**
     * 랙-부서 매핑 생성
     * POST /api/departments/racks
     */
    @PostMapping("/racks")
    public ResponseEntity<CommonResDto> assignRackToDepartment(
            @Valid @RequestBody RackDepartmentCreateRequest request) {
        departmentService.assignRackToDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "랙-부서 매핑 생성 완료", null));
    }

    /**
     * 랙-부서 매핑 삭제
     * DELETE /api/departments/racks/{rackId}/{departmentId}
     */
    @DeleteMapping("/racks/{rackId}/{departmentId}")
    public ResponseEntity<CommonResDto> removeRackFromDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId,
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long departmentId) {
        departmentService.removeRackFromDepartment(rackId, departmentId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙-부서 매핑 삭제 완료", null));
    }

    /**
     * 부서별 랙 목록 조회
     * GET /api/departments/{departmentId}/racks
     */
    @GetMapping("/{departmentId}/racks")
    public ResponseEntity<CommonResDto> getRacksByDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long departmentId) {
        List<RackListResponse> racks = departmentService.getRacksByDepartment(departmentId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서별 랙 목록 조회 완료", racks));
    }
}