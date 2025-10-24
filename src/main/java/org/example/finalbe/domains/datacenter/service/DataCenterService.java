package org.example.finalbe.domains.datacenter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;

import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataCenterService {

    private final DataCenterRepository dataCenterRepository;
    private final MemberRepository memberRepository;

    /**
     * 현재 인증된 사용자 조회
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        String userId = authentication.getName();

        if (userId == null || userId.equals("anonymousUser")) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        try {
            return memberRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 확인 (ADMIN, OPERATOR만 가능)
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("관리자 또는 운영자만 수정할 수 있습니다.");
        }
    }

    /**
     * 전산실 접근 권한 확인
     * ADMIN은 모든 전산실 접근 가능, OPERATOR/VIEWER는 자기 회사 전산실만 접근 가능
     */
    private void validateDataCenterAccess(Member member, Long dataCenterId) {
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // ADMIN은 모든 전산실 접근 가능
        if (member.getRole() == Role.ADMIN) {
            return;
        }

        // OPERATOR, VIEWER는 자기 회사 전산실만 접근 가능
        if (!dataCenterRepository.hasAccessToDataCenter(member.getCompany().getId(), dataCenterId)) {
            throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 사용자가 접근 가능한 전산실 목록 조회
     * ADMIN: 모든 전산실, OPERATOR/VIEWER: 자기 회사 전산실만
     */
    public List<DataCenterListResponse> getAccessibleDataCenters() {
        Member currentMember = getCurrentMember();
        log.info("Fetching accessible data centers for user: {} (role: {}, company: {})",
                currentMember.getId(), currentMember.getRole(), currentMember.getCompany().getId());

        List<DataCenter> dataCenters;

        // ADMIN은 모든 전산실 조회 가능
        if (currentMember.getRole() == Role.ADMIN) {
            dataCenters = dataCenterRepository.findByDelYn(DelYN.N);
            log.info("Admin user - returning all {} data centers", dataCenters.size());
        } else {
            // OPERATOR, VIEWER는 자기 회사 전산실만
            dataCenters = dataCenterRepository.findAccessibleDataCentersByCompanyId(
                    currentMember.getCompany().getId());
            log.info("Non-admin user - returning {} accessible data centers", dataCenters.size());
        }

        return dataCenters.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전산실 상세 조회
     */
    public DataCenterDetailResponse getDataCenterById(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Fetching data center by id: {} for user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // 접근 권한 확인 (ADMIN은 자동 통과)
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        return DataCenterDetailResponse.from(dataCenter);
    }

    /**
     * 전산실 생성 (ADMIN, OPERATOR만 가능)
     */
    @Transactional
    public DataCenterDetailResponse createDataCenter(DataCenterCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Creating data center with code: {} by user: {} (role: {})",
                request.code(), currentMember.getId(), currentMember.getRole());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 입력값 검증
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("전산실 이름을 입력해주세요.");
        }
        if (request.code() == null || request.code().trim().isEmpty()) {
            throw new IllegalArgumentException("전산실 코드를 입력해주세요.");
        }
        if (request.managerId() == null) {
            throw new IllegalArgumentException("담당자를 지정해주세요.");
        }

        // 코드 중복 체크
        if (dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            throw new DuplicateException("전산실 코드", request.code());
        }

        // 담당자 조회
        Member manager = memberRepository.findById(request.managerId())
                .orElseThrow(() -> new EntityNotFoundException("담당자", request.managerId()));

        DataCenter dataCenter = request.toEntity(manager, currentMember.getUserName());
        DataCenter savedDataCenter = dataCenterRepository.save(dataCenter);

        log.info("Data center created successfully with id: {}", savedDataCenter.getId());
        return DataCenterDetailResponse.from(savedDataCenter);
    }

    /**
     * 전산실 정보 수정 (ADMIN, OPERATOR만 가능)
     */
    @Transactional
    public DataCenterDetailResponse updateDataCenter(Long id, DataCenterUpdateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Updating data center with id: {} by user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 접근 권한 확인 (ADMIN은 자동 통과)
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        // 코드 변경 시 중복 체크
        if (request.code() != null
                && !request.code().trim().isEmpty()
                && !request.code().equals(dataCenter.getCode())) {
            if (dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
                throw new DuplicateException("전산실 코드", request.code());
            }
        }

        // 담당자 변경
        Member manager = null;
        if (request.managerId() != null) {
            manager = memberRepository.findById(request.managerId())
                    .orElseThrow(() -> new EntityNotFoundException("담당자", request.managerId()));
        }

        dataCenter.updateInfo(
                request.name(),
                request.code(),
                request.location(),
                request.floor(),
                request.rows(),
                request.columns(),
                request.backgroundImageUrl(),
                request.status(),
                request.description(),
                request.totalArea(),
                request.totalPowerCapacity(),
                request.totalCoolingCapacity(),
                request.maxRackCount(),
                request.temperatureMin(),
                request.temperatureMax(),
                request.humidityMin(),
                request.humidityMax(),
                manager,
                currentMember.getUserName()
        );

        log.info("Data center updated successfully with id: {}", id);
        return DataCenterDetailResponse.from(dataCenter);
    }

    /**
     * 전산실 삭제 (Soft Delete) (ADMIN, OPERATOR만 가능)
     */
    @Transactional
    public void deleteDataCenter(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Deleting data center with id: {} by user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 접근 권한 확인 (ADMIN은 자동 통과)
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        dataCenter.softDelete();
        log.info("Data center soft deleted successfully with id: {}", id);
    }

    /**
     * 전산실 이름으로 검색
     * ADMIN: 모든 전산실 검색, OPERATOR/VIEWER: 자기 회사 전산실만 검색
     */
    public List<DataCenterListResponse> searchDataCentersByName(String name) {
        Member currentMember = getCurrentMember();
        log.info("Searching data centers by name: {} for user: {} (role: {})",
                name, currentMember.getId(), currentMember.getRole());

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<DataCenter> searchResults;

        // ADMIN은 모든 전산실에서 검색
        if (currentMember.getRole() == Role.ADMIN) {
            searchResults = dataCenterRepository.searchByName(name);
            log.info("Admin user - searched all data centers, found: {}", searchResults.size());
        } else {
            // OPERATOR, VIEWER는 자기 회사 전산실에서만 검색
            List<DataCenter> accessibleDataCenters = dataCenterRepository
                    .findAccessibleDataCentersByCompanyId(currentMember.getCompany().getId());

            searchResults = accessibleDataCenters.stream()
                    .filter(dc -> dc.getName().contains(name))
                    .collect(Collectors.toList());
            log.info("Non-admin user - searched accessible data centers, found: {}", searchResults.size());
        }

        return searchResults.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }
}