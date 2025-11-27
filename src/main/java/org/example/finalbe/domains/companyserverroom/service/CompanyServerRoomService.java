/**
 * 작성자: 황요한
 * 회사-서버실 매핑 서비스
 * 매핑 생성, 조회, 삭제 기능 제공
 */
package org.example.finalbe.domains.companyserverroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companyserverroom.domain.CompanyServerRoom;
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomCreateRequest;
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomGroupedByDataCenterResponse;
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomResponse;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
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
public class CompanyServerRoomService {

    private final CompanyServerRoomRepository companyServerRoomRepository;
    private final CompanyRepository companyRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final MemberRepository memberRepository;

    /**
     * 인증된 사용자 조회
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
     * 회사-서버실 매핑 생성
     */
    @Transactional
    public List<CompanyServerRoomResponse> createCompanyServerRoomMappings(CompanyServerRoomCreateRequest request) {

        Member current = getCurrentMember();
        log.info("[CompanyServerRoom] Create mappings. companyId={}, user={}", request.companyId(), current.getId());

        if (request.companyId() == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        if (request.serverRoomIds() == null || request.serverRoomIds().isEmpty()) {
            throw new IllegalArgumentException("서버실을 하나 이상 선택해주세요.");
        }

        // 중복 ID 제거
        List<Long> serverRoomIds = new ArrayList<>(new HashSet<>(request.serverRoomIds()));

        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));

        // 서버실 조회
        List<ServerRoom> serverRooms = serverRoomIds.stream()
                .map(id -> serverRoomRepository.findActiveById(id)
                        .orElseThrow(() -> new EntityNotFoundException("서버실", id)))
                .toList();

        // 기존 매핑 중복 체크
        List<Long> duplicates = serverRoomIds.stream()
                .filter(id -> companyServerRoomRepository.existsByCompanyIdAndServerRoomId(company.getId(), id))
                .toList();

        if (!duplicates.isEmpty()) {
            throw new DuplicateException("이미 매핑된 서버실 ID: " + duplicates);
        }

        // 매핑 생성
        List<CompanyServerRoom> saved = new ArrayList<>();
        for (ServerRoom sr : serverRooms) {
            CompanyServerRoom mapping = CompanyServerRoom.builder()
                    .company(company)
                    .serverRoom(sr)
                    .description(request.description())
                    .grantedBy(current.getUserName())
                    .build();

            saved.add(companyServerRoomRepository.save(mapping));
        }

        log.info("[CompanyServerRoom] {} mappings created.", saved.size());

        return saved.stream()
                .map(CompanyServerRoomResponse::from)
                .toList();
    }

    /**
     * 특정 회사의 매핑 목록 조회
     */
    public List<CompanyServerRoomResponse> getCompanyServerRoomsByCompanyId(Long companyId) {
        log.info("[CompanyServerRoom] Fetch mappings by company: {}", companyId);

        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        return companyServerRoomRepository.findByCompanyId(companyId)
                .stream()
                .map(CompanyServerRoomResponse::from)
                .toList();
    }

    /**
     * 특정 서버실의 매핑 목록 조회
     */
    public List<CompanyServerRoomResponse> getCompanyServerRoomsByServerRoomId(Long serverRoomId) {
        log.info("[CompanyServerRoom] Fetch mappings by serverRoom: {}", serverRoomId);

        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        return companyServerRoomRepository.findByServerRoomId(serverRoomId)
                .stream()
                .map(CompanyServerRoomResponse::from)
                .toList();
    }

    /**
     * 단일 매핑 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteCompanyServerRoomMapping(Long companyId, Long serverRoomId) {
        log.info("[CompanyServerRoom] Delete mapping: company={}, serverRoom={}", companyId, serverRoomId);

        CompanyServerRoom mapping = companyServerRoomRepository
                .findByCompanyIdAndServerRoomId(companyId, serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("회사(ID: %d)와 서버실(ID: %d)의 매핑", companyId, serverRoomId)));

        mapping.softDelete();
    }

    /**
     * 회사 기준 매핑 일괄 삭제
     */
    @Transactional
    public int deleteCompanyServerRoomsByCompany(Long companyId, List<Long> serverRoomIds) {
        log.info("[CompanyServerRoom] Batch delete. company={}, rooms={}", companyId, serverRoomIds);

        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        List<CompanyServerRoom> mappings = serverRoomIds.stream()
                .map(id -> companyServerRoomRepository.findByCompanyIdAndServerRoomId(companyId, id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                String.format("회사(ID: %d)와 서버실(ID: %d)의 매핑", companyId, id))))
                .toList();

        mappings.forEach(CompanyServerRoom::softDelete);
        return mappings.size();
    }

    /**
     * 특정 서버실의 모든 회사 매핑 삭제
     */
    @Transactional
    public int deleteAllCompaniesByServerRoom(Long serverRoomId) {
        log.info("[CompanyServerRoom] Delete all companies for serverRoom={}", serverRoomId);

        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<CompanyServerRoom> mappings = companyServerRoomRepository.findByServerRoomId(serverRoomId);

        mappings.forEach(CompanyServerRoom::softDelete);
        return mappings.size();
    }

    /**
     * 특정 서버실-특정 회사들의 매핑 일괄 삭제
     */
    @Transactional
    public int deleteCompaniesByServerRoom(Long serverRoomId, List<Long> companyIds) {
        log.info("[CompanyServerRoom] Batch delete by serverRoom={}, companies={}", serverRoomId, companyIds);

        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<CompanyServerRoom> mappings = companyIds.stream()
                .map(id -> companyServerRoomRepository.findByCompanyIdAndServerRoomId(id, serverRoomId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                String.format("회사(ID: %d)와 서버실(ID: %d)의 매핑", id, serverRoomId))))
                .toList();

        mappings.forEach(CompanyServerRoom::softDelete);
        return mappings.size();
    }

    /**
     * 회사의 서버실 목록을 데이터센터별로 그룹화하여 조회
     */
    public List<CompanyServerRoomGroupedByDataCenterResponse> getCompanyServerRoomsGroupedByDataCenter(Long companyId) {
        log.info("[CompanyServerRoom] Fetch grouped mappings for company={}", companyId);

        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        List<CompanyServerRoom> mappings = companyServerRoomRepository.findByCompanyId(companyId);

        Map<Long, List<CompanyServerRoom>> grouped = mappings.stream()
                .filter(m -> m.getServerRoom().getDataCenter() != null)
                .collect(Collectors.groupingBy(m -> m.getServerRoom().getDataCenter().getId()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    DataCenter dc = entry.getValue().get(0).getServerRoom().getDataCenter();

                    List<CompanyServerRoomGroupedByDataCenterResponse.ServerRoomInfo> rooms =
                            entry.getValue().stream()
                                    .map(m -> {
                                        ServerRoom sr = m.getServerRoom();
                                        return CompanyServerRoomGroupedByDataCenterResponse.ServerRoomInfo.builder()
                                                .id(sr.getId())
                                                .name(sr.getName())
                                                .code(sr.getCode())
                                                .location(sr.getLocation())
                                                .floor(sr.getFloor())
                                                .rows(sr.getRows())
                                                .columns(sr.getColumns())
                                                .status(sr.getStatus())
                                                .description(sr.getDescription())
                                                .build();
                                    })
                                    .toList();

                    return CompanyServerRoomGroupedByDataCenterResponse.builder()
                            .dataCenterId(dc.getId())
                            .dataCenterName(dc.getName())
                            .dataCenterCode(dc.getCode())
                            .dataCenterAddress(dc.getAddress())
                            .serverRooms(rooms)
                            .build();
                })
                .toList();
    }
}
