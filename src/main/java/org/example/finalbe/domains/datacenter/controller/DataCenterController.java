package org.example.finalbe.domains.datacenter.controller;

import jakarta.servlet.http.HttpServletRequest;
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
 * 전산실 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/datacenters")
@RequiredArgsConstructor
@Validated
public class DataCenterController {

    private final DataCenterService dataCenterService;

    /**
     * 접근 가능한 전산실 목록 조회
     * GET /api/datacenters
     *
     * @return 전산실 목록
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAccessibleDataCenters() {
        List<DataCenterListResponse> datacenters = dataCenterService.getAccessibleDataCenters();
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "전산실 목록 조회 완료", datacenters));
    }

    /**
     * 전산실 상세 조회
     * GET /api/datacenters/{id}
     *
     * @param id 전산실 ID
     * @return 전산실 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getDataCenterById(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.")
            Long id
    ) {
        DataCenterDetailResponse datacenter = dataCenterService.getDataCenterById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "전산실 조회 완료", datacenter));
    }

    /**
     * 전산실 생성
     * POST /api/datacenters
     *
     * @param request 전산실 생성 요청 DTO
     * @return 생성된 전산실 정보
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createDataCenter(
            @Valid @RequestBody DataCenterCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        DataCenterDetailResponse datacenter = dataCenterService.createDataCenter(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "전산실 생성 완료", datacenter));
    }

    /**
     * 전산실 정보 수정
     * PUT /api/datacenters/{id}
     *
     * @param id 전산실 ID
     * @param request 전산실 수정 요청 DTO
     * @return 수정된 전산실 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDataCenter(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.")
            Long id,
            @Valid @RequestBody DataCenterUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        DataCenterDetailResponse datacenter = dataCenterService.updateDataCenter(id, request, httpRequest);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "전산실 수정 완료", datacenter));
    }

    /**
     * 전산실 삭제
     * DELETE /api/datacenters/{id}
     *
     * @param id 전산실 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteDataCenter(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.")
            Long id,
            HttpServletRequest httpRequest,
            String reason
    ) {
        dataCenterService.deleteDataCenter(id, reason);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "전산실 삭제 완료", null));
    }

    /**
     * 전산실 이름으로 검색
     * GET /api/datacenters/search?name={name}
     *
     * @param name 검색 키워드
     * @return 검색된 전산실 목록
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchDataCentersByName(
            @RequestParam("name") String name
    ) {
        List<DataCenterListResponse> searchResults = dataCenterService.searchDataCentersByName(name);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "전산실 검색 완료", searchResults));
    }
}