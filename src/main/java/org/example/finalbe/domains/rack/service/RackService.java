package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.department.repository.RackDepartmentRepository;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 랙 관리 서비스
 * 랙의 CRUD 및 검색, 권한 관리 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackService {

    private final RackRepository rackRepository;
    private final DataCenterRepository dataCenterRepository;
    private final EquipmentRepository equipmentRepository;
    private final MemberRepository memberRepository;
    private final RackDepartmentRepository rackDepartmentRepository;

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
            throw new IllegalStateException("사용자 ID를 찾을 수 없습니다.");
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
     * 랙 접근 권한 확인
     */
    private void validateRackAccess(Member member, Long rackId) {
        if (member.getRole() == Role.ADMIN) {
            return; // ADMIN은 모든 랙에 접근 가능
        }

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        // 회사의 전산실 접근 권한 확인
        if (!dataCenterRepository.hasAccessToDataCenter(
                member.getCompany().getId(),
                rack.getDatacenter().getId())) {
            throw new AccessDeniedException("해당 랙에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 랙 목록 조회
     */
    public List<RackListResponse> getRacksByDataCenter(
            Long dataCenterId, String status, String sortBy) {

        List<Rack> racks = rackRepository.findByDatacenterIdAndDelYn(dataCenterId, DelYN.N);

        // 필터링
        if (status != null) {
            racks = racks.stream()
                    .filter(r -> r.getStatus().name().equals(status))
                    .collect(Collectors.toList());
        }

        // 정렬
        if ("usage".equals(sortBy)) {
            racks.sort(Comparator.comparing(Rack::getUsageRate).reversed());
        } else if ("power".equals(sortBy)) {
            racks.sort(Comparator.comparing(Rack::getPowerUsageRate).reversed());
        }

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 랙 상세 조회
     */
    public RackDetailResponse getRackById(Long id) {
        Member currentMember = getCurrentMember();
        log.debug("Fetching rack details for id: {}", id);

        // 접근 권한 확인
        validateRackAccess(currentMember, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        return RackDetailResponse.from(rack);
    }

    /**
     * 랙 생성
     */
    @Transactional
    public RackDetailResponse createRack(RackCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Creating new rack: {} by user: {}", request.rackName(), currentMember.getId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 전산실 조회 및 접근 권한 확인
        DataCenter dataCenter = dataCenterRepository.findActiveById(request.datacenterId())
                .orElseThrow(() -> new EntityNotFoundException("전산실", request.datacenterId()));

        if (currentMember.getRole() != Role.ADMIN) {
            if (!dataCenterRepository.hasAccessToDataCenter(
                    currentMember.getCompany().getId(), request.datacenterId())) {
                throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
            }
        }

        // 전산실 최대 랙 수 확인
        if (dataCenter.getCurrentRackCount() >= dataCenter.getMaxRackCount()) {
            throw new BusinessException("전산실의 최대 랙 수를 초과했습니다.");
        }

        // 랙 이름 중복 체크 (같은 전산실 내)
        if (rackRepository.existsByDatacenterIdAndRackNameAndDelYn(
                request.datacenterId(), request.rackName(), DelYN.N)) {
            throw new DuplicateException("랙 이름", request.rackName());
        }

        // 랙 생성
        Rack rack = request.toEntity(dataCenter, currentMember.getUserName());
        Rack savedRack = rackRepository.save(rack);

        // 전산실의 현재 랙 수 증가
        dataCenter.incrementRackCount();

        log.info("Rack created successfully with id: {}", savedRack.getId());
        return RackDetailResponse.from(savedRack);
    }

    /**
     * 랙 수정
     */
    @Transactional
    public RackDetailResponse updateRack(Long id, RackUpdateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Updating rack with id: {} by user: {}", id, currentMember.getId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 접근 권한 확인
        validateRackAccess(currentMember, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        // 랙 이름 중복 체크 (변경하는 경우)
        if (request.rackName() != null && !request.rackName().equals(rack.getRackName())) {
            if (rackRepository.existsByDatacenterIdAndRackNameAndDelYn(
                    rack.getDatacenter().getId(), request.rackName(), DelYN.N)) {
                throw new DuplicateException("랙 이름", request.rackName());
            }
        }

        // 랙 정보 업데이트
        rack.updateInfo(request, currentMember.getUserName());

        log.info("Rack updated successfully for id: {}", id);
        return RackDetailResponse.from(rack);
    }

    /**
     * 랙 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteRack(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Deleting rack with id: {} by user: {}", id, currentMember.getId());

        // ADMIN만 가능
        if (currentMember.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");
        }

        // 접근 권한 확인
        validateRackAccess(currentMember, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        // 랙에 장비가 있는지 확인
        boolean hasEquipment = equipmentRepository.existsByRackIdAndDelYn(id, DelYN.N);
        if (hasEquipment) {
            throw new BusinessException("랙에 장비가 존재하여 삭제할 수 없습니다. 먼저 장비를 제거해주세요.");
        }

        // 소프트 삭제
        rack.softDelete();

        // 전산실의 현재 랙 수 감소
        rack.getDatacenter().decrementRackCount();

        log.info("Rack deleted successfully for id: {}", id);
    }

    /**
     * 랙 상태 변경
     */
    @Transactional
    public RackDetailResponse changeRackStatus(Long id, RackStatusChangeRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Changing rack status for id: {} to {}", id, request.status());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 접근 권한 확인
        validateRackAccess(currentMember, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        rack.changeStatus(request.status(), request.reason(), currentMember.getUserName());

        log.info("Rack status changed successfully for id: {}", id);
        return RackDetailResponse.from(rack);
    }

    /**
     * 랙 검색
     * 키워드로 랙 이름, 그룹 번호, 위치 검색
     */
    public List<RackListResponse> searchRacks(String keyword, Long dataCenterId) {
        Member currentMember = getCurrentMember();
        log.debug("Searching racks with keyword: {}", keyword);

        List<Rack> racks;

        if (dataCenterId != null) {
            // 특정 전산실 내 검색
            racks = rackRepository.searchByKeywordInDataCenter(keyword, dataCenterId);
        } else if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 전체 검색
            racks = rackRepository.searchByKeyword(keyword);
        } else {
            // OPERATOR, VIEWER는 접근 가능한 전산실 내 검색
            racks = rackRepository.searchByKeywordForCompany(keyword, currentMember.getCompany().getId());
        }

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 담당자별 랙 목록 조회
     */
    public List<RackListResponse> getRacksByManager(Long managerId) {
        log.debug("Fetching racks by manager: {}", managerId);

        List<Rack> racks = rackRepository.findByManagerIdAndDelYn(managerId, DelYN.N);

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 부서별 랙 목록 조회
     */
    public List<RackListResponse> getRacksByDepartment(Long departmentId) {
        log.debug("Fetching racks by department ID: {}", departmentId);

        List<Rack> racks = rackDepartmentRepository.findRacksByDepartmentId(departmentId);

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }
}