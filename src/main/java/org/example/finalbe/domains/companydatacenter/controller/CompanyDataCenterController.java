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
import java.util.Map;

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
     * 회사-전산실 매핑 삭제 (단건, 관리자만 가능)
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

    /**
     * 케이스 1: 특정 회사의 여러 전산실 매핑 일괄 삭제 (관리자만 가능)
     * DELETE /company-datacenters/company/{companyId}/batch
     * Body: { "dataCenterIds": [5, 6, 7] }
     *
     * 예시: 회사 1번이 전산실 5, 6, 7에 대한 접근 권한 삭제
     */
    @DeleteMapping("/company/{companyId}/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteCompanyDataCentersByCompany(
            @PathVariable Long companyId,
            @RequestBody Map<String, List<Long>> request) {

        List<Long> dataCenterIds = request.get("dataCenterIds");
        int deletedCount = companyDataCenterService.deleteCompanyDataCentersByCompany(companyId, dataCenterIds);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("회사의 전산실 매핑 %d건 삭제 완료", deletedCount),
                Map.of("deletedCount", deletedCount)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 케이스 2: 특정 전산실의 모든 회사 매핑 삭제 (관리자만 가능)
     * DELETE /company-datacenters/datacenter/{dataCenterId}/all
     *
     * 예시: 전산실 1번 폐쇄 시, 모든 회사와의 매핑 삭제
     */
    @DeleteMapping("/datacenter/{dataCenterId}/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteAllCompaniesByDataCenter(@PathVariable Long dataCenterId) {
        int deletedCount = companyDataCenterService.deleteAllCompaniesByDataCenter(dataCenterId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("전산실의 모든 회사 매핑 %d건 삭제 완료", deletedCount),
                Map.of("deletedCount", deletedCount)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 케이스 3: 특정 전산실의 특정 회사들 매핑 삭제 (관리자만 가능)
     * DELETE /company-datacenters/datacenter/{dataCenterId}/batch
     * Body: { "companyIds": [3, 4, 5] }
     *
     * 예시: 전산실 1번에서 회사 3, 4, 5의 접근 권한 삭제
     */
    @DeleteMapping("/datacenter/{dataCenterId}/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteCompaniesByDataCenter(
            @PathVariable Long dataCenterId,
            @RequestBody Map<String, List<Long>> request) {

        List<Long> companyIds = request.get("companyIds");
        int deletedCount = companyDataCenterService.deleteCompaniesByDataCenter(dataCenterId, companyIds);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("전산실의 회사 매핑 %d건 삭제 완료", deletedCount),
                Map.of("deletedCount", deletedCount)
        );
        return ResponseEntity.ok(response);
    }
}