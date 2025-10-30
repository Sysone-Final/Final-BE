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
 * 랙 관리 컨트롤러
 * 랙 CRUD 및 검색 API 제공
 */
@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
@Validated
public class RackController {

    private final RackService rackService;

    /**
     * 전산실별 랙 목록 조회
     * GET /api/racks/datacenter/{dataCenterId}
     *
     * @param dataCenterId 전산실 ID
     * @param status 랙 상태 필터 (선택)
     * @param sortBy 정렬 기준 (기본값: name)
     * @return 랙 목록
     */
    @GetMapping("/datacenter/{dataCenterId}")
    public ResponseEntity<CommonResDto> getRacksByDataCenter(
            @PathVariable Long dataCenterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "name") String sortBy) {

        List<RackListResponse> racks = rackService.getRacksByDataCenter(dataCenterId, status, sortBy);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 목록 조회 완료", racks));
    }

    /**
     * 랙 상세 조회
     * GET /api/racks/{id}
     *
     * @param id 랙 ID
     * @return 랙 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getRackById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id) {

        RackDetailResponse rack = rackService.getRackById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 조회 완료", rack));
    }

    /**
     * 랙 검색
     * GET /api/racks/search
     *
     * @param keyword 검색 키워드 (랙 이름, 그룹 번호, 위치)
     * @param dataCenterId 전산실 ID (선택)
     * @return 검색된 랙 목록
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
     * GET /api/racks/manager/{managerId}
     *
     * @param managerId 담당자 ID
     * @return 담당자별 랙 목록
     */
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<CommonResDto> getRacksByManager(
            @PathVariable @Min(value = 1, message = "유효하지 않은 담당자 ID입니다.") Long managerId) {

        List<RackListResponse> racks = rackService.getRacksByManager(managerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "담당자별 랙 목록 조회 완료", racks));
    }

    /**
     * 부서별 랙 목록 조회
     * GET /api/racks/department/{departmentId}
     *
     * @param departmentId 부서 ID
     * @return 부서별 랙 목록
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<CommonResDto> getRacksByDepartment(
            @PathVariable @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") Long departmentId) {

        List<RackListResponse> racks = rackService.getRacksByDepartment(departmentId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서별 랙 목록 조회 완료", racks));
    }

    /**
     * 랙 생성
     * POST /api/racks
     *
     * @param request 랙 생성 요청 DTO
     * @return 생성된 랙 정보
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
     * PUT /api/racks/{id}
     *
     * @param id 랙 ID
     * @param request 랙 수정 요청 DTO
     * @return 수정된 랙 정보
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
     * DELETE /api/racks/{id}
     *
     * @param id 랙 ID
     * @return 삭제 완료 메시지
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
     * PUT /api/racks/{id}/status
     *
     * @param id 랙 ID
     * @param request 상태 변경 요청 DTO
     * @return 수정된 랙 정보
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeRackStatus(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id,
            @Valid @RequestBody RackStatusChangeRequest request) {

        RackDetailResponse rack = rackService.changeRackStatus(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 상태 변경 완료", rack));
    }
}