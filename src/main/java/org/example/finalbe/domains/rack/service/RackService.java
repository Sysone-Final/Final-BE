package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
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
 * 랙 기본 CRUD 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackService {

    private final RackRepository rackRepository;
    private final DataCenterRepository dataCenterRepository;
    private final MemberRepository memberRepository;
    private final EquipmentRepository equipmentRepository;

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
     * 랙 접근 권한 확인
     */
    private void validateRackAccess(Member member, Long rackId) {
        if (rackId == null || rackId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        // ADMIN은 모든 랙 접근 가능
        if (member.getRole() == Role.ADMIN) {
            return;
        }

        // OPERATOR, VIEWER는 자기 회사가 접근 가능한 전산실의 랙만 접근 가능
        if (!dataCenterRepository.hasAccessToDataCenter(
                member.getCompany().getId(), rack.getDatacenter().getId())) {
            throw new AccessDeniedException("해당 랙에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 전산실별 랙 목록 조회 (필터링, 정렬 지원)
     */
    public List<RackListResponse> getRacksByDataCenter(
            Long dataCenterId, String status, String department, String sortBy) {

        Member currentMember = getCurrentMember();
        log.info("Fetching racks for datacenter: {} by user: {} (role: {})",
                dataCenterId, currentMember.getId(), currentMember.getRole());

        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        // 전산실 존재 및 접근 권한 확인
        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("전산실", dataCenterId));

        // ADMIN이 아닌 경우 접근 권한 확인
        if (currentMember.getRole() != Role.ADMIN) {
            if (!dataCenterRepository.hasAccessToDataCenter(
                    currentMember.getCompany().getId(), dataCenterId)) {
                throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
            }
        }

        List<Rack> racks = rackRepository.findByDatacenterIdAndDelYn(dataCenterId, DelYN.N);

        // 필터링
        if (status != null && !status.isEmpty()) {
            RackStatus rackStatus = RackStatus.valueOf(status);
            racks = racks.stream()
                    .filter(rack -> rack.getStatus() == rackStatus)
                    .collect(Collectors.toList());
        }

        if (department != null && !department.isEmpty()) {
            racks = racks.stream()
                    .filter(rack -> department.equals(rack.getDepartment()))
                    .collect(Collectors.toList());
        }

        // 정렬
        switch (sortBy.toLowerCase()) {
            case "usage":
                racks.sort(Comparator.comparing(Rack::getUsageRate).reversed());
                break;
            case "power":
                racks.sort(Comparator.comparing(Rack::getPowerUsageRate).reversed());
                break;
            case "name":
            default:
                racks.sort(Comparator.comparing(Rack::getRackName));
                break;
        }

        log.info("Found {} racks for datacenter: {}", racks.size(), dataCenterId);

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 랙 상세 조회
     */
    public RackDetailResponse getRackById(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Fetching rack by id: {} by user: {}", id, currentMember.getId());

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

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
        log.info("Creating rack in datacenter: {} by user: {}",
                request.datacenterId(), currentMember.getId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 입력값 검증
        if (request.rackName() == null || request.rackName().trim().isEmpty()) {
            throw new IllegalArgumentException("랙 이름을 입력해주세요.");
        }
        if (request.datacenterId() == null) {
            throw new IllegalArgumentException("전산실을 선택해주세요.");
        }
        if (request.managerId() == null || request.managerId().trim().isEmpty()) {
            throw new IllegalArgumentException("담당자를 지정해주세요.");
        }

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

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 접근 권한 확인
        validateRackAccess(currentMember, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        // 랙 이름 변경 시 중복 체크
        if (request.rackName() != null
                && !request.rackName().trim().isEmpty()
                && !request.rackName().equals(rack.getRackName())) {
            if (rackRepository.existsByDatacenterIdAndRackNameAndDelYn(
                    rack.getDatacenter().getId(), request.rackName(), DelYN.N)) {
                throw new DuplicateException("랙 이름", request.rackName());
            }
        }

        rack.updateInfo(request, currentMember.getUserName());

        log.info("Rack updated successfully with id: {}", id);
        return RackDetailResponse.from(rack);
    }

    /**
     * 랙 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteRack(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Deleting rack with id: {} by user: {}", id, currentMember.getId());

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 접근 권한 확인
        validateRackAccess(currentMember, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        // 장착된 장비 존재 확인
        if (equipmentRepository.existsByRackIdAndDelYn(id, DelYN.N)) {
            throw new BusinessException("랙에 장비가 존재하여 삭제할 수 없습니다. 먼저 장비를 제거해주세요.");
        }

        rack.softDelete();

        // 전산실의 현재 랙 수 감소
        DataCenter dataCenter = rack.getDatacenter();
        dataCenter.decrementRackCount();

        log.info("Rack soft deleted successfully with id: {}", id);
    }

    /**
     * 랙 상태 변경
     */
    @Transactional
    public RackDetailResponse changeRackStatus(Long id, RackStatusChangeRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Changing rack status for id: {} to {} by user: {}",
                id, request.status(), currentMember.getId());

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

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
     */
    public List<RackListResponse> searchRacks(String keyword, Long dataCenterId) {
        Member currentMember = getCurrentMember();
        log.info("Searching racks with keyword: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

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
    public List<RackListResponse> getRacksByManager(String managerId) {
        log.info("Fetching racks by manager: {}", managerId);

        if (managerId == null || managerId.trim().isEmpty()) {
            throw new IllegalArgumentException("담당자 ID를 입력해주세요.");
        }

        List<Rack> racks = rackRepository.findByManagerIdAndDelYn(managerId, DelYN.N);

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 부서별 랙 목록 조회
     */
    public List<RackListResponse> getRacksByDepartment(String department) {
        log.info("Fetching racks by department: {}", department);

        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("부서명을 입력해주세요.");
        }

        List<Rack> racks = rackRepository.findByDepartmentAndDelYn(department, DelYN.N);

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }
}