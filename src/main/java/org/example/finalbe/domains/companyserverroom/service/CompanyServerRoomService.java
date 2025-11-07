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
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomResponse;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 회사-서버실 매핑 서비스
 * 매핑 생성, 조회, 삭제 기능 제공
 */
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
     * 회사-서버실 매핑 생성
     */
    @Transactional
    public List<CompanyServerRoomResponse> createCompanyServerRoomMappings(
            CompanyServerRoomCreateRequest request) {

        Member currentMember = getCurrentMember();
        log.info("Creating company-serverRoom mappings for company: {} by user: {}",
                request.companyId(), currentMember.getId());

        if (request.companyId() == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }
        if (request.serverRoomIds() == null || request.serverRoomIds().isEmpty()) {
            throw new IllegalArgumentException("서버실을 하나 이상 선택해주세요.");
        }

        // 중복 제거
        List<Long> uniqueServerRoomIds = new ArrayList<>(new HashSet<>(request.serverRoomIds()));
        if (uniqueServerRoomIds.size() != request.serverRoomIds().size()) {
            log.warn("Duplicate serverRoom IDs found in request, removed duplicates");
        }

        // 회사 존재 확인
        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));

        // 서버실 존재 여부 검증
        List<ServerRoom> serverRooms = new ArrayList<>();
        for (Long serverRoomId : uniqueServerRoomIds) {
            if (serverRoomId == null) {
                throw new IllegalArgumentException("서버실 ID는 null일 수 없습니다.");
            }

            ServerRoom serverRoom = serverRoomRepository.findActiveById(serverRoomId)
                    .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));
            serverRooms.add(serverRoom);
        }

        // 중복 매핑 체크
        List<Long> duplicateIds = new ArrayList<>();
        for (Long serverRoomId : uniqueServerRoomIds) {
            if (companyServerRoomRepository.existsByCompanyIdAndServerRoomId(
                    request.companyId(), serverRoomId)) {
                duplicateIds.add(serverRoomId);
            }
        }

        if (!duplicateIds.isEmpty()) {
            String duplicateIdsStr = duplicateIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new DuplicateException(
                    String.format("이미 매핑된 서버실이 있습니다. 서버실 ID: %s", duplicateIdsStr));
        }

        // 매핑 생성
        List<CompanyServerRoom> savedMappings = new ArrayList<>();
        try {
            for (ServerRoom serverRoom : serverRooms) {
                CompanyServerRoom companyServerRoom = CompanyServerRoom.builder()
                        .company(company)
                        .serverRoom(serverRoom)
                        .description(request.description())
                        .grantedBy(currentMember.getUserName())
                        .build();

                CompanyServerRoom saved = companyServerRoomRepository.save(companyServerRoom);
                savedMappings.add(saved);

                log.debug("Company-serverRoom mapping created: company={}, serverRoom={}",
                        request.companyId(), serverRoom.getId());
            }

            log.info("Successfully created {} company-serverRoom mappings", savedMappings.size());

            return savedMappings.stream()
                    .map(CompanyServerRoomResponse::from)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to create company-serverRoom mappings, transaction will rollback", e);
            throw new IllegalStateException("매핑 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 회사의 서버실 매핑 목록 조회
     */
    public List<CompanyServerRoomResponse> getCompanyServerRoomsByCompanyId(Long companyId) {
        log.info("Fetching company-serverRoom mappings for company: {}", companyId);

        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        List<CompanyServerRoomResponse> mappings = companyServerRoomRepository
                .findByCompanyId(companyId)
                .stream()
                .map(CompanyServerRoomResponse::from)
                .collect(Collectors.toList());

        log.info("Found {} mappings for company: {}", mappings.size(), companyId);
        return mappings;
    }

    /**
     * 서버실의 회사 매핑 목록 조회
     */
    public List<CompanyServerRoomResponse> getCompanyServerRoomsByServerRoomId(Long serverRoomId) {
        log.info("Fetching company-serverRoom mappings for serverRoom: {}", serverRoomId);

        if (serverRoomId == null) {
            throw new IllegalArgumentException("서버실 ID를 입력해주세요.");
        }

        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<CompanyServerRoomResponse> mappings = companyServerRoomRepository
                .findByServerRoomId(serverRoomId)
                .stream()
                .map(CompanyServerRoomResponse::from)
                .collect(Collectors.toList());

        log.info("Found {} mappings for serverRoom: {}", mappings.size(), serverRoomId);
        return mappings;
    }

    /**
     * 회사-서버실 매핑 삭제
     */
    @Transactional
    public void deleteCompanyServerRoomMapping(Long companyId, Long serverRoomId) {
        log.info("Deleting company-serverRoom mapping: company={}, serverRoom={}",
                companyId, serverRoomId);

        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }
        if (serverRoomId == null) {
            throw new IllegalArgumentException("서버실 ID를 입력해주세요.");
        }

        CompanyServerRoom companyServerRoom = companyServerRoomRepository
                .findByCompanyIdAndServerRoomId(companyId, serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("회사(ID: %d)와 서버실(ID: %d)의 매핑", companyId, serverRoomId)));

        companyServerRoom.softDelete();

        log.info("Company-serverRoom mapping deleted successfully");
    }

    /**
     * 특정 회사의 여러 서버실 매핑 일괄 삭제
     */
    @Transactional
    public int deleteCompanyServerRoomsByCompany(Long companyId, List<Long> serverRoomIds) {
        log.info("Deleting multiple company-serverRoom mappings: company={}, serverRoomIds={}",
                companyId, serverRoomIds);

        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }
        if (serverRoomIds == null || serverRoomIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 서버실을 하나 이상 선택해주세요.");
        }

        for (Long serverRoomId : serverRoomIds) {
            if (serverRoomId == null) {
                throw new IllegalArgumentException("서버실 ID는 null일 수 없습니다.");
            }
        }

        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));

        List<CompanyServerRoom> mappingsToDelete = new ArrayList<>();
        for (Long serverRoomId : serverRoomIds) {
            CompanyServerRoom mapping = companyServerRoomRepository
                    .findByCompanyIdAndServerRoomId(companyId, serverRoomId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("회사(ID: %d)와 서버실(ID: %d)의 매핑", companyId, serverRoomId)));
            mappingsToDelete.add(mapping);
        }

        mappingsToDelete.forEach(CompanyServerRoom::softDelete);

        log.info("Successfully deleted {} company-serverRoom mappings for company {}",
                mappingsToDelete.size(), companyId);

        return mappingsToDelete.size();
    }

    /**
     * 특정 서버실의 모든 회사 매핑 삭제
     */
    @Transactional
    public int deleteAllCompaniesByServerRoom(Long serverRoomId) {
        log.info("Deleting all company mappings for serverRoom: {}", serverRoomId);

        if (serverRoomId == null) {
            throw new IllegalArgumentException("서버실 ID를 입력해주세요.");
        }

        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<CompanyServerRoom> mappingsToDelete = companyServerRoomRepository
                .findByServerRoomId(serverRoomId);

        if (mappingsToDelete.isEmpty()) {
            log.info("No mappings found for serverRoom: {}", serverRoomId);
            return 0;
        }

        mappingsToDelete.forEach(CompanyServerRoom::softDelete);

        log.info("Successfully deleted {} company-serverRoom mappings for serverRoom {}",
                mappingsToDelete.size(), serverRoomId);

        return mappingsToDelete.size();
    }

    /**
     * 특정 서버실의 특정 회사들 매핑 일괄 삭제
     */
    @Transactional
    public int deleteCompaniesByServerRoom(Long serverRoomId, List<Long> companyIds) {

        if (serverRoomId == null) {
            throw new IllegalArgumentException("서버실 ID를 입력해주세요.");
        }
        if (companyIds == null || companyIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 회사를 하나 이상 선택해주세요.");
        }

        for (Long companyId : companyIds) {
            if (companyId == null) {
                throw new IllegalArgumentException("회사 ID는 null일 수 없습니다.");
            }
        }

        serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<CompanyServerRoom> mappingsToDelete = new ArrayList<>();
        for (Long companyId : companyIds) {
            CompanyServerRoom mapping = companyServerRoomRepository
                    .findByCompanyIdAndServerRoomId(companyId, serverRoomId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("회사(ID: %d)와 서버실(ID: %d)의 매핑", companyId, serverRoomId)));
            mappingsToDelete.add(mapping);
        }

        mappingsToDelete.forEach(CompanyServerRoom::softDelete);

        log.info("Successfully deleted {} company-serverRoom mappings for serverRoom {}",
                mappingsToDelete.size(), serverRoomId);

        return mappingsToDelete.size();
    }
}