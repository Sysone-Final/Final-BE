package org.example.finalbe.domains.rack.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
 * - Controller 중복 검증 제거
 * - @Valid를 통한 Request DTO 검증
 */
@RestController
@RequestMapping("/racks")
@RequiredArgsConstructor
@Validated
public class RackController {

    private final RackService rackService;

    /**
     * 특정 데이터센터의 랙 목록을 조회하는 기능
     * 상태, 부서, 정렬 기준으로 필터링 가능
     * 권한: 모든 사용자 접근 가능
     *
     * @param dataCenterId 전산실 ID
     * @param status 랙 상태 (optional)
     * @param department 부서명 (optional)
     * @param sortBy 정렬 기준 (default: name)
     */
    @GetMapping("/datacenter/{dataCenterId}")
    public ResponseEntity<CommonResDto> getRacksByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false, defaultValue = "name") String sortBy) {

        List<RackListResponse> racks = rackService.getRacksByDataCenter(
                dataCenterId, status, department, sortBy);

        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 목록 조회 완료", racks));
    }

    /**
     * 특정 랙의 상세 정보를 조회하는 기능
     * ID로 랙 하나를 찾아서 자세한 정보를 반환
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
     * 새로운 랙을 생성하는 기능
     * 랙 이름, 위치, 용량 등의 정보를 받아서 신규 랙 등록
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param request 랙 생성 요청 (Validation 적용)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createRack(@Valid @RequestBody RackCreateRequest request) {
        RackDetailResponse rack = rackService.createRack(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "랙 생성 완료", rack));
    }

    /**
     * 기존 랙의 정보를 수정하는 기능
     * 랙의 이름, 위치, 담당자 등을 변경
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 랙 ID
     * @param request 랙 수정 요청 (Validation 적용)
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
     * 랙을 삭제하는 기능
     * 실제로는 소프트 삭제(delYn = Y)로 처리
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 랙 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteRack(
            @PathVariable @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long id) {

        rackService.deleteRack(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 삭제 완료", null));
    }
}