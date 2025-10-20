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
     * 사용자가 접근 가능한 전산실(데이터센터) 목록을 조회하는 기능
     * 사용자의 권한과 소속에 따라 볼 수 있는 전산실 목록이 결정됨
     * 권한: 모든 인증된 사용자 접근 가능
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
     * 특정 전산실의 상세 정보를 조회하는 기능
     * 전산실 이름, 위치, 담당자, 랙 개수 등의 세부 정보 제공
     * 권한: 모든 인증된 사용자 접근 가능
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
     * 새로운 전산실을 생성하는 기능
     * 전산실 이름, 위치, 주소, 담당자 등의 정보를 입력받아 신규 전산실 등록
     * 권한: ADMIN 또는 OPERATOR만 가능
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
     * 기존 전산실의 정보를 수정하는 기능
     * 전산실의 이름, 위치, 담당자 등을 변경
     * 권한: ADMIN 또는 OPERATOR만 가능
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
     * 전산실을 삭제하는 기능
     * 전산실 정보를 시스템에서 제거
     * 주의: 전산실 내의 랙과 장비가 모두 제거되어야 삭제 가능할 수 있음
     * 권한: ADMIN 또는 OPERATOR만 가능
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
     * 전산실 이름으로 검색하는 기능
     * 입력한 키워드가 포함된 이름을 가진 전산실 목록 반환
     * 권한: 모든 인증된 사용자 접근 가능
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