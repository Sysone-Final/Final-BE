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
     * 회사와 전산실 간의 매핑(연결 관계)을 생성하는 기능
     * 특정 회사가 특정 전산실에 접근할 수 있도록 권한을 부여
     * 여러 개의 매핑을 한 번에 생성 가능
     * 권한: ADMIN만 가능
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
     * 특정 회사가 접근 가능한 전산실 목록을 조회하는 기능
     * 해당 회사에 매핑된 모든 전산실 정보를 반환
     * 권한: 모든 인증된 사용자 접근 가능
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
     * 특정 전산실에 접근 가능한 회사 목록을 조회하는 기능
     * 해당 전산실을 사용하는 모든 회사 정보를 반환
     * 권한: 모든 인증된 사용자 접근 가능
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
     * 회사와 전산실 간의 매핑을 하나 삭제하는 기능
     * 특정 회사의 특정 전산실 접근 권한을 제거
     * 권한: ADMIN만 가능
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
     * 특정 회사의 여러 전산실 매핑을 한 번에 삭제하는 기능
     * 한 회사가 여러 전산실에 대한 접근 권한을 한꺼번에 잃게 됨
     * 예: 회사 1번의 전산실 5, 6, 7 접근 권한 일괄 삭제
     * 권한: ADMIN만 가능
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
     * 특정 전산실과 연결된 모든 회사 매핑을 삭제하는 기능
     * 전산실 폐쇄 시 모든 회사의 접근 권한을 한 번에 제거
     * 예: 전산실 1번 폐쇄로 모든 회사와의 연결 끊기
     * 권한: ADMIN만 가능
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
     * 특정 전산실에서 특정 회사들의 매핑을 한 번에 삭제하는 기능
     * 한 전산실에서 여러 회사의 접근 권한을 일괄 제거
     * 예: 전산실 1번에서 회사 3, 4, 5의 접근 권한 삭제
     * 권한: ADMIN만 가능
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