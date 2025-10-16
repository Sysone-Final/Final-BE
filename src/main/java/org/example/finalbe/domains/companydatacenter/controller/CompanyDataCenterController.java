package org.example.finalbe.domains.companydatacenter.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.companydatacenter.dto.CompanyDataCenterCreateRequest;
import org.example.finalbe.domains.companydatacenter.dto.CompanyDataCenterResponse;
import org.example.finalbe.domains.companydatacenter.service.CompanyDataCenterService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/company-datacenters")
@RequiredArgsConstructor
public class CompanyDataCenterController {

    private final CompanyDataCenterService companyDataCenterService;

    /**
     * 회사-전산실 매핑 생성 (관리자만 가능)
     * POST /company-datacenters
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> createMappings(@RequestBody CompanyDataCenterCreateRequest request) {
        List<CompanyDataCenterResponse> mappings = companyDataCenterService.createCompanyDataCenterMappings(request);
        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED,
                "회사-전산실 매핑 생성 완료",
                mappings
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 회사별 전산실 매핑 조회
     * GET /company-datacenters/company/{companyId}
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<CommonResDto> getMappingsByCompany(@PathVariable Long companyId) {
        List<CompanyDataCenterResponse> mappings = companyDataCenterService.getCompanyDataCentersByCompanyId(companyId);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사의 전산실 매핑 조회 완료",
                mappings
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 전산실별 회사 매핑 조회
     * GET /company-datacenters/datacenter/{dataCenterId}
     */
    @GetMapping("/datacenter/{dataCenterId}")
    public ResponseEntity<CommonResDto> getMappingsByDataCenter(@PathVariable Long dataCenterId) {
        List<CompanyDataCenterResponse> mappings = companyDataCenterService.getCompanyDataCentersByDataCenterId(dataCenterId);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실의 회사 매핑 조회 완료",
                mappings
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 회사-전산실 매핑 삭제 (관리자만 가능)
     * DELETE /company-datacenters/{companyId}/{dataCenterId}
     */
    @DeleteMapping("/{companyId}/{dataCenterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteMapping(
            @PathVariable Long companyId,
            @PathVariable Long dataCenterId) {
        companyDataCenterService.deleteCompanyDataCenterMapping(companyId, dataCenterId);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사-전산실 매핑 삭제 완료",
                null
        );
        return ResponseEntity.ok(response);
    }
}