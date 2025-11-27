// 작성자: 황요한
// 서버실 생성/수정/삭제, 접근 권한 검증, 회사-서버실 매핑 관리,
// Rack·장비·디바이스 연쇄 삭제, 히스토리 기록, 서버실 검색/조회 기능 제공

package org.example.finalbe.domains.serverroom.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companyserverroom.domain.CompanyServerRoom;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.dto.*;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.history.service.ServerRoomHistoryRecorder;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.device.domain.Device;
import org.example.finalbe.domains.device.repository.DeviceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerRoomService {

    private final ServerRoomRepository serverRoomRepository;
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository companyServerRoomRepository;
    private final ServerRoomHistoryRecorder serverRoomHistoryRecorder;
    private final DataCenterRepository dataCenterRepository;
    private final RackRepository rackRepository;
    private final EquipmentRepository equipmentRepository;
    private final DeviceRepository deviceRepository;

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
     * CompanyServerRoom 매핑 테이블로 접근 권한 확인
     */
    private void validateServerRoomAccess(Member member, Long serverRoomId) {
        if (serverRoomId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        if (member.getRole() == Role.ADMIN) {
            return; // ADMIN은 모든 전산실 접근 가능
        }

        // CompanyServerRoom 매핑 테이블에서 접근 권한 확인
        boolean hasAccess = companyServerRoomRepository.existsByCompanyIdAndServerRoomId(
                member.getCompany().getId(),
                serverRoomId
        );

        if (!hasAccess) {
            throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * CompanyServerRoom 매핑 테이블로 접근 가능한 서버실 목록 조회
     */
    public List<ServerRoomListResponse> getAccessibleServerRooms() {
        Member currentMember = getCurrentMember();
        log.info("Fetching accessible data centers for user: {} (role: {}, company: {})",
                currentMember.getId(), currentMember.getRole(), currentMember.getCompany().getId());

        List<ServerRoom> serverRooms;

        if (currentMember.getRole() == Role.ADMIN) {
            serverRooms = serverRoomRepository.findByDelYn(DelYN.N);
        } else {
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            serverRooms = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .collect(Collectors.toList());
        }

        return serverRooms.stream()
                .map(ServerRoomListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 서버실 상세 조회
     */
    public ServerRoomDetailResponse getServerRoomById(Long id) {
        Member currentMember = getCurrentMember();
        validateServerRoomAccess(currentMember, id);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        return ServerRoomDetailResponse.from(serverRoom);
    }

    /**
     * 서버실 생성 + CompanyServerRoom 자동 매핑 + 히스토리 기록
     */
    @Transactional
    public ServerRoomDetailResponse createServerRoom(ServerRoomCreateRequest request,
                                                     HttpServletRequest httpRequest) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("전산실 이름을 입력해주세요.");
        }
        if (request.code() == null || request.code().trim().isEmpty()) {
            throw new IllegalArgumentException("전산실 코드를 입력해주세요.");
        }

        if (serverRoomRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            throw new DuplicateException("전산실 코드", request.code());
        }

        ServerRoom serverRoom = request.toEntity();

        if (request.dataCenterId() != null) {
            DataCenter dataCenter = dataCenterRepository.findActiveById(request.dataCenterId())
                    .orElseThrow(() -> new EntityNotFoundException("데이터센터", request.dataCenterId()));
            serverRoom.setDataCenter(dataCenter);
        }

        ServerRoom savedServerRoom = serverRoomRepository.save(serverRoom);

        CompanyServerRoom mapping = CompanyServerRoom.builder()
                .company(currentMember.getCompany())
                .serverRoom(savedServerRoom)
                .grantedBy(currentMember.getUserName())
                .build();
        companyServerRoomRepository.save(mapping);

        serverRoomHistoryRecorder.recordCreate(savedServerRoom, currentMember);

        return ServerRoomDetailResponse.from(savedServerRoom);
    }

    /**
     * 서버실 정보 수정 + 히스토리 기록
     */
    @Transactional
    public ServerRoomDetailResponse updateServerRoom(Long id,
                                                     ServerRoomUpdateRequest request) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);
        validateServerRoomAccess(currentMember, id);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        ServerRoom oldServerRoom = copyServerRoom(serverRoom);

        if (request.code() != null
                && !request.code().trim().isEmpty()
                && !request.code().equals(serverRoom.getCode())) {

            if (serverRoomRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
                throw new DuplicateException("전산실 코드", request.code());
            }
        }

        serverRoom.updateInfo(
                request.name(),
                request.code(),
                request.location(),
                request.floor(),
                request.rows(),
                request.columns(),
                request.status(),
                request.description(),
                request.totalArea(),
                request.totalPowerCapacity(),
                request.totalCoolingCapacity(),
                request.temperatureMin(),
                request.temperatureMax(),
                request.humidityMin(),
                request.humidityMax()
        );

        if (request.dataCenterId() != null) {
            DataCenter dataCenter = dataCenterRepository.findActiveById(request.dataCenterId())
                    .orElseThrow(() -> new EntityNotFoundException("데이터센터", request.dataCenterId()));
            serverRoom.setDataCenter(dataCenter);
        }

        serverRoomHistoryRecorder.recordUpdate(oldServerRoom, serverRoom, currentMember);

        return ServerRoomDetailResponse.from(serverRoom);
    }

    /**
     * 서버실 삭제 + 연관된 Rack/Equipment/Device Soft Delete + 히스토리 기록
     */
    @Transactional
    public void deleteServerRoom(Long id) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);
        validateServerRoomAccess(currentMember, id);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(id, DelYN.N);

        int totalEquipments = 0;
        int totalDevices = 0;

        for (Rack rack : racks) {
            List<Equipment> equipments = equipmentRepository.findActiveByRackId(rack.getId());
            equipments.forEach(e -> e.setDelYn(DelYN.Y));
            totalEquipments += equipments.size();

            List<Device> devices = deviceRepository.findActiveByRackId(rack.getId());
            devices.forEach(d -> d.setDelYn(DelYN.Y));
            totalDevices += devices.size();

            rack.setDelYn(DelYN.Y);
        }

        serverRoomHistoryRecorder.recordDelete(serverRoom, currentMember);
        serverRoom.softDelete();
    }

    /**
     * CompanyServerRoom 기반 서버실 이름 검색
     */
    public List<ServerRoomListResponse> searchServerRoomsByName(String name) {
        Member currentMember = getCurrentMember();

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<ServerRoom> searchResults;

        if (currentMember.getRole() == Role.ADMIN) {
            searchResults = serverRoomRepository.searchByName(name);
        } else {
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            searchResults = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .filter(sr -> sr.getName().contains(name))
                    .collect(Collectors.toList());
        }

        return searchResults.stream()
                .map(ServerRoomListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ServerRoom Deep Copy (히스토리 기록용)
     */
    private ServerRoom copyServerRoom(ServerRoom original) {
        return ServerRoom.builder()
                .id(original.getId())
                .name(original.getName())
                .code(original.getCode())
                .location(original.getLocation())
                .floor(original.getFloor())
                .rows(original.getRows())
                .columns(original.getColumns())
                .status(original.getStatus())
                .description(original.getDescription())
                .totalArea(original.getTotalArea())
                .totalPowerCapacity(original.getTotalPowerCapacity())
                .totalCoolingCapacity(original.getTotalCoolingCapacity())
                .currentRackCount(original.getCurrentRackCount())
                .temperatureMin(original.getTemperatureMin())
                .temperatureMax(original.getTemperatureMax())
                .humidityMin(original.getHumidityMin())
                .humidityMax(original.getHumidityMax())
                .dataCenter(original.getDataCenter())
                .build();
    }

    /**
     * 접근 가능한 서버실 목록을 데이터센터 기준으로 그룹화하여 조회
     */
    public List<ServerRoomGroupedByDataCenterResponse> getAccessibleServerRoomsGroupedByDataCenter() {
        Member currentMember = getCurrentMember();

        List<ServerRoom> serverRooms;

        if (currentMember.getRole() == Role.ADMIN) {
            serverRooms = serverRoomRepository.findByDelYn(DelYN.N);
        } else {
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            serverRooms = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .collect(Collectors.toList());
        }

        Map<Long, List<ServerRoom>> grouped = serverRooms.stream()
                .filter(sr -> sr.getDataCenter() != null)
                .collect(Collectors.groupingBy(sr -> sr.getDataCenter().getId()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    DataCenter dc = entry.getValue().get(0).getDataCenter();

                    List<ServerRoomGroupedByDataCenterResponse.ServerRoomInfo> rooms =
                            entry.getValue().stream()
                                    .map(sr -> ServerRoomGroupedByDataCenterResponse.ServerRoomInfo.builder()
                                            .id(sr.getId())
                                            .name(sr.getName())
                                            .code(sr.getCode())
                                            .location(sr.getLocation())
                                            .floor(sr.getFloor())
                                            .status(sr.getStatus())
                                            .description(sr.getDescription())
                                            .build())
                                    .collect(Collectors.toList());

                    return ServerRoomGroupedByDataCenterResponse.builder()
                            .dataCenterId(dc.getId())
                            .dataCenterName(dc.getName())
                            .dataCenterCode(dc.getCode())
                            .dataCenterAddress(dc.getAddress())
                            .serverRooms(rooms)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
