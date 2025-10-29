package org.example.finalbe.domains.companydatacenter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
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

/**
 * 회사-전산실 매핑 서비스
 * 매핑 생성/조회/삭제 비즈니스 로직 처리
 * 원자성 보장: 하나라도 실패하면 전체 롤백
 */
@Slf4j // 로깅
@Service // Service Bean 등록
@RequiredArgsConstructor // final 필드 생성자 주입
@Transactional(readOnly = true) // 기본 읽기 전용 트랜잭션
public class CompanyDataCenterService {

    // Repository 의존성 주입
    private final CompanyDataCenterRepository companyDataCenterRepository;
    private final CompanyRepository companyRepository;
    private final DataCenterRepository dataCenterRepository;
    private final MemberRepository memberRepository;

    /**
     * 현재 인증된 사용자 조회
     * SecurityContext에서 사용자 ID 추출 후 Member 엔티티 반환
     */
    private Member getCurrentMember() {
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 여부 확인
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        // 사용자 ID 추출 (JWT의 subject)
        String userId = authentication.getName();

        // 익명 사용자 체크
        if (userId == null || userId.equals("anonymousUser")) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        // 사용자 ID로 Member 조회
        try {
            return memberRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 회사-전산실 매핑 생성 (여러 전산실 동시 등록)
     * 원자성 보장: 하나라도 실패하면 전체 롤백
     */
    @Transactional // 쓰기 트랜잭션
    public List<CompanyDataCenterResponse> createCompanyDataCenterMappings(
            CompanyDataCenterCreateRequest request) {

        // 현재 사용자 조회 (권한 부여자 기록용)
        Member currentMember = getCurrentMember();
        log.info("Creating company-datacenter mappings for company: {} by user: {}",
                request.companyId(), currentMember.getId());

        // === 1. 입력값 검증 ===
        if (request.companyId() == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }
        if (request.dataCenterIds() == null || request.dataCenterIds().isEmpty()) {
            throw new IllegalArgumentException("전산실을 하나 이상 선택해주세요.");
        }

        // === 2. 중복 제거 (동일 전산실 ID가 여러 번 입력된 경우) ===
        List<Long> uniqueDataCenterIds = new ArrayList<>(new HashSet<>(request.dataCenterIds()));
        // HashSet으로 중복 제거 후 다시 List로 변환
        if (uniqueDataCenterIds.size() != request.dataCenterIds().size()) {
            log.warn("Duplicate datacenter IDs found in request, removed duplicates");
        }

        // === 3. 회사 존재 확인 ===
        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));
        // 회사가 없거나 삭제된 경우 예외 발생

        // === 4. 모든 전산실 존재 여부 사전 검증 ===
        List<DataCenter> datacenters = new ArrayList<>();
        for (Long dataCenterId : uniqueDataCenterIds) {
            // 전산실 ID null 체크
            if (dataCenterId == null) {
                throw new IllegalArgumentException("전산실 ID는 null일 수 없습니다.");
            }

            // 전산실 조회
            DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                    .orElseThrow(() -> new EntityNotFoundException("전산실", dataCenterId));
            datacenters.add(dataCenter);
        }
        // 모든 전산실이 존재하는지 먼저 확인 (원자성 보장)

        // === 5. 중복 매핑 체크 ===
        List<Long> duplicateIds = new ArrayList<>();
        for (Long dataCenterId : uniqueDataCenterIds) {
            // 이미 매핑이 존재하는지 확인
            if (companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                    request.companyId(), dataCenterId)) {
                duplicateIds.add(dataCenterId);
            }
        }

        // 중복이 있으면 전체 실패
        if (!duplicateIds.isEmpty()) {
            String duplicateIdsStr = duplicateIds.stream()
                    .map(String::valueOf) // Long → String 변환
                    .collect(Collectors.joining(", ")); // ", "로 구분하여 결합
            throw new DuplicateException(
                    String.format("이미 매핑된 전산실이 있습니다. 전산실 ID: %s", duplicateIdsStr));
        }

