package org.example.finalbe.domains.company.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.company.dto.*;
import org.example.finalbe.domains.company.service.CompanyService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회사 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Validated
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 회사 목록 조회
     * GET /api/companies
     *
     * @return 회사 목록
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAllCompanies() {
        List<CompanyListResponse> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 목록 조회 완료", companies));
    }

    /**
     * 회사 상세 조회
     * GET /api/companies/{id}
     *
     * @param id 회사 ID
     * @return 회사 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getCompanyById(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
            Long id
    ) {
        CompanyDetailResponse company = companyService.getCompanyById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 조회 완료", company));
    }

    /**
     * 회사 생성
     * POST /api/companies
     *
     * @param request 회사 생성 요청 DTO
     * @return 생성된 회사 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> createCompany(
            @Valid @RequestBody CompanyCreateRequest request
    ) {
        CompanyDetailResponse company = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "회사 생성 완료", company));
    }

    /**
     * 회사 정보 수정
     * PUT /api/companies/{id}
     *
     * @param id 회사 ID
     * @param request 회사 수정 요청 DTO
     * @return 수정된 회사 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> updateCompany(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
            Long id,
            @Valid @RequestBody CompanyUpdateRequest request
    ) {
        CompanyDetailResponse company = companyService.updateCompany(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 수정 완료", company));
    }

    /**
     * 회사 삭제
     * DELETE /api/companies/{id}
     *
     * @param id 회사 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteCompany(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
            Long id
    ) {
        companyService.deleteCompany(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 삭제 완료", null));
    }

    /**
     * 회사명으로 검색
     * GET /api/companies/search?name={name}
     *
     * @param name 검색 키워드
     * @return 검색된 회사 목록
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchCompanies(
            @RequestParam
            @NotBlank(message = "검색어를 입력해주세요.")
            String name
    ) {
        List<CompanyListResponse> companies = companyService.searchCompaniesByName(name);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 검색 완료", companies));
    }

    /**
     * 회사의 전산실 목록 조회
     * GET /api/companies/{id}/datacenters
     *
     * @param id 회사 ID
     * @return 전산실 목록
     */
    @GetMapping("/{id}/datacenters")
    public ResponseEntity<CommonResDto> getCompanyDataCenters(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
            Long id
    ) {
        List<CompanyDataCenterListResponse> datacenters = companyService.getCompanyDataCenters(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 전산실 목록 조회 완료", datacenters));
    }
}