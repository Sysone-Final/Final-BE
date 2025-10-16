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
     * 회사-전산실 매핑 생성 (여러 전산실 동시 등록)
     */
    @Transactional
    public List<CompanyDataCenterResponse> createCompanyDataCenterMappings(CompanyDataCenterCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Creating company-datacenter mappings for company: {} by user: {}",
                request.companyId(), currentMember.getId());

        // 회사 조회
        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new IllegalArgumentException("회사를 찾을 수 없습니다."));

        List<CompanyDataCenterResponse> responses = new ArrayList<>();

        for (Long dataCenterId : request.dataCenterIds()) {
            // 전산실 조회
            DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                    .orElseThrow(() -> new IllegalArgumentException("전산실을 찾을 수 없습니다. ID: " + dataCenterId));

            // 중복 체크
            if (companyDataCenterRepository.existsByCompanyIdAndDataCenterId(request.companyId(), dataCenterId)) {
                log.warn("Mapping already exists for company: {} and datacenter: {}", request.companyId(), dataCenterId);
                continue;
            }

            // 매핑 생성
            CompanyDataCenter companyDataCenter = CompanyDataCenter.builder()
                    .company(company)
                    .dataCenter(dataCenter)
                    .description(request.description())
                    .grantedBy(currentMember.getUsername())
                    .build();

            CompanyDataCenter saved = companyDataCenterRepository.save(companyDataCenter);
            responses.add(CompanyDataCenterResponse.from(saved));

            log.info("Company-DataCenter mapping created: company={}, datacenter={}",
                    request.companyId(), dataCenterId);
        }

        return responses;
    }

    /**
     * 회사의 전산실 매핑 목록 조회
     */
    public List<CompanyDataCenterResponse> getCompanyDataCentersByCompanyId(Long companyId) {
        log.info("Fetching company-datacenter mappings for company: {}", companyId);

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

        return companyDataCenterRepository.findByDataCenterId(dataCenterId)
                .stream()
                .map(CompanyDataCenterResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회사-전산실 매핑 삭제
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
}
