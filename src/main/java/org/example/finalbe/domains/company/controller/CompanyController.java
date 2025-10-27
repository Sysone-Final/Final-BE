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
 *
 * 개선사항:
 * - Bean Validation 적용
 * - @Valid를 통한 Request DTO 자동 검증
 * - 선언적 파라미터 검증
 */
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Validated
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
     *
     * @param id 회사 ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getCompanyById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long id) {

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
     *
     * @param request 회사 생성 요청 (Validation 적용)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> createCompany(@Valid @RequestBody CompanyCreateRequest request) {
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
     *
     * @param id 회사 ID
     * @param request 회사 수정 요청 (Validation 적용)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> updateCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long id,
            @Valid @RequestBody CompanyUpdateRequest request) {

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
     *
     * @param id 회사 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long id) {

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
     *
     * @param name 검색 키워드
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchCompanies(
            @RequestParam @NotBlank(message = "검색어를 입력해주세요.") String name) {

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
     *
     * @param id 회사 ID
     */
    @GetMapping("/{id}/datacenters")
    public ResponseEntity<CommonResDto> getCompanyDataCenters(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long id) {

        List<CompanyDataCenterListResponse> datacenters = companyService.getCompanyDataCenters(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사 전산실 목록 조회 완료",
                datacenters
        );
        return ResponseEntity.ok(response);
    }
}