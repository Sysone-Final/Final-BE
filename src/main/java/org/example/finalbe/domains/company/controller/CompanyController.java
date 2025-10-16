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
     * 회사 목록 조회
     * GET /companies
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
     * 회사 상세 조회
     * GET /companies/{id}
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
     * 회사 생성 (관리자만 가능)
     * POST /companies
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
     * 회사 수정 (관리자만 가능)
     * PUT /companies/{id}
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
     * 회사 삭제 (관리자만 가능)
     * DELETE /companies/{id}
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
     * 회사 검색
     * GET /companies/search?name={name}
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
     * 회사가 접근 가능한 전산실 목록 조회
     * GET /companies/{id}/datacenters
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