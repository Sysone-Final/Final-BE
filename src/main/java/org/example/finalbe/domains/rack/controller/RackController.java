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

    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getRackById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        RackDetailResponse rack = rackService.getRackById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 조회 완료", rack));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createRack(@RequestBody RackCreateRequest request) {
        RackDetailResponse rack = rackService.createRack(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "랙 생성 완료", rack));
    }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteRack(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        rackService.deleteRack(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 삭제 완료", null));
    }

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

    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchRacks(
            @RequestParam String keyword,
            @RequestParam(required = false) Long dataCenterId) {

        List<RackListResponse> racks = rackService.searchRacks(keyword, dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 검색 완료", racks));
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<CommonResDto> getRacksByManager(@PathVariable String managerId) {
        List<RackListResponse> racks = rackService.getRacksByManager(managerId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "담당자별 랙 목록 조회 완료", racks));
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<CommonResDto> getRacksByDepartment(@PathVariable String department) {
        List<RackListResponse> racks = rackService.getRacksByDepartment(department);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "부서별 랙 목록 조회 완료", racks));
    }
}