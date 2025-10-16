package org.example.finalbe.domains.companydatacenter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;
import org.example.finalbe.domains.companydatacenter.dto.CompanyDataCenterCreateRequest;
import org.example.finalbe.domains.companydatacenter.dto.CompanyDataCenterResponse;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyDataCenterService {

    private final CompanyDataCenterRepository companyDataCenterRepository;
    private final CompanyRepository companyRepository;
    private final DataCenterRepository dataCenterRepository;
    private final MemberRepository memberRepository;

    /**
     * 현재 인증된 사용자 조회
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        return memberRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 회사-전산실 매핑 생성 (여러 전산실 동시 등록) - 원자성 보장
     */
    @Transactional
    public List<CompanyDataCenterResponse> createCompanyDataCenterMappings(CompanyDataCenterCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Creating company-datacenter mappings for company: {} by user: {}",
                request.companyId(), currentMember.getId());

        // 1. 입력 검증
        if (request.dataCenterIds() == null || request.dataCenterIds().isEmpty()) {
            throw new IllegalArgumentException("전산실을 하나 이상 선택해주세요.");
        }

        // 2. 중복 제거
        List<Long> uniqueDataCenterIds = new ArrayList<>(new HashSet<>(request.dataCenterIds()));
        if (uniqueDataCenterIds.size() != request.dataCenterIds().size()) {
            log.warn("Duplicate datacenter IDs found in request, removed duplicates");
        }

        // 3. 회사 조회
        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        // 4. 모든 전산실 존재 여부 사전 검증 (원자성을 위해)
        List<DataCenter> datacenters = new ArrayList<>();
        for (Long dataCenterId : uniqueDataCenterIds) {
            DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "전산실을 찾을 수 없습니다. ID: " + dataCenterId));
            datacenters.add(dataCenter);
        }

        // 5. 중복 매핑 체크 (사전 검증)
        List<Long> duplicateIds = new ArrayList<>();
        for (Long dataCenterId : uniqueDataCenterIds) {
            if (companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                    request.companyId(), dataCenterId)) {
                duplicateIds.add(dataCenterId);
            }
        }

        // 중복이 있으면 전체 실패 (원자성 보장)
        if (!duplicateIds.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("이미 매핑된 전산실이 있습니다. 전산실 ID: %s",
                            duplicateIds.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(", ")))
            );
        }

        // 6. 모든 검증 통과 후 일괄 저장
        List<CompanyDataCenter> savedMappings = new ArrayList<>();
        try {
            for (DataCenter dataCenter : datacenters) {
                CompanyDataCenter companyDataCenter = CompanyDataCenter.builder()
                        .company(company)
                        .dataCenter(dataCenter)
                        .description(request.description())
                        .grantedBy(currentMember.getUsername())
                        .build();

                CompanyDataCenter saved = companyDataCenterRepository.save(companyDataCenter);
                savedMappings.add(saved);

                log.debug("Company-DataCenter mapping created: company={}, datacenter={}",
                        request.companyId(), dataCenter.getId());
            }

            log.info("Successfully created {} company-datacenter mappings", savedMappings.size());

            return savedMappings.stream()
                    .map(CompanyDataCenterResponse::from)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to create company-datacenter mappings, transaction will rollback", e);
            throw new IllegalStateException("매핑 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회사의 전산실 매핑 목록 조회
     */
    public List<CompanyDataCenterResponse> getCompanyDataCentersByCompanyId(Long companyId) {
        log.info("Fetching company-datacenter mappings for company: {}", companyId);

        // 회사 존재 확인
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        return companyDataCenterRepository.findByCompanyId(companyId)
                .stream()
                .map(CompanyDataCenterResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전산실의 회사 매핑 목록 조회
     */
    public List<CompanyDataCenterResponse> getCompanyDataCentersByDataCenterId(Long dataCenterId) {
        log.info("Fetching company-datacenter mappings for datacenter: {}", dataCenterId);

        // 전산실 존재 확인
        dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("전산실을 찾을 수 없습니다."));

        return companyDataCenterRepository.findByDataCenterId(dataCenterId)
                .stream()
                .map(CompanyDataCenterResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회사-전산실 매핑 삭제 (단건)
     */
    @Transactional
    public void deleteCompanyDataCenterMapping(Long companyId, Long dataCenterId) {
        log.info("Deleting company-datacenter mapping: company={}, datacenter={}", companyId, dataCenterId);

        CompanyDataCenter companyDataCenter = companyDataCenterRepository
                .findByCompanyIdAndDataCenterId(companyId, dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("매핑을 찾을 수 없습니다."));

        companyDataCenter.softDelete();
        log.info("Company-DataCenter mapping deleted successfully");
    }

    /**
     * 케이스 1: 특정 회사의 여러 전산실 매핑 일괄 삭제 (원자성 보장)
     * 예: 회사 1번이 전산실 [5, 6, 7]에 대한 접근 권한 삭제
     */
    @Transactional
    public int deleteCompanyDataCentersByCompany(Long companyId, List<Long> dataCenterIds) {
        log.info("Deleting multiple company-datacenter mappings: company={}, datacenterIds={}",
                companyId, dataCenterIds);

        if (dataCenterIds == null || dataCenterIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 전산실을 선택해주세요.");
        }

        // 회사 존재 확인
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        // 모든 매핑 존재 여부 사전 검증
        List<CompanyDataCenter> mappingsToDelete = new ArrayList<>();
        for (Long dataCenterId : dataCenterIds) {
            CompanyDataCenter mapping = companyDataCenterRepository
                    .findByCompanyIdAndDataCenterId(companyId, dataCenterId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("매핑을 찾을 수 없습니다. Company: %d, DataCenter: %d",
                                    companyId, dataCenterId)));
            mappingsToDelete.add(mapping);
        }

        // 일괄 삭제
        mappingsToDelete.forEach(CompanyDataCenter::softDelete);
        log.info("Successfully deleted {} company-datacenter mappings for company {}",
                mappingsToDelete.size(), companyId);

        return mappingsToDelete.size();
    }

    /**
     * 케이스 2: 특정 전산실과 연결된 모든 회사 매핑 삭제 (원자성 보장)
     * 예: 전산실 1번 폐쇄 시, 모든 회사와의 매핑 삭제
     */
    @Transactional
    public int deleteAllCompaniesByDataCenter(Long dataCenterId) {
        log.info("Deleting all company mappings for datacenter: {}", dataCenterId);

        // 전산실 존재 확인
        dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("전산실을 찾을 수 없습니다."));

        // 해당 전산실과 연결된 모든 매핑 조회
        List<CompanyDataCenter> mappingsToDelete = companyDataCenterRepository.findByDataCenterId(dataCenterId);

        if (mappingsToDelete.isEmpty()) {
            log.info("No mappings found for datacenter: {}", dataCenterId);
            return 0;
        }

        // 일괄 삭제
        mappingsToDelete.forEach(CompanyDataCenter::softDelete);
        log.info("Successfully deleted {} company-datacenter mappings for datacenter {}",
                mappingsToDelete.size(), dataCenterId);

        return mappingsToDelete.size();
    }

    /**
     * 케이스 3: 특정 전산실의 특정 회사들 매핑 삭제
     * 예: 전산실 1번에서 회사 [3, 4, 5]의 접근 권한 삭제
     */
    @Transactional
    public int deleteCompaniesByDataCenter(Long dataCenterId, List<Long> companyIds) {
        log.info("Deleting specific company mappings for datacenter: {}, companyIds={}",
                dataCenterId, companyIds);

        if (companyIds == null || companyIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 회사를 선택해주세요.");
        }

        // 전산실 존재 확인
        dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("전산실을 찾을 수 없습니다."));

        // 모든 매핑 존재 여부 사전 검증
        List<CompanyDataCenter> mappingsToDelete = new ArrayList<>();
        for (Long companyId : companyIds) {
            CompanyDataCenter mapping = companyDataCenterRepository
                    .findByCompanyIdAndDataCenterId(companyId, dataCenterId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("매핑을 찾을 수 없습니다. Company: %d, DataCenter: %d",
                                    companyId, dataCenterId)));
            mappingsToDelete.add(mapping);
        }

        // 일괄 삭제
        mappingsToDelete.forEach(CompanyDataCenter::softDelete);
        log.info("Successfully deleted {} company-datacenter mappings for datacenter {}",
                mappingsToDelete.size(), dataCenterId);

        return mappingsToDelete.size();
    }
}