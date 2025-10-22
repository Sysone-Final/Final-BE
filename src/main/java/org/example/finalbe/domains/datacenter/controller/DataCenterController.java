package org.example.finalbe.domains.datacenter.controller;


import lombok.RequiredArgsConstructor;

import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.service.DataCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/datacenters")
@RequiredArgsConstructor
public class DataCenterController {

    private final DataCenterService dataCenterService;

    /**
     * 접근 가능한 전산실 목록 조회
     * GET /datacenters
     * 권한: 모든 인증된 사용자 (ADMIN, OPERATOR, VIEWER)
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAccessibleDataCenters() {
        List<DataCenterListResponse> datacenters = dataCenterService.getAccessibleDataCenters();
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실 목록 조회 완료",
                datacenters
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 전산실 상세 조회
     * GET /datacenters/{id}
     * 권한: 모든 인증된 사용자 (ADMIN, OPERATOR, VIEWER)
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getDataCenterById(@PathVariable Long id) {
        DataCenterDetailResponse datacenter = dataCenterService.getDataCenterById(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실 조회 완료",
                datacenter
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 전산실 생성
     * POST /datacenters
     * 권한: ADMIN, OPERATOR
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createDataCenter(@RequestBody DataCenterCreateRequest request) {
        DataCenterDetailResponse datacenter = dataCenterService.createDataCenter(request);
        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED,
                "전산실 생성 완료",
                datacenter
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 전산실 수정
     * PUT /datacenters/{id}
     * 권한: ADMIN, OPERATOR
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDataCenter(
            @PathVariable Long id,
            @RequestBody DataCenterUpdateRequest request) {
        DataCenterDetailResponse datacenter = dataCenterService.updateDataCenter(id, request);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실 수정 완료",
                datacenter
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 전산실 삭제
     * DELETE /datacenters/{id}
     * 권한: ADMIN, OPERATOR
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteDataCenter(@PathVariable Long id) {
        dataCenterService.deleteDataCenter(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실 삭제 완료",
                null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 전산실 검색
     * GET /datacenters/search?name={name}
     * 권한: 모든 인증된 사용자 (ADMIN, OPERATOR, VIEWER)
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchDataCenters(@RequestParam String name) {
        List<DataCenterListResponse> datacenters = dataCenterService.searchDataCentersByName(name);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실 검색 완료",
                datacenters
        );
        return ResponseEntity.ok(response);
    }
}
