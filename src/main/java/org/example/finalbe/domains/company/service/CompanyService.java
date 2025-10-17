package org.example.finalbe.domains.company.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
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

        List<Company> companies = companyRepository.findByDelYn(DelYN.N);
        log.info("Found {} active companies", companies.size());

        return companies.stream()
                .map(CompanyListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회사 상세 조회
     */
    public CompanyDetailResponse getCompanyById(Long id) {
        log.info("Fetching company by id: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("회사", id));

        return CompanyDetailResponse.from(company);
    }

    /**
     * 회사 생성
     */
    @Transactional
    public CompanyDetailResponse createCompany(CompanyCreateRequest request) {
        log.info("Creating company with code: {}", request.code());

        // 입력값 검증
        if (request.code() == null || request.code().trim().isEmpty()) {
            throw new IllegalArgumentException("회사 코드를 입력해주세요.");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("회사명을 입력해주세요.");
        }

        // 코드 중복 체크
        if (companyRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            throw new DuplicateException("회사 코드", request.code());
        }

        // 사업자등록번호 중복 체크
        if (request.businessNumber() != null && !request.businessNumber().trim().isEmpty()) {
            if (companyRepository.existsByBusinessNumberAndDelYn(request.businessNumber(), DelYN.N)) {
                throw new DuplicateException("사업자등록번호", request.businessNumber());
            }
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

        if (id == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("회사", id));

        // 사업자등록번호 변경 시 중복 체크
        if (request.businessNumber() != null
                && !request.businessNumber().trim().isEmpty()
                && !request.businessNumber().equals(company.getBusinessNumber())) {
            if (companyRepository.existsByBusinessNumberAndDelYn(request.businessNumber(), DelYN.N)) {
                throw new DuplicateException("사업자등록번호", request.businessNumber());
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

        if (id == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("회사", id));

        company.softDelete();
        log.info("Company soft deleted successfully with id: {}", id);
    }

    /**
     * 회사 이름으로 검색
     */
    public List<CompanyListResponse> searchCompaniesByName(String name) {
        log.info("Searching companies by name: {}", name);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<Company> companies = companyRepository.searchByName(name);
        log.info("Found {} companies with name containing: {}", companies.size(), name);

        return companies.stream()
                .map(CompanyListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회사가 접근 가능한 전산실 목록 조회
     */
    public List<CompanyDataCenterListResponse> getCompanyDataCenters(Long companyId) {
        log.info("Fetching data centers for company: {}", companyId);

        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        // 회사 존재 확인
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        List<CompanyDataCenterListResponse> dataCenters = companyDataCenterRepository.findByCompanyId(companyId)
                .stream()
                .map(cdc -> CompanyDataCenterListResponse.from(cdc.getDataCenter(), cdc.getCreatedAt()))
                .collect(Collectors.toList());

        log.info("Found {} data centers for company: {}", dataCenters.size(), companyId);
        return dataCenters;
    }
}