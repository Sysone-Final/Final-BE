package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.device.domain.Device;
import org.example.finalbe.domains.device.repository.DeviceRepository;
import org.example.finalbe.domains.history.service.RackHistoryRecorder;
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
    private final ServerRoomRepository serverRoomRepository;
    private final EquipmentRepository equipmentRepository;
    private final DeviceRepository deviceRepository;
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository csrRepository;
    private final RackHistoryRecorder rackHistoryRecorder;

    /**
     * 랙 목록 조회
     */
    public List<RackListResponse> getRacksByServerRoom(
            Long serverRoomId, String status, String sortBy) {

        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

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

        // 서버실 조회 및 접근 권한 확인
        ServerRoom serverRoom = serverRoomRepository.findActiveById(request.serverRoomId())
                .orElseThrow(() -> new EntityNotFoundException("서버실", request.serverRoomId()));

        if (currentMember.getRole() != Role.ADMIN) {
            if (!csrRepository.existsByCompanyIdAndServerRoomId(
                    currentMember.getCompany().getId(), request.serverRoomId())) {
                throw new AccessDeniedException("해당 서버실에 대한 접근 권한이 없습니다.");
            }
        }

        // 랙 이름 중복 체크 (같은 서버실 내)
        if (rackRepository.existsByRackNameAndServerRoomIdAndDelYn(
                request.rackName(), request.serverRoomId(), DelYN.N)) {
            throw new DuplicateException("랙 이름", request.rackName());
        }

        // 랙 생성
        Rack rack = request.toEntity(serverRoom);
        Rack savedRack = rackRepository.save(rack);

        // 히스토리 기록
        rackHistoryRecorder.recordCreate(savedRack, currentMember);

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
            if (rackRepository.existsByRackNameAndServerRoomIdAndDelYn(
                    request.rackName(), rack.getServerRoom().getId(), DelYN.N)) {
                throw new DuplicateException("랙 이름", request.rackName());
            }
        }

        // 수정 전 스냅샷 저장
        Rack oldRack = cloneRack(rack);

        // 랙 정보 업데이트
        rack.updateInfo(request);

        // 히스토리 기록
        rackHistoryRecorder.recordUpdate(oldRack, rack, currentMember);

        log.info("Rack updated successfully for id: {}", id);
        return RackDetailResponse.from(rack);
    }

    /**
     * 랙 삭제 (소프트 삭제)
     * 랙 삭제 시 포함된 장비(Equipment)와 장치(Device)도 함께 삭제
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

        // 1. 해당 랙의 모든 활성 장비(Equipment) 삭제
        List<Equipment> equipments = equipmentRepository.findActiveByRackId(id);
        if (!equipments.isEmpty()) {
            log.info("Deleting {} equipments in rack {}", equipments.size(), id);
            equipments.forEach(equipment -> {
                equipment.setDelYn(DelYN.Y);
                log.debug("Equipment {} marked as deleted", equipment.getId());
            });
        }

        // 2. 해당 랙의 모든 활성 장치(Device) 삭제
        List<Device> devices = deviceRepository.findActiveByRackId(id);
        if (!devices.isEmpty()) {
            log.info("Deleting {} devices in rack {}", devices.size(), id);
            devices.forEach(device -> {
                device.setDelYn(DelYN.Y);
                log.debug("Device {} marked as deleted", device.getId());
            });
        }

        // 3. 랙 소프트 삭제
        rack.setDelYn(DelYN.Y);

        // 히스토리 기록
        rackHistoryRecorder.recordDelete(rack, currentMember);

        log.info("Rack deleted successfully for id: {} (with {} equipments and {} devices)",
                id, equipments.size(), devices.size());
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

        // 이전 상태 저장
        String oldStatus = rack.getStatus() != null ? rack.getStatus().name() : "UNKNOWN";

        rack.setStatus(request.status());

        // 히스토리 기록
        rackHistoryRecorder.recordStatusChange(rack, oldStatus, request.status().name(), currentMember);

        log.info("Rack status changed successfully for id: {}", id);
        return RackDetailResponse.from(rack);
    }

    /**
     * 랙 검색
     */
    public List<RackListResponse> searchRacks(String keyword, Long serverRoomId) {
        Member currentMember = getCurrentMember();
        log.debug("Searching racks with keyword: {}", keyword);

        List<Rack> racks;

        if (serverRoomId != null) {
            // 특정 서버실 내 검색
            racks = rackRepository.searchByKeywordInServerRoom(keyword, serverRoomId);
        } else if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 전체 검색
            racks = rackRepository.searchByKeyword(keyword);
        } else {
            // OPERATOR, VIEWER는 접근 가능한 서버실 내 검색
            racks = rackRepository.searchByKeywordForCompany(keyword, currentMember.getCompany().getId());
        }

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }

    // === Private Helper Methods ===

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

    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("관리자 또는 운영자만 수정할 수 있습니다.");
        }
    }

    private void validateRackAccess(Member member, Long rackId) {
        if (member.getRole() == Role.ADMIN) {
            return; // ADMIN은 모든 랙에 접근 가능
        }

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        // 회사의 서버실 접근 권한 확인
        if (!csrRepository.existsByCompanyIdAndServerRoomId(
                member.getCompany().getId(),
                rack.getServerRoom().getId())) {
            throw new AccessDeniedException("해당 랙에 대한 접근 권한이 없습니다.");
        }
    }

    private Rack cloneRack(Rack rack) {
        return Rack.builder()
                .id(rack.getId())
                .rackName(rack.getRackName())
                .gridX(rack.getGridX())
                .gridY(rack.getGridY())
                .totalUnits(rack.getTotalUnits())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .status(rack.getStatus())
                .rackType(rack.getRackType())
                .doorDirection(rack.getDoorDirection())
                .zoneDirection(rack.getZoneDirection())
                .serverRoom(rack.getServerRoom())
                .build();
    }
}