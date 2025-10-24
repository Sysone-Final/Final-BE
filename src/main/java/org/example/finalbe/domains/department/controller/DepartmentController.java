package org.example.finalbe.domains.department.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.department.dto.*;
import org.example.finalbe.domains.department.service.DepartmentService;
import org.example.finalbe.domains.rack.dto.RackListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 부서 관리 컨트롤러
 */
@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@Validated
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 회사별 부서 목록 조회
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * @param companyId 회사 ID
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<CommonResDto> getDepartmentsByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId) {

        List<DepartmentListResponse> departments = departmentService.getDepartmentsByCompany(companyId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 목록 조회 완료", departments));
    }

    /**
     * 부서 상세 조회
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * @param id 부서 ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getDepartmentById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long id) {

        DepartmentDetailResponse department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 조회 완료", department));
    }

    /**
     * 부서 생성
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param request 부서 생성 요청
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentDetailResponse department = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "부서 생성 완료", department));
    }

    /**
     * 부서 수정
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 부서 ID
     * @param request 부서 수정 요청
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long id,
            @Valid @RequestBody DepartmentUpdateRequest request) {

        DepartmentDetailResponse department = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 수정 완료", department));
    }

    /**
     * 부서 삭제 (소프트 삭제)
     * 권한: ADMIN만 가능
     *
     * @param id 부서 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long id) {

        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 삭제 완료", null));
    }

    /**
     * 부서 검색 (회사 내)
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * @param companyId 회사 ID
     * @param keyword 검색 키워드
     */
    @GetMapping("/company/{companyId}/search")
    public ResponseEntity<CommonResDto> searchDepartments(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId,
            @RequestParam @NotBlank(message = "검색 키워드를 입력해주세요.") String keyword) {

        List<DepartmentListResponse> departments = departmentService.searchDepartments(companyId, keyword);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서 검색 완료", departments));
    }

    /**
     * 회원을 부서에 추가
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param request 회원-부서 매핑 요청
     */
    @PostMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> addMemberToDepartment(@Valid @RequestBody MemberDepartmentCreateRequest request) {
        departmentService.addMemberToDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "회원이 부서에 추가되었습니다.", null));
    }

    /**
     * 부서에서 회원 제거
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param memberId 회원 ID
     * @param departmentId 부서 ID
     */
    @DeleteMapping("/members/{memberId}/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> removeMemberFromDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long memberId,
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long departmentId) {

        departmentService.removeMemberFromDepartment(memberId, departmentId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "회원이 부서에서 제거되었습니다.", null));
    }

    /**
     * 랙을 부서에 추가
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param request 랙-부서 매핑 요청
     */
    @PostMapping("/racks")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> addRackToDepartment(@Valid @RequestBody RackDepartmentCreateRequest request) {
        departmentService.addRackToDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "랙이 부서에 추가되었습니다.", null));
    }

    /**
     * 부서에서 랙 제거
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param rackId 랙 ID
     * @param departmentId 부서 ID
     */
    @DeleteMapping("/racks/{rackId}/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> removeRackFromDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId,
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long departmentId) {

        departmentService.removeRackFromDepartment(rackId, departmentId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙이 부서에서 제거되었습니다.", null));
    }

    /**
     * 부서별 랙 목록 조회
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * @param departmentId 부서 ID
     */
    @GetMapping("/{departmentId}/racks")
    public ResponseEntity<CommonResDto> getRacksByDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long departmentId) {

        List<RackListResponse> racks = departmentService.getRacksByDepartment(departmentId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서별 랙 목록 조회 완료", racks));
    }
}