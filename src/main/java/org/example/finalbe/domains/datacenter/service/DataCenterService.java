/**
 * 작성자: 황요한
 * 데이터센터 서비스 클래스
 */
package org.example.finalbe.domains.datacenter.service;

import jakarta.servlet.http.HttpServletRequest;
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
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.dto.ServerRoomSimpleResponse;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
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
    private final ServerRoomRepository serverRoomRepository;

    /**
     * 현재 로그인한 사용자 조회
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
            return memberRepository.findActiveById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 확인
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("관리자 또는 운영자만 수정할 수 있습니다.");
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
        dataCenter.setCompany(currentMember.getCompany());

        DataCenter savedDataCenter = dataCenterRepository.save(dataCenter);

        List<ServerRoomSimpleResponse> serverRooms = serverRoomRepository.findByDataCenterIdAndDelYn(savedDataCenter.getId(), DelYN.N)
                .stream()
                .map(ServerRoomSimpleResponse::from)
                .toList();

        log.info("데이터센터 생성 완료: {}", savedDataCenter.getName());
        return DataCenterDetailResponse.from(savedDataCenter, serverRooms);
    }

    /**
     * 데이터센터 목록 조회
     */
    public List<DataCenterListResponse> getAllDataCenters() {
        Member currentMember = getCurrentMember();
        log.info("Fetching data centers for user: {} (role: {}, company: {})",
                currentMember.getId(), currentMember.getRole(), currentMember.getCompany().getId());

        List<DataCenter> dataCenters;

        if (currentMember.getRole() == Role.ADMIN) {
            dataCenters = dataCenterRepository.findByDelYn(DelYN.N);
            log.info("Admin user - returning all {} data centers", dataCenters.size());
        } else {
            dataCenters = dataCenterRepository.findByCompanyIdAndDelYn(
                    currentMember.getCompany().getId(), DelYN.N);
            log.info("Non-admin user - returning {} data centers for company: {}",
                    dataCenters.size(), currentMember.getCompany().getId());
        }

        return dataCenters.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 데이터센터 상세 조회
     */
    public DataCenterDetailResponse getDataCenterById(Long dataCenterId) {
        Member currentMember = getCurrentMember();

        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("데이터센터", dataCenterId));

        if (currentMember.getRole() != Role.ADMIN) {
            if (dataCenter.getCompany() == null ||
                    !dataCenter.getCompany().getId().equals(currentMember.getCompany().getId())) {
                throw new AccessDeniedException("해당 데이터센터에 대한 접근 권한이 없습니다.");
            }
        }

        List<ServerRoom> serverRooms = serverRoomRepository.findByDataCenterIdAndDelYn(dataCenterId, DelYN.N);
        List<ServerRoomSimpleResponse> serverRoomResponses = serverRooms.stream()
                .map(ServerRoomSimpleResponse::from)
                .collect(Collectors.toList());

        log.info("데이터센터 조회 완료: {}", dataCenter.getName());
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

        if (currentMember.getRole() != Role.ADMIN) {
            if (dataCenter.getCompany() == null ||
                    !dataCenter.getCompany().getId().equals(currentMember.getCompany().getId())) {
                throw new AccessDeniedException("해당 데이터센터에 대한 수정 권한이 없습니다.");
            }
        }

        dataCenter.updateInfo(request.name(), request.address(), request.description());

        List<ServerRoom> serverRooms = serverRoomRepository.findByDataCenterIdAndDelYn(dataCenterId, DelYN.N);
        List<ServerRoomSimpleResponse> serverRoomResponses = serverRooms.stream()
                .map(ServerRoomSimpleResponse::from)
                .collect(Collectors.toList());

        log.info("데이터센터 수정 완료: {}", dataCenter.getName());
        return DataCenterDetailResponse.from(dataCenter, serverRoomResponses);
    }

    /**
     * 데이터센터 삭제
     */
    @Transactional
    public void deleteDataCenter(Long dataCenterId) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("데이터센터", dataCenterId));

        if (currentMember.getRole() != Role.ADMIN) {
            if (dataCenter.getCompany() == null ||
                    !dataCenter.getCompany().getId().equals(currentMember.getCompany().getId())) {
                throw new AccessDeniedException("해당 데이터센터에 대한 삭제 권한이 없습니다.");
            }
        }

        dataCenter.softDelete();
        log.info("데이터센터 삭제 완료: {}", dataCenter.getName());
    }

    /**
     * 데이터센터명으로 검색
     */
    public List<DataCenterListResponse> searchDataCentersByName(String name) {
        Member currentMember = getCurrentMember();

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<DataCenter> searchResults;

        if (currentMember.getRole() == Role.ADMIN) {
            searchResults = dataCenterRepository.searchByName(name);
            log.info("Admin user - searched all data centers, found: {}", searchResults.size());
        } else {
            searchResults = dataCenterRepository.searchByNameAndCompanyId(name, currentMember.getCompany().getId());
            log.info("Non-admin user - searched company data centers, found: {}", searchResults.size());
        }

        return searchResults.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }
}