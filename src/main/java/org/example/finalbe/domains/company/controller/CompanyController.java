/**
 * 작성자: 황요한
 * 회사(Company) 관리 컨트롤러
 * 주요 기능:
 *  - 회사 목록 조회
 *  - 회사 상세 조회
 *  - 회사 생성/수정/삭제 (ADMIN 권한 필요)
 *  - 회사명 검색
 *  - 회사에 속한 서버실 목록 조회
 */
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

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Validated
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 회사 목록 조회
     * GET /api/companies
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAllCompanies() {
        List<CompanyListResponse> companies = companyService.getAllCompanies();
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 목록 조회 완료", companies)
        );
    }

    /**
     * 회사 상세 조회
     * GET /api/companies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getCompanyById(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
            Long id
    ) {
        CompanyDetailResponse company = companyService.getCompanyById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 조회 완료", company)
        );
    }

    /**
     * 회사 생성
     * POST /api/companies
     * ADMIN 전용
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
     * ADMIN 전용
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
                new CommonResDto(HttpStatus.OK, "회사 수정 완료", company)
        );
    }

    /**
     * 회사 삭제
     * DELETE /api/companies/{id}
     * ADMIN 전용
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
                new CommonResDto(HttpStatus.OK, "회사 삭제 완료", null)
        );
    }

    /**
     * 회사명 검색
     * GET /api/companies/search?name=xxx
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchCompanies(
            @RequestParam
            @NotBlank(message = "검색어를 입력해주세요.")
            String name
    ) {
        List<CompanyListResponse> companies = companyService.searchCompaniesByName(name);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 검색 완료", companies)
        );
    }

    /**
     * 특정 회사의 서버실 목록 조회
     * GET /api/companies/{id}/serverrooms
     */
    @GetMapping("/{id}/serverrooms")
    public ResponseEntity<CommonResDto> getCompanyServerRooms(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
            Long id
    ) {
        List<CompanyServerRoomListResponse> serverRooms = companyService.getCompanyServerRooms(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회사 서버실 목록 조회 완료", serverRooms)
        );
    }
}
