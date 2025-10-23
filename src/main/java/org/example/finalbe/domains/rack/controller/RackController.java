package org.example.finalbe.domains.rack.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.service.RackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 랙 기본 CRUD 컨트롤러
 *
 * 개선사항:
 * - Bean Validation 적용
 * - department를 ID 기반으로 변경 (드롭다운 선택용)
 * - 모든 서비스 메서드 활용
 * - 중복 검증 제거
 */
@RestController
@RequestMapping("/racks")
@RequiredArgsConstructor
@Validated
public class RackController {

    private final RackService rackService;

    /**
     * 전산실별 랙 목록 조회
     * 상태, 부서(ID), 정렬 기준으로 필터링 가능
     * 권한: 모든 사용자 접근 가능
     *
     * @param dataCenterId 전산실 ID
     * @param status 랙 상태 (optional)
     * @param departmentId 부서 ID (optional) - 드롭다운에서 선택한 부서 ID
     * @param sortBy 정렬 기준 (name, usage, power)
     */
    @GetMapping("/datacenter/{dataCenterId}")
    public ResponseEntity<CommonResDto> getRacksByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false, defaultValue = "name") String sortBy) {

        List<RackListResponse> racks = rackService.getRacksByDataCenter(dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 목록 조회 완료", racks));
    }

    /**
     * 랙 상세 조회
     * 권한: 모든 사용자 접근 가능
     *
     * @param id 랙 ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getRackById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id) {

        RackDetailResponse rack = rackService.getRackById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 조회 완료", rack));
    }

    /**
     * 랙 생성
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param request 랙 생성 요청 (department 필드 제거됨)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createRack(@Valid @RequestBody RackCreateRequest request) {
        RackDetailResponse rack = rackService.createRack(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "랙 생성 완료", rack));
    }

    /**
     * 랙 수정
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 랙 ID
     * @param request 랙 수정 요청
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateRack(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @Valid @RequestBody RackUpdateRequest request) {

        RackDetailResponse rack = rackService.updateRack(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 수정 완료", rack));
    }

    /**
     * 랙 삭제 (소프트 삭제)
     * 권한: ADMIN만 가능
     *
     * @param id 랙 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteRack(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id) {

        rackService.deleteRack(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 삭제 완료", null));
    }

    /**
     * 랙 상태 변경
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 랙 ID
     * @param request 상태 변경 요청
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeRackStatus(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @Valid @RequestBody RackStatusChangeRequest request) {

        RackDetailResponse rack = rackService.changeRackStatus(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 상태 변경 완료", rack));
    }

    /**
     * 랙 검색
     * 키워드로 랙 이름, 그룹 번호, 위치 검색
     * 권한: 모든 사용자 접근 가능
     *
     * @param keyword 검색 키워드
     * @param dataCenterId 전산실 ID (optional)
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchRacks(
            @RequestParam @NotBlank(message = "검색 키워드를 입력해주세요.") String keyword,
            @RequestParam(required = false) Long dataCenterId) {

        List<RackListResponse> racks = rackService.searchRacks(keyword, dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 검색 완료", racks));
    }

    /**
     * 담당자별 랙 목록 조회
     * 권한: 모든 사용자 접근 가능
     *
     * @param managerId 담당자(회원) ID
     */
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<CommonResDto> getRacksByManager(
            @PathVariable @Min(value = 1, message = "유효하지 않은 담당자 ID입니다.") Long managerId) {

        List<RackListResponse> racks = rackService.getRacksByManager(managerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "담당자별 랙 목록 조회 완료", racks));
    }

    /**
     * 부서별 랙 목록 조회
     * 드롭다운에서 선택한 부서 ID로 조회
     * 권한: 모든 사용자 접근 가능
     *
     * @param departmentId 부서 ID
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<CommonResDto> getRacksByDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long departmentId) {

        List<RackListResponse> racks = rackService.getRacksByDepartment(departmentId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서별 랙 목록 조회 완료", racks));
    }
}