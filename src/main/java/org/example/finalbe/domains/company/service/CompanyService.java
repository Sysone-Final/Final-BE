package org.example.finalbe.domains.company.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.dto.*;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyDataCenterRepository companyDataCenterRepository;

    /**
     * 회사 목록 조회 (삭제되지 않은 것만)
     */
    public List<CompanyListResponse> getAllCompanies() {
        log.info("Fetching all active companies");
        return companyRepository.findByDelYn(DelYN.N)
                .stream()
                .map(CompanyListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회사 상세 조회
     */
    public CompanyDetailResponse getCompanyById(Long id) {
        log.info("Fetching company by id: {}", id);
        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));
        return CompanyDetailResponse.from(company);
    }

    /**
     * 회사 생성
     */
    @Transactional
    public CompanyDetailResponse createCompany(CompanyCreateRequest request) {
        log.info("Creating company with code: {}", request.code());

        // 코드 중복 체크
        if (companyRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            throw new IllegalArgumentException("이미 존재하는 회사 코드입니다.");
        }

        // 사업자등록번호 중복 체크
        if (request.businessNumber() != null &&
                companyRepository.existsByBusinessNumberAndDelYn(request.businessNumber(), DelYN.N)) {
            throw new IllegalArgumentException("이미 존재하는 사업자등록번호입니다.");
        }

        Company company = request.toEntity();
        Company savedCompany = companyRepository.save(company);

        log.info("Company created successfully with id: {}", savedCompany.getId());
        return CompanyDetailResponse.from(savedCompany);
    }

    /**
     * 회사 정보 수정
     */
    @Transactional
    public CompanyDetailResponse updateCompany(Long id, CompanyUpdateRequest request) {
        log.info("Updating company with id: {}", id);

        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        // 사업자등록번호 변경 시 중복 체크
        if (request.businessNumber() != null && !request.businessNumber().equals(company.getBusinessNumber())) {
            if (companyRepository.existsByBusinessNumberAndDelYn(request.businessNumber(), DelYN.N)) {
                throw new IllegalArgumentException("이미 존재하는 사업자등록번호입니다.");
            }
        }

        company.updateInfo(
                request.name(),
                request.businessNumber(),
                request.ceoName(),
                request.phone(),
                request.fax(),
                request.email(),
                request.address(),
                request.website(),
                request.industry(),
                request.description(),
                request.employeeCount(),
                request.establishedDate(),
                request.logoUrl()
        );

        log.info("Company updated successfully with id: {}", id);
        return CompanyDetailResponse.from(company);
    }

    /**
     * 회사 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteCompany(Long id) {
        log.info("Deleting company with id: {}", id);

        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        company.softDelete();
        log.info("Company soft deleted successfully with id: {}", id);
    }

    /**
     * 회사 이름으로 검색
     */
    public List<CompanyListResponse> searchCompaniesByName(String name) {
        log.info("Searching companies by name: {}", name);
        return companyRepository.searchByName(name)
                .stream()
                .map(CompanyListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회사가 접근 가능한 전산실 목록 조회
     */
    public List<CompanyDataCenterListResponse> getCompanyDataCenters(Long companyId) {
        log.info("Fetching data centers for company: {}", companyId);

        // 회사 존재 확인
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        return companyDataCenterRepository.findByCompanyId(companyId)
                .stream()
                .map(cdc -> CompanyDataCenterListResponse.from(cdc.getDataCenter(), cdc.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
