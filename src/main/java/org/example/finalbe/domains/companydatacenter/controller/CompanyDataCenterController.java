package org.example.finalbe.domains.companydatacenter.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.companydatacenter.dto.CompanyDataCenterCreateRequest;
import org.example.finalbe.domains.companydatacenter.dto.CompanyDataCenterResponse;
import org.example.finalbe.domains.companydatacenter.service.CompanyDataCenterService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 회사-전산실 매핑 관리 컨트롤러
 * 매핑 생성, 조회, 삭제 API 제공
 */
@RestController
@RequestMapping("/api/company-datacenters")
@RequiredArgsConstructor
@Validated
public class CompanyDataCenterController {

    private final CompanyDataCenterService companyDataCenterService;

    /**
     * 회사-전산실 매핑 생성
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> createMappings(
            @Valid @RequestBody CompanyDataCenterCreateRequest request) {

        List<CompanyDataCenterResponse> mappings =
                companyDataCenterService.createCompanyDataCenterMappings(request);

        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED,
                "회사-전산실 매핑 생성 완료",
                mappings
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 회사의 전산실 매핑 목록 조회
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<CommonResDto> getMappingsByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId) {

        List<CompanyDataCenterResponse> mappings =
                companyDataCenterService.getCompanyDataCentersByCompanyId(companyId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사의 전산실 매핑 조회 완료",
                mappings
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 전산실의 회사 매핑 목록 조회
     */
    @GetMapping("/datacenter/{dataCenterId}")
    public ResponseEntity<CommonResDto> getMappingsByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {

        List<CompanyDataCenterResponse> mappings =
                companyDataCenterService.getCompanyDataCentersByDataCenterId(dataCenterId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실의 회사 매핑 조회 완료",
                mappings
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 회사-전산실 매핑 삭제
     */
    @DeleteMapping("/{companyId}/{dataCenterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteMapping(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId,
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {

        companyDataCenterService.deleteCompanyDataCenterMapping(companyId, dataCenterId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사-전산실 매핑 삭제 완료",
                null
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 회사의 여러 전산실 매핑 일괄 삭제
     */
    @DeleteMapping("/company/{companyId}/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteCompanyDataCentersByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId,
            @RequestBody Map<String, List<Long>> request) {

        List<Long> dataCenterIds = request.get("dataCenterIds");

        int deletedCount = companyDataCenterService.deleteCompanyDataCentersByCompany(
                companyId, dataCenterIds);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("회사의 전산실 매핑 %d건 삭제 완료", deletedCount),
                Map.of("deletedCount", deletedCount)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 전산실의 모든 회사 매핑 삭제
     */
    @DeleteMapping("/datacenter/{dataCenterId}/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteAllCompaniesByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {

        int deletedCount = companyDataCenterService.deleteAllCompaniesByDataCenter(dataCenterId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("전산실의 모든 회사 매핑 %d건 삭제 완료", deletedCount),
                Map.of("deletedCount", deletedCount)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 전산실의 특정 회사들 매핑 일괄 삭제
     */
    @DeleteMapping("/datacenter/{dataCenterId}/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteCompaniesByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId,
            @RequestBody Map<String, List<Long>> request) {

        List<Long> companyIds = request.get("companyIds");

        int deletedCount = companyDataCenterService.deleteCompaniesByDataCenter(
                dataCenterId, companyIds);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("전산실의 회사 매핑 %d건 삭제 완료", deletedCount),
                Map.of("deletedCount", deletedCount)
        );

        return ResponseEntity.ok(response);
    }
}