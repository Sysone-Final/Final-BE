// src/main/java/org/example/finalbe/domains/datacenter/service/DataCenterService.java

package org.example.finalbe.domains.datacenter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        log.info("데이터센터 생성 완료: {}", savedDataCenter.getName());
        return DataCenterDetailResponse.from(savedDataCenter);
    }

    /**
     * 데이터센터 목록 조회
     */
    public List<DataCenterListResponse> getAllDataCenters() {
        getCurrentMember(); // 인증 확인

        List<DataCenter> dataCenters = dataCenterRepository.findByDelYn(DelYN.N);
        return dataCenters.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 데이터센터 상세 조회
     */
    public DataCenterDetailResponse getDataCenterById(Long dataCenterId) {
        getCurrentMember(); // 인증 확인

        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("데이터센터", dataCenterId));

        return DataCenterDetailResponse.from(dataCenter);
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

        log.info("데이터센터 수정 완료: {}", dataCenter.getName());
        return DataCenterDetailResponse.from(dataCenter);
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
     * 데이터센터명으로 검색
     */
    public List<DataCenterListResponse> searchDataCentersByName(String name) {
        getCurrentMember(); // 인증 확인

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<DataCenter> dataCenters = dataCenterRepository.searchByName(name);
        return dataCenters.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }
}