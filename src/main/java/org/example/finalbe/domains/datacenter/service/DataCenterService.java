// src/main/java/org/example/finalbe/domains/datacenter/service/DataCenterService.java

package org.example.finalbe.domains.datacenter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companyserverroom.domain.CompanyServerRoom;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.dto.ServerRoomSimpleResponse;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DataCenter 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataCenterService {

    private final DataCenterRepository dataCenterRepository;
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository companyServerRoomRepository;
    private final ServerRoomRepository serverRoomRepository;

    /**
     * 현재 로그인한 사용자 조회
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        String userId = authentication.getName();
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("사용자 ID가 존재하지 않습니다.");
        }

        try {
            return memberRepository.findActiveById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 검증
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() == Role.VIEWER) {
            throw new AccessDeniedException("조회 권한만 있습니다. 수정 권한이 필요합니다.");
        }
    }

    /**
     * 데이터센터 생성
     */
    @Transactional
    public DataCenterDetailResponse createDataCenter(DataCenterCreateRequest request) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        if (dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            throw new DuplicateException("데이터센터 코드", request.code());
        }

        DataCenter dataCenter = request.toEntity();
        DataCenter savedDataCenter = dataCenterRepository.save(dataCenter);

        List<ServerRoomSimpleResponse> serverRooms = serverRoomRepository.findByDataCenterIdAndDelYn(dataCenter.getId())
                .stream()
                .map(ServerRoomSimpleResponse::from)
                .toList();


        log.info("데이터센터 생성 완료: {}", savedDataCenter.getName());
        return DataCenterDetailResponse.from(savedDataCenter,serverRooms);
    }

    /**
     * 데이터센터 목록 조회 (권한 필터링 적용)
     */
    public List<DataCenterListResponse> getAllDataCenters() {
        Member currentMember = getCurrentMember();
        log.info("Fetching accessible data centers for user: {} (role: {}, company: {})",
                currentMember.getId(), currentMember.getRole(), currentMember.getCompany().getId());

        List<DataCenter> dataCenters;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 모든 데이터센터 조회
            dataCenters = dataCenterRepository.findByDelYn(DelYN.N);
            log.info("Admin user - returning all {} data centers", dataCenters.size());
        } else {
            // 일반 사용자: 자신의 회사가 관리하는 서버실이 속한 데이터센터만 조회
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            // 접근 가능한 서버실에서 데이터센터 ID 추출 (중복 제거)
            Set<Long> accessibleDataCenterIds = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .map(ServerRoom::getDataCenter)
                    .filter(Objects::nonNull)
                    .map(DataCenter::getId)
                    .collect(Collectors.toSet());

            if (accessibleDataCenterIds.isEmpty()) {
                log.info("Non-admin user - no accessible data centers found");
                return List.of();
            }

            dataCenters = dataCenterRepository.findAllById(accessibleDataCenterIds)
                    .stream()
                    .filter(dc -> dc.getDelYn() == DelYN.N)
                    .collect(Collectors.toList());

            log.info("Non-admin user - returning {} accessible data centers", dataCenters.size());
        }

        return dataCenters.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }



    /**
     * 데이터센터 상세 조회 (서버실 목록 포함 - 권한 필터링 적용)
     */
    public DataCenterDetailResponse getDataCenterById(Long dataCenterId) {
        Member currentMember = getCurrentMember(); // 인증 확인

        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("데이터센터", dataCenterId));

        List<ServerRoomSimpleResponse> serverRoomResponses;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 해당 데이터센터의 모든 서버실 조회
            List<ServerRoom> serverRooms = serverRoomRepository.findByDataCenterIdAndDelYn(dataCenterId);

            serverRoomResponses = serverRooms.stream()
                    .map(ServerRoomSimpleResponse::from)
                    .collect(Collectors.toList());

            log.info("Admin user - found {} server rooms for data center: {}", serverRoomResponses.size(), dataCenterId);
        } else {
            // 일반 사용자: 자신의 회사가 관리하는 서버실만 조회
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            // 해당 데이터센터에 속하고, 회사가 관리하는 서버실만 필터링
            serverRoomResponses = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .filter(sr -> sr.getDataCenter() != null
                            && sr.getDataCenter().getId().equals(dataCenterId)
                            && sr.getDelYn() == DelYN.N)
                    .map(ServerRoomSimpleResponse::from)
                    .collect(Collectors.toList());

            log.info("Non-admin user - found {} accessible server rooms for data center: {}",
                    serverRoomResponses.size(), dataCenterId);
        }

        return DataCenterDetailResponse.from(dataCenter, serverRoomResponses);
    }

    /**
     * 데이터센터 수정
     */
    @Transactional
    public DataCenterDetailResponse updateDataCenter(Long dataCenterId, DataCenterUpdateRequest request) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("데이터센터", dataCenterId));

        dataCenter.updateInfo(
                request.name(),
                request.address(),
                request.description()
        );

        List<ServerRoomSimpleResponse> serverRoomResponses;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 해당 데이터센터의 모든 서버실 조회
            List<ServerRoom> serverRooms = serverRoomRepository.findByDataCenterIdAndDelYn(dataCenterId);

            serverRoomResponses = serverRooms.stream()
                    .map(ServerRoomSimpleResponse::from)
                    .collect(Collectors.toList());
        } else {
            // 일반 사용자: 자신의 회사가 관리하는 서버실만 조회
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            serverRoomResponses = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .filter(sr -> sr.getDataCenter() != null
                            && sr.getDataCenter().getId().equals(dataCenterId)
                            && sr.getDelYn() == DelYN.N)
                    .map(ServerRoomSimpleResponse::from)
                    .collect(Collectors.toList());
        }

        log.info("데이터센터 수정 완료: {}", dataCenter.getName());
        return DataCenterDetailResponse.from(dataCenter, serverRoomResponses);
    }

    /**
     * 데이터센터 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteDataCenter(Long dataCenterId) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("데이터센터", dataCenterId));

        dataCenter.softDelete();
        log.info("데이터센터 삭제 완료: {}", dataCenter.getName());
    }

    /**
     * 데이터센터명으로 검색 (권한 필터링 적용)
     */
    public List<DataCenterListResponse> searchDataCentersByName(String name) {
        Member currentMember = getCurrentMember();

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<DataCenter> searchResults;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 전체 검색
            searchResults = dataCenterRepository.searchByName(name);
            log.info("Admin user - searched all data centers, found: {}", searchResults.size());
        } else {
            // 일반 사용자: 접근 가능한 데이터센터 중에서만 검색
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            Set<Long> accessibleDataCenterIds = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .map(ServerRoom::getDataCenter)
                    .filter(Objects::nonNull)
                    .map(DataCenter::getId)
                    .collect(Collectors.toSet());

            if (accessibleDataCenterIds.isEmpty()) {
                log.info("Non-admin user - no accessible data centers to search");
                return List.of();
            }

            searchResults = dataCenterRepository.findAllById(accessibleDataCenterIds)
                    .stream()
                    .filter(dc -> dc.getDelYn() == DelYN.N)
                    .filter(dc -> dc.getName().contains(name))
                    .collect(Collectors.toList());

            log.info("Non-admin user - searched accessible data centers, found: {}", searchResults.size());
        }

        return searchResults.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }


}