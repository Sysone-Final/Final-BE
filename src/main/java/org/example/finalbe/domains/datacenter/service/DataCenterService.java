package org.example.finalbe.domains.datacenter.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
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
        String userId = authentication.getName();
        return memberRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 쓰기 권한 확인 (ADMIN, OPERATOR만 가능)
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new IllegalArgumentException("권한이 없습니다. 관리자 또는 운영자만 수정할 수 있습니다.");
        }
    }

    /**
     * 전산실 접근 권한 확인
     */
    private void validateDataCenterAccess(Member member, Long dataCenterId) {
        if (!dataCenterRepository.hasAccessToDataCenter(member.getCompany().getId(), dataCenterId)) {
            throw new IllegalArgumentException("해당 전산실에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 사용자가 접근 가능한 전산실 목록 조회
     */
    public List<DataCenterListResponse> getAccessibleDataCenters() {
        Member currentMember = getCurrentMember();
        log.info("Fetching accessible data centers for user: {} (company: {})",
                currentMember.getId(), currentMember.getCompany().getId());

        return dataCenterRepository.findAccessibleDataCentersByCompanyId(currentMember.getCompany().getId())
                .stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전산실 상세 조회
     */
    public DataCenterDetailResponse getDataCenterById(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Fetching data center by id: {} for user: {}", id, currentMember.getId());

        // 접근 권한 확인
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("전산실을 찾을 수 없습니다."));

        return DataCenterDetailResponse.from(dataCenter);
    }

    /**
     * 전산실 생성 (ADMIN, OPERATOR만 가능)
     */
    @Transactional
    public DataCenterDetailResponse createDataCenter(DataCenterCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Creating data center with code: {} by user: {}", request.code(), currentMember.getId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 코드 중복 체크
        if (request.code() != null && dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            throw new IllegalArgumentException("이미 존재하는 전산실 코드입니다.");
        }

        // 담당자 조회
        Member manager = memberRepository.findById(request.managerId())
                .orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다."));

        DataCenter dataCenter = request.toEntity(manager, currentMember.getUsername());
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
        log.info("Updating data center with id: {} by user: {}", id, currentMember.getId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 접근 권한 확인
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("전산실을 찾을 수 없습니다."));

        // 코드 변경 시 중복 체크
        if (request.code() != null && !request.code().equals(dataCenter.getCode())) {
            if (dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
                throw new IllegalArgumentException("이미 존재하는 전산실 코드입니다.");
            }
        }

        // 담당자 변경
        Member manager = null;
        if (request.managerId() != null) {
            manager = memberRepository.findById(request.managerId())
                    .orElseThrow(() -> new IllegalArgumentException("담당자를 찾을 수 없습니다."));
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
                currentMember.getUsername()
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
        log.info("Deleting data center with id: {} by user: {}", id, currentMember.getId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 접근 권한 확인
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("전산실을 찾을 수 없습니다."));

        dataCenter.softDelete();
        log.info("Data center soft deleted successfully with id: {}", id);
    }

    /**
     * 전산실 이름으로 검색 (접근 가능한 전산실만)
     */
    public List<DataCenterListResponse> searchDataCentersByName(String name) {
        Member currentMember = getCurrentMember();
        log.info("Searching data centers by name: {} for user: {}", name, currentMember.getId());

        List<DataCenter> accessibleDataCenters = dataCenterRepository
                .findAccessibleDataCentersByCompanyId(currentMember.getCompany().getId());

        return accessibleDataCenters.stream()
                .filter(dc -> dc.getName().contains(name))
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }
}