        // === 6. 모든 검증 통과 후 일괄 저장 ===
        List<CompanyDataCenter> savedMappings = new ArrayList<>();
        try {
            // 각 전산실에 대해 매핑 생성
            for (DataCenter dataCenter : datacenters) {
                // CompanyDataCenter 엔티티 생성
                CompanyDataCenter companyDataCenter = CompanyDataCenter.builder()
                        .company(company) // 회사 설정
                        .dataCenter(dataCenter) // 전산실 설정
                        .description(request.description()) // 설명 설정
                        .grantedBy(currentMember.getUserName()) // 권한 부여자 설정
                        .build();

                // DB에 저장
                CompanyDataCenter saved = companyDataCenterRepository.save(companyDataCenter);
                savedMappings.add(saved);

                log.debug("Company-DataCenter mapping created: company={}, datacenter={}",
                        request.companyId(), dataCenter.getId());
            }

            log.info("Successfully created {} company-datacenter mappings", savedMappings.size());

            // Entity → DTO 변환 후 반환
            return savedMappings.stream()
                    .map(CompanyDataCenterResponse::from) // 각 Entity를 DTO로 변환
                    .collect(Collectors.toList()); // List로 수집

        } catch (Exception e) {
            // 저장 중 예외 발생 시 롤백
            log.error("Failed to create company-datacenter mappings, transaction will rollback", e);
            throw new IllegalStateException("매핑 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회사의 전산실 매핑 목록 조회
     * JOIN FETCH로 N+1 문제 방지
     */
    public List<CompanyDataCenterResponse> getCompanyDataCentersByCompanyId(Long companyId) {
        log.info("Fetching company-datacenter mappings for company: {}", companyId);

        // 입력값 검증
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        // 회사 존재 확인
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        // Repository에서 JOIN FETCH로 조회
        List<CompanyDataCenterResponse> mappings = companyDataCenterRepository
                .findByCompanyId(companyId) // 매핑 조회
                .stream()
                .map(CompanyDataCenterResponse::from) // Entity → DTO 변환
                .collect(Collectors.toList()); // List로 수집

        log.info("Found {} mappings for company: {}", mappings.size(), companyId);
        return mappings;
    }

    /**
     * 전산실의 회사 매핑 목록 조회
     * JOIN FETCH로 N+1 문제 방지
     */
    public List<CompanyDataCenterResponse> getCompanyDataCentersByDataCenterId(Long dataCenterId) {
        log.info("Fetching company-datacenter mappings for datacenter: {}", dataCenterId);

        // 입력값 검증
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // 전산실 존재 확인
        dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("전산실", dataCenterId));

        // Repository에서 JOIN FETCH로 조회
        List<CompanyDataCenterResponse> mappings = companyDataCenterRepository
                .findByDataCenterId(dataCenterId) // 매핑 조회
                .stream()
                .map(CompanyDataCenterResponse::from) // Entity → DTO 변환
                .collect(Collectors.toList()); // List로 수집

        log.info("Found {} mappings for datacenter: {}", mappings.size(), dataCenterId);
        return mappings;
    }

    /**
     * 회사-전산실 매핑 삭제 (단건)
     * Soft Delete로 처리 (delYn = 'Y')
     */
    @Transactional // 쓰기 트랜잭션
    public void deleteCompanyDataCenterMapping(Long companyId, Long dataCenterId) {
        log.info("Deleting company-datacenter mapping: company={}, datacenter={}",
                companyId, dataCenterId);

        // 입력값 검증
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // 매핑 조회
        CompanyDataCenter companyDataCenter = companyDataCenterRepository
                .findByCompanyIdAndDataCenterId(companyId, dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("회사(ID: %d)와 전산실(ID: %d)의 매핑", companyId, dataCenterId)));

        // Soft Delete (delYn = 'Y')
        companyDataCenter.softDelete();
        // JPA Dirty Checking으로 UPDATE 쿼리 자동 실행

        log.info("Company-DataCenter mapping deleted successfully");
    }

    /**
     * 특정 회사의 여러 전산실 매핑 일괄 삭제
     * 원자성 보장: 하나라도 없으면 전체 실패
     */
    @Transactional // 쓰기 트랜잭션
    public int deleteCompanyDataCentersByCompany(Long companyId, List<Long> dataCenterIds) {
        log.info("Deleting multiple company-datacenter mappings: company={}, datacenterIds={}",
                companyId, dataCenterIds);

        // === 입력값 검증 ===
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }
        if (dataCenterIds == null || dataCenterIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 전산실을 하나 이상 선택해주세요.");
        }

        // 각 전산실 ID null 체크
        for (Long dataCenterId : dataCenterIds) {
            if (dataCenterId == null) {
                throw new IllegalArgumentException("전산실 ID는 null일 수 없습니다.");
            }
        }

        // 회사 존재 확인
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        // === 모든 매핑 존재 여부 사전 검증 ===
        List<CompanyDataCenter> mappingsToDelete = new ArrayList<>();
        for (Long dataCenterId : dataCenterIds) {
            // 매핑 조회
            CompanyDataCenter mapping = companyDataCenterRepository
                    .findByCompanyIdAndDataCenterId(companyId, dataCenterId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("회사(ID: %d)와 전산실(ID: %d)의 매핑", companyId, dataCenterId)));
            mappingsToDelete.add(mapping);
        }
        // 모든 매핑이 존재하는지 확인 후 처리 (원자성 보장)

        // === 일괄 삭제 ===
        mappingsToDelete.forEach(CompanyDataCenter::softDelete);
        // 각 매핑에 대해 softDelete() 호출
        // JPA Dirty Checking으로 UPDATE 쿼리 일괄 실행

        log.info("Successfully deleted {} company-datacenter mappings for company {}",
                mappingsToDelete.size(), companyId);

        return mappingsToDelete.size(); // 삭제된 개수 반환
    }

    /**
     * 특정 전산실의 모든 회사 매핑 삭제
     * 전산실 폐쇄 시 사용
     */
    @Transactional // 쓰기 트랜잭션
    public int deleteAllCompaniesByDataCenter(Long dataCenterId) {
        log.info("Deleting all company mappings for datacenter: {}", dataCenterId);

        // 입력값 검증
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // 전산실 존재 확인
        dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("전산실", dataCenterId));

        // 해당 전산실의 모든 매핑 조회
        List<CompanyDataCenter> mappingsToDelete = companyDataCenterRepository
                .findByDataCenterId(dataCenterId);

        // 매핑이 없으면 0 반환
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
     * 특정 전산실의 특정 회사들 매핑 일괄 삭제
     * 원자성 보장: 하나라도 없으면 전체 실패
     */
    @Transactional // 쓰기 트랜잭션
    public int deleteCompaniesByDataCenter(Long dataCenterId, List<Long> companyIds) {
        log.info("Deleting specific company mappings for datacenter: {}, companyIds={}",
                dataCenterId, companyIds);

        // === 입력값 검증 ===
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }
        if (companyIds == null || companyIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 회사를 하나 이상 선택해주세요.");
        }

        // 각 회사 ID null 체크
        for (Long companyId : companyIds) {
            if (companyId == null) {
                throw new IllegalArgumentException("회사 ID는 null일 수 없습니다.");
            }
        }

        // 전산실 존재 확인
        dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("전산실", dataCenterId));

        // === 모든 매핑 존재 여부 사전 검증 ===
        List<CompanyDataCenter> mappingsToDelete = new ArrayList<>();
        for (Long companyId : companyIds) {
            // 매핑 조회
            CompanyDataCenter mapping = companyDataCenterRepository
                    .findByCompanyIdAndDataCenterId(companyId, dataCenterId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("회사(ID: %d)와 전산실(ID: %d)의 매핑", companyId, dataCenterId)));
            mappingsToDelete.add(mapping);
        }

        // === 일괄 삭제 ===
        mappingsToDelete.forEach(CompanyDataCenter::softDelete);

        log.info("Successfully deleted {} company-datacenter mappings for datacenter {}",
                mappingsToDelete.size(), dataCenterId);

        return mappingsToDelete.size();
    }
}