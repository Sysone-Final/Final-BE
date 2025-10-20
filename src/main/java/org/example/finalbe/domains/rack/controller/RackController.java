package org.example.finalbe.domains.rack.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.service.RackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 랙 기본 CRUD 컨트롤러
 */
@RestController
@RequestMapping("/racks")
@RequiredArgsConstructor
public class RackController {

    private final RackService rackService;

    /**
     * 특정 데이터센터의 랙 목록을 조회하는 기능
     * 상태, 부서, 정렬 기준으로 필터링 가능
     * 권한: 모든 사용자 접근 가능
     */
    @GetMapping("/datacenter/{dataCenterId}")
    public ResponseEntity<CommonResDto> getRacksByDataCenter(
            @PathVariable Long dataCenterId,
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
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getRackById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        RackDetailResponse rack = rackService.getRackById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 조회 완료", rack));
    }

    /**
     * 새로운 랙을 생성하는 기능
     * 랙 이름, 위치, 용량 등의 정보를 받아서 신규 랙 등록
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createRack(@RequestBody RackCreateRequest request) {
        RackDetailResponse rack = rackService.createRack(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "랙 생성 완료", rack));
    }

    /**
     * 기존 랙의 정보를 수정하는 기능
     * 랙의 이름, 위치, 담당자 등을 변경
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateRack(
            @PathVariable Long id,
            @RequestBody RackUpdateRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        RackDetailResponse rack = rackService.updateRack(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 수정 완료", rack));
    }

    /**
     * 랙을 삭제하는 기능
     * 랙 정보를 시스템에서 제거
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteRack(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        rackService.deleteRack(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 삭제 완료", null));
    }

    /**
     * 랙의 상태를 변경하는 기능
     * 예: 정상 운영 중 -> 점검 중, 점검 중 -> 비활성 등
     * 권한: ADMIN 또는 OPERATOR만 가능
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeRackStatus(
            @PathVariable Long id,
            @RequestBody RackStatusChangeRequest request) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        RackDetailResponse rack = rackService.changeRackStatus(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 상태 변경 완료", rack));
    }

    /**
     * 키워드로 랙을 검색하는 기능
     * 랙 이름이나 위치 등에서 키워드를 찾아서 해당하는 랙 목록 반환
     * 데이터센터로 추가 필터링 가능
     * 권한: 모든 사용자 접근 가능
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchRacks(
            @RequestParam String keyword,
            @RequestParam(required = false) Long dataCenterId) {

        List<RackListResponse> racks = rackService.searchRacks(keyword, dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 검색 완료", racks));
    }

    /**
     * 특정 담당자가 관리하는 랙 목록을 조회하는 기능
     * 담당자 ID로 그 사람이 맡은 모든 랙을 찾아서 반환
     * 권한: 모든 사용자 접근 가능
     */
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<CommonResDto> getRacksByManager(@PathVariable Long managerId) {
        List<RackListResponse> racks = rackService.getRacksByManager(managerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "담당자별 랙 목록 조회 완료", racks));
    }

    /**
     * 특정 부서의 랙 목록을 조회하는 기능
     * 부서명으로 해당 부서에 속한 모든 랙을 찾아서 반환
     * 권한: 모든 사용자 접근 가능
     */
    @GetMapping("/department/{department}")
    public ResponseEntity<CommonResDto> getRacksByDepartment(@PathVariable String department) {
        List<RackListResponse> racks = rackService.getRacksByDepartment(department);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서별 랙 목록 조회 완료", racks));
    }
}