package org.example.finalbe.domains.company.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.company.dto.*;
import org.example.finalbe.domains.company.service.CompanyService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 시스템에 등록된 모든 회사 목록을 조회하는 기능
     * 회사 이름, ID 등의 기본 정보 목록 제공
     * 권한: 모든 인증된 사용자 접근 가능
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAllCompanies() {
        List<CompanyListResponse> companies = companyService.getAllCompanies();
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사 목록 조회 완료",
                companies
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 회사의 상세 정보를 조회하는 기능
     * 회사명, 주소, 연락처, 설립일 등의 자세한 정보 제공
     * 권한: 모든 인증된 사용자 접근 가능
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getCompanyById(@PathVariable Long id) {
        CompanyDetailResponse company = companyService.getCompanyById(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사 조회 완료",
                company
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 새로운 회사를 등록하는 기능
     * 회사명, 주소, 연락처 등의 정보를 입력받아 신규 회사 생성
     * 권한: ADMIN만 가능
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> createCompany(@RequestBody CompanyCreateRequest request) {
        CompanyDetailResponse company = companyService.createCompany(request);
        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED,
                "회사 생성 완료",
                company
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 기존 회사의 정보를 수정하는 기능
     * 회사명, 주소, 연락처 등을 변경
     * 권한: ADMIN만 가능
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> updateCompany(
            @PathVariable Long id,
            @RequestBody CompanyUpdateRequest request) {
        CompanyDetailResponse company = companyService.updateCompany(id, request);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사 수정 완료",
                company
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 회사를 삭제하는 기능
     * 회사 정보를 시스템에서 제거
     * 주의: 회사에 속한 직원이나 전산실 매핑이 있으면 삭제가 제한될 수 있음
     * 권한: ADMIN만 가능
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사 삭제 완료",
                null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 회사 이름으로 검색하는 기능
     * 입력한 키워드가 포함된 이름을 가진 회사 목록 반환
     * 권한: 모든 인증된 사용자 접근 가능
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchCompanies(@RequestParam String name) {
        List<CompanyListResponse> companies = companyService.searchCompaniesByName(name);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사 검색 완료",
                companies
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 회사가 접근할 수 있는 전산실 목록을 조회하는 기능
     * 해당 회사와 매핑된 모든 전산실 정보 제공
     * 회사가 사용 가능한 데이터센터를 확인할 때 사용
     * 권한: 모든 인증된 사용자 접근 가능
     */
    @GetMapping("/{id}/datacenters")
    public ResponseEntity<CommonResDto> getCompanyDataCenters(@PathVariable Long id) {
        List<CompanyDataCenterListResponse> datacenters = companyService.getCompanyDataCenters(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사 전산실 목록 조회 완료",
                datacenters
        );
        return ResponseEntity.ok(response);
    }
}