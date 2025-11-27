/**
 * 작성자: 황요한
 * 데이터센터 관리 API 컨트롤러
 */
package org.example.finalbe.domains.datacenter.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.service.DataCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datacenters")
@RequiredArgsConstructor
@Validated
public class DataCenterController {

    private final DataCenterService dataCenterService;

    /**
     * 데이터센터 목록 조회
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAllDataCenters() {
        List<DataCenterListResponse> dataCenters = dataCenterService.getAllDataCenters();
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "데이터센터 목록 조회 완료", dataCenters));
    }

    /**
     * 데이터센터 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getDataCenterById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 데이터센터 ID입니다.") Long id) {
        DataCenterDetailResponse dataCenter = dataCenterService.getDataCenterById(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "데이터센터 조회 완료", dataCenter));
    }

    /**
     * 데이터센터 생성
     */
    @PostMapping
    public ResponseEntity<CommonResDto> createDataCenter(@Valid @RequestBody DataCenterCreateRequest request) {
        DataCenterDetailResponse dataCenter = dataCenterService.createDataCenter(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "데이터센터 생성 완료", dataCenter));
    }

    /**
     * 데이터센터 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommonResDto> updateDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 데이터센터 ID입니다.") Long id,
            @Valid @RequestBody DataCenterUpdateRequest request) {
        DataCenterDetailResponse dataCenter = dataCenterService.updateDataCenter(id, request);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "데이터센터 수정 완료", dataCenter));
    }

    /**
     * 데이터센터 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResDto> deleteDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 데이터센터 ID입니다.") Long id) {
        dataCenterService.deleteDataCenter(id);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "데이터센터 삭제 완료", null));
    }

    /**
     * 데이터센터 검색
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchDataCenters(@RequestParam String name) {
        List<DataCenterListResponse> dataCenters = dataCenterService.searchDataCentersByName(name);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "데이터센터 검색 완료", dataCenters));
    }
}