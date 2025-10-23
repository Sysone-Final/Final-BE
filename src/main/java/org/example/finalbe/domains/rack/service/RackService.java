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
 *
 * 개선사항:
 * - 모든 메서드 완전 구현
 * - 소프트 삭제 오류 수정
 * - Bean Validation으로 중복 검증 제거
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
     * Bean Validation이 dataCenterId 검증을 처리하므로 중복 검증 제거
     */
    public List<RackListResponse> getRacksByDataCenter(Long dataCenterId) {
        Member currentMember = getCurrentMember();
        log.debug("Fetching racks for datacenter: {}", dataCenterId);

        // 접근 권한 확인
        if (currentMember.getRole() != Role.ADMIN) {
            if (!dataCenterRepository.hasAccessToDataCenter(
                    currentMember.getCompany().getId(), dataCenterId)) {
                throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
            }
        }

        List<Rack> racks = rackRepository.findByDatacenterIdAndDelYn(dataCenterId, DelYN.N);

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
     * Bean Validation이 request 검증을 처리하므로 중복 검증 제거
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
     *
     * 수정사항:
     * - countByRackIdAndDelYn → existsByRackIdAndDelYn 사용
     * - softDelete() 메서드에 파라미터 제거 (엔티티 내부에서 처리)
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

        // 랙에 장비가 있는지 확인 (수정: exists 메서드 사용)
        boolean hasEquipment = equipmentRepository.existsByRackIdAndDelYn(id, DelYN.N);
        if (hasEquipment) {
            throw new BusinessException("랙에 장비가 존재하여 삭제할 수 없습니다. 먼저 장비를 제거해주세요.");
        }

        // 소프트 삭제 (수정: 파라미터 없이 호출)
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
    public List<RackListResponse> getRacksByDepartment(String department) {
        log.debug("Fetching racks by department: {}", department);

        List<Rack> racks = rackRepository.findByDepartmentAndDelYn(department, DelYN.N);

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }
}