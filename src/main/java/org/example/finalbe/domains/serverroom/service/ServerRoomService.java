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
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.dto.*;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.history.service.ServerRoomHistoryRecorder;
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
public class ServerRoomService {

    private final ServerRoomRepository serverRoomRepository;
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository companyServerRoomRepository;
    private final ServerRoomHistoryRecorder serverRoomHistoryRecorder;
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
     * CompanyDataCenter 매핑 테이블로 접근 권한 확인
     */
    private void validateDataCenterAccess(Member member, Long dataCenterId) {
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        if (member.getRole() == Role.ADMIN) {
            return; // ADMIN은 모든 전산실 접근 가능
        }

        // CompanyDataCenter 매핑 테이블에서 접근 권한 확인
        boolean hasAccess = companyServerRoomRepository.existsByCompanyIdAndDataCenterId(
                member.getCompany().getId(),
                dataCenterId
        );

        if (!hasAccess) {
            throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * CompanyDataCenter 매핑 테이블로 접근 가능한 전산실 목록 조회
     */
    public List<ServerRoomListResponse> getAccessibleDataCenters() {
        Member currentMember = getCurrentMember();
        log.info("Fetching accessible data centers for user: {} (role: {}, company: {})",
                currentMember.getId(), currentMember.getRole(), currentMember.getCompany().getId());

        List<ServerRoom> serverRooms;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 모든 전산실 조회
            serverRooms = serverRoomRepository.findByDelYn(DelYN.N);
            log.info("Admin user - returning all {} data centers", serverRooms.size());
        } else {
            // 일반 사용자: CompanyDataCenter 매핑을 통해 접근 가능한 전산실만 조회
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            serverRooms = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .collect(Collectors.toList());

            log.info("Non-admin user - returning {} accessible data centers", serverRooms.size());
        }

        return serverRooms.stream()
                .map(ServerRoomListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전산실 상세 조회
     */
    public ServerRoomDetailResponse getDataCenterById(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Fetching data center by id: {} for user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // CompanyDataCenter 매핑으로 접근 권한 확인
        validateDataCenterAccess(currentMember, id);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        return ServerRoomDetailResponse.from(serverRoom);
    }

    /**
     * 전산실 생성 + CompanyDataCenter 매핑 자동 생성 + 히스토리 기록
     */
    @Transactional
    public ServerRoomDetailResponse createDataCenter(ServerRoomCreateRequest request,
                                                     HttpServletRequest httpRequest) {
        Member currentMember = getCurrentMember();
        log.info("Creating data center with code: {} by user: {} (role: {}, company: {})",
                request.code(), currentMember.getId(), currentMember.getRole(),
                currentMember.getCompany().getId());

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


        // 전산실 생성
        ServerRoom serverRoom = request.toEntity();
        ServerRoom savedServerRoom = serverRoomRepository.save(serverRoom);

        // CompanyDataCenter 매핑 자동 생성
        CompanyServerRoom mapping = CompanyServerRoom.builder()
                .company(currentMember.getCompany())
                .serverRoom(savedServerRoom)
                .grantedBy(currentMember.getUserName())
                .build();
        companyServerRoomRepository.save(mapping);

        // 히스토리 기록
        serverRoomHistoryRecorder.recordCreate(savedServerRoom, currentMember);

        log.info("Data center created successfully with id: {}, automatically mapped to company: {}",
                savedServerRoom.getId(), currentMember.getCompany().getId());

        return ServerRoomDetailResponse.from(savedServerRoom);
    }

    /**
     * 전산실 정보 수정 + 히스토리 기록
     */
    @Transactional
    public ServerRoomDetailResponse updateDataCenter(Long id,
                                                     ServerRoomUpdateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Updating data center with id: {} by user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        validateWritePermission(currentMember);
        validateDataCenterAccess(currentMember, id);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        // 변경 전 복사
        ServerRoom oldServerRoom = copyDataCenter(serverRoom);

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

        serverRoomHistoryRecorder.recordUpdate(oldServerRoom, serverRoom, currentMember);

        log.info("Data center updated successfully with id: {}", id);

        return ServerRoomDetailResponse.from(serverRoom);
    }

    /**
     * 전산실 삭제 (Soft Delete) + 히스토리 기록
     */
    @Transactional
    public void deleteDataCenter(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Deleting data center with id: {} by user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        validateWritePermission(currentMember);
        validateDataCenterAccess(currentMember, id);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        serverRoomHistoryRecorder.recordDelete(serverRoom, currentMember);

        // 소프트 삭제
        serverRoom.softDelete();

        log.info("Data center soft deleted successfully with id: {}", id);
    }

    /**
     * CompanyDataCenter 기반 전산실 이름 검색
     */
    public List<ServerRoomListResponse> searchDataCentersByName(String name) {
        Member currentMember = getCurrentMember();
        log.info("Searching data centers by name: {} for user: {} (role: {})",
                name, currentMember.getId(), currentMember.getRole());

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<ServerRoom> searchResults;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 전체 검색
            searchResults = serverRoomRepository.searchByName(name);
            log.info("Admin user - searched all data centers, found: {}", searchResults.size());
        } else {
            // 일반 사용자: 접근 가능한 전산실 중에서만 검색
            List<CompanyServerRoom> mappings = companyServerRoomRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            searchResults = mappings.stream()
                    .map(CompanyServerRoom::getServerRoom)
                    .filter(dc -> dc.getName().contains(name))
                    .collect(Collectors.toList());

            log.info("Non-admin user - searched accessible data centers, found: {}", searchResults.size());
        }

        return searchResults.stream()
                .map(ServerRoomListResponse::from)
                .collect(Collectors.toList());
    }


    /**
     * DataCenter Deep Copy (변경 전 상태 저장용)
     */
    private ServerRoom copyDataCenter(ServerRoom original) {
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
                .build();
    }
}