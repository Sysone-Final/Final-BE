/**
 * 작성자: 황요한
 * 회사 관리 서비스
 * - 회사 CRUD
 * - 서버실 접근 권한 조회
 */
package org.example.finalbe.domains.company.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.dto.*;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
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
    private final CompanyServerRoomRepository companyServerRoomRepository;

    /**
     * 모든 활성(삭제되지 않은) 회사 조회
     */
    public List<CompanyListResponse> getAllCompanies() {
        log.info("회사 목록 조회 시작");

        List<Company> companies = companyRepository.findByDelYn(DelYN.N);

        log.info("조회된 활성 회사 수: {}", companies.size());

        return companies.stream()
                .map(CompanyListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회사 상세 정보 조회
     */
    public CompanyDetailResponse getCompanyById(Long id) {
        log.info("회사 상세 조회: {}", id);

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
        log.info("회사 생성 요청: {}", request.code());

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
        if (request.businessNumber() != null && !request.businessNumber().isBlank()) {
            if (companyRepository.existsByBusinessNumberAndDelYn(request.businessNumber(), DelYN.N)) {
                throw new DuplicateException("사업자등록번호", request.businessNumber());
            }
        }

        Company saved = companyRepository.save(request.toEntity());

        log.info("회사 생성 완료: {}", saved.getId());

        return CompanyDetailResponse.from(saved);
    }

    /**
     * 회사 정보 수정
     */
    @Transactional
    public CompanyDetailResponse updateCompany(Long id, CompanyUpdateRequest request) {
        log.info("회사 정보 수정 요청: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("회사", id));

        // 사업자등록번호 변경 시 중복 체크
        if (request.businessNumber() != null
                && !request.businessNumber().isBlank()
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

        log.info("회사 정보 수정 완료: {}", id);

        return CompanyDetailResponse.from(company);
    }

    /**
     * 회사 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteCompany(Long id) {
        log.info("회사 삭제 요청: {}", id);

        if (id == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("회사", id));

        company.softDelete();

        log.info("회사 Soft Delete 완료: {}", id);
    }

    /**
     * 회사명 검색 (부분 일치)
     */
    public List<CompanyListResponse> searchCompaniesByName(String name) {
        log.info("회사 검색 요청: {}", name);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<Company> companies = companyRepository.searchByName(name);

        log.info("검색 결과 수: {}", companies.size());

        return companies.stream()
                .map(CompanyListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회사가 접근 가능한 서버실 목록 조회
     */
    public List<CompanyServerRoomListResponse> getCompanyServerRooms(Long companyId) {
        log.info("회사 서버실 매핑 조회: {}", companyId);

        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        // 회사 존재 여부 확인
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        // 매핑된 서버실 목록 조회
        List<CompanyServerRoomListResponse> serverRooms = companyServerRoomRepository.findByCompanyId(companyId)
                .stream()
                .map(csr -> CompanyServerRoomListResponse.from(
                        csr.getServerRoom(),
                        csr.getCreatedAt()
                ))
                .collect(Collectors.toList());

        log.info("조회된 서버실 수: {}", serverRooms.size());

        return serverRooms;
    }
}
