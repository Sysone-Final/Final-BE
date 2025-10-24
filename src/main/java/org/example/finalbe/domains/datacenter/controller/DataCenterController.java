package org.example.finalbe.domains.datacenter.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.service.DataCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 전산실(데이터센터) 관리 컨트롤러
 *
 * 개선사항:
 * - Bean Validation 적용
 * - @Valid를 통한 Request DTO 자동 검증
 * - Controller 중복 검증 제거
 */
@RestController
@RequestMapping("/datacenters")
@RequiredArgsConstructor
@Validated
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
     *
     * @param id 전산실 ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getDataCenterById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long id) {

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
     *
     * @param request 전산실 생성 요청 (Validation 적용)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createDataCenter(@Valid @RequestBody DataCenterCreateRequest request) {
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
     *
     * @param id 전산실 ID
     * @param request 전산실 수정 요청 (Validation 적용)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long id,
            @Valid @RequestBody DataCenterUpdateRequest request) {

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
     * 실제로는 소프트 삭제(delYn = Y)로 처리
     * 주의: 전산실에 속한 랙이나 장비가 있으면 삭제가 제한될 수 있음
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 전산실 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long id) {

        dataCenterService.deleteDataCenter(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실 삭제 완료",
                null
        );
        return ResponseEntity.ok(response);
    }
}