/**
 * 작성자: 황요한
 * 랙 관리 서비스 (CRUD, 권한 검증, 검색)
 */
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

import java.util.*;
import java.util.stream.Collectors;

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

    /* ====================== 조회 ====================== */

    // 서버실별 랙 목록 조회
    public List<RackListResponse> getRacksByServerRoom(Long serverRoomId, String status, String sortBy) {

        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

        // 상태 필터
        if (status != null) {
            racks = racks.stream()
                    .filter(r -> r.getStatus().name().equals(status))
                    .collect(Collectors.toList());
        }

        // 정렬
        switch (sortBy) {
            case "usage" -> racks.sort(Comparator.comparing(Rack::getUsageRate).reversed());
            case "power" -> racks.sort(Comparator.comparing(Rack::getPowerUsageRate).reversed());
        }

        // rackIds → 장비개수 맵
        Map<Long, Long> equipmentCountMap = new HashMap<>();
        List<Long> rackIds = racks.stream().map(Rack::getId).toList();

        if (!rackIds.isEmpty()) {
            equipmentRepository.countEquipmentsByRackIds(rackIds, DelYN.N)
                    .forEach(c -> equipmentCountMap.put(c.getRackId(), c.getCount()));
        }

        return racks.stream()
                .map(r -> RackListResponse.from(r, equipmentCountMap.getOrDefault(r.getId(), 0L).intValue()))
                .toList();
    }

    // 랙 상세 조회
    public RackDetailResponse getRackById(Long id) {
        Member member = getCurrentMember();
        validateRackAccess(member, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        return RackDetailResponse.from(rack);
    }

    /* ====================== 생성 ====================== */

    @Transactional
    public RackDetailResponse createRack(RackCreateRequest request) {
        Member member = getCurrentMember();
        validateWritePermission(member);

        // 서버실 조회
        ServerRoom serverRoom = serverRoomRepository.findActiveById(request.serverRoomId())
                .orElseThrow(() -> new EntityNotFoundException("서버실", request.serverRoomId()));

        // 접근 권한
        if (member.getRole() != Role.ADMIN &&
                !csrRepository.existsByCompanyIdAndServerRoomId(member.getCompany().getId(), request.serverRoomId())) {
            throw new AccessDeniedException("해당 서버실에 대한 접근 권한이 없습니다.");
        }

        // 중복 랙명 검증
        if (rackRepository.existsByRackNameAndServerRoomIdAndDelYn(
                request.rackName(), request.serverRoomId(), DelYN.N)) {
            throw new DuplicateException("랙 이름", request.rackName());
        }

        Rack rack = rackRepository.save(request.toEntity(serverRoom));

        rackHistoryRecorder.recordCreate(rack, member);
        return RackDetailResponse.from(rack);
    }

    /* ====================== 수정 ====================== */

    @Transactional
    public RackDetailResponse updateRack(Long id, RackUpdateRequest request) {
        Member member = getCurrentMember();
        validateWritePermission(member);
        validateRackAccess(member, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        // 랙명 중복 체크
        if (request.rackName() != null && !request.rackName().equals(rack.getRackName())) {
            if (rackRepository.existsByRackNameAndServerRoomIdAndDelYn(
                    request.rackName(), rack.getServerRoom().getId(), DelYN.N)) {
                throw new DuplicateException("랙 이름", request.rackName());
            }
        }

        Rack oldRack = cloneRack(rack);
        rack.updateInfo(request);

        rackHistoryRecorder.recordUpdate(oldRack, rack, member);
        return RackDetailResponse.from(rack);
    }

    /* ====================== 삭제 ====================== */

    @Transactional
    public void deleteRack(Long id) {
        Member member = getCurrentMember();

        if (member.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");
        }

        validateRackAccess(member, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        // 장비 삭제
        equipmentRepository.findActiveByRackId(id)
                .forEach(e -> e.setDelYn(DelYN.Y));

        // 장치 삭제
        deviceRepository.findActiveByRackId(id)
                .forEach(d -> d.setDelYn(DelYN.Y));

        // 랙 소프트 삭제
        rack.setDelYn(DelYN.Y);

        rackHistoryRecorder.recordDelete(rack, member);
    }

    /* ====================== 상태 변경 ====================== */

    @Transactional
    public RackDetailResponse changeRackStatus(Long id, RackStatusChangeRequest request) {
        Member member = getCurrentMember();
        validateWritePermission(member);
        validateRackAccess(member, id);

        Rack rack = rackRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("랙", id));

        String oldStatus = rack.getStatus().name();
        rack.setStatus(request.status());

        rackHistoryRecorder.recordStatusChange(rack, oldStatus, request.status().name(), member);
        return RackDetailResponse.from(rack);
    }

    /* ====================== 검색 ====================== */

    public List<RackListResponse> searchRacks(String keyword, Long serverRoomId) {
        Member member = getCurrentMember();

        List<Rack> racks = switch (serverRoomId != null ? "room" :
                member.getRole() == Role.ADMIN ? "admin" : "company") {

            case "room" -> rackRepository.searchByKeywordInServerRoom(keyword, serverRoomId);
            case "admin" -> rackRepository.searchByKeyword(keyword);
            default -> rackRepository.searchByKeywordForCompany(keyword, member.getCompany().getId());
        };

        return racks.stream().map(RackListResponse::from).toList();
    }

    /* ====================== 내부 유틸 ====================== */

    private Member getCurrentMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        String userId = auth.getName();
        return memberRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
    }

    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("관리자 또는 운영자만 수정할 수 있습니다.");
        }
    }

    private void validateRackAccess(Member member, Long rackId) {
        if (member.getRole() == Role.ADMIN) return;

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        if (!csrRepository.existsByCompanyIdAndServerRoomId(
                member.getCompany().getId(), rack.getServerRoom().getId())) {
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
