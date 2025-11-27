/**
 * 작성자: 황요한
 * 히스토리 서비스 클래스
 */
package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.history.domain.History;
import org.example.finalbe.domains.history.dto.*;
import org.example.finalbe.domains.history.repository.HistoryRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository companyServerRoomRepository;

    /**
     * 히스토리 기록
     */
    @Transactional
    public void recordHistory(HistoryCreateRequest request) {
        try {
            History history = History.builder()
                    .serverRoomId(request.serverRoomId())
                    .serverRoomName(request.serverRoomName())
                    .entityType(request.entityType())
                    .entityId(request.entityId())
                    .entityName(request.entityName())
                    .entityCode(request.entityCode())
                    .action(request.action())
                    .changedBy(request.changedBy())
                    .changedByName(request.changedByName())
                    .changedByRole(request.changedByRole())
                    .changedAt(LocalDateTime.now())
                    .changedFields(request.changedFieldsAsJson())
                    .beforeValue(request.beforeValueAsJson())
                    .afterValue(request.afterValueAsJson())
                    .metadata(request.metadataAsJson())
                    .build();

            historyRepository.save(history);
            log.debug("History recorded: {} {} by {}",
                    request.action(), request.entityType(), request.changedByName());

        } catch (Exception e) {
            log.error("Failed to record history: {}", e.getMessage(), e);
        }
    }

    /**
     * 복합 검색
     */
    public Page<HistoryResponse> searchHistory(HistorySearchRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Searching history for serverroom: {}", request.serverRoomId());

        validateServerRoomAccess(currentMember, request.serverRoomId());

        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20
        );

        Page<History> historyPage = historyRepository.searchHistory(
                request.serverRoomId(),
                request.entityType(),
                request.action(),
                request.changedBy(),
                request.startDate(),
                request.endDate(),
                pageable
        );

        return historyPage.map(HistoryResponse::from);
    }

    /**
     * 서버실 히스토리 통계
     */
    public HistoryStatisticsResponse getStatistics(Long serverRoomId, LocalDateTime startDate, LocalDateTime endDate) {
        Member currentMember = getCurrentMember();
        validateServerRoomAccess(currentMember, serverRoomId);

        ServerRoom serverRoom = serverRoomRepository.findById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("전산실", serverRoomId));

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        List<Object[]> actionCounts = historyRepository.countByActionAndDateRange(
                serverRoomId, startDate, endDate);
        Map<HistoryAction, Long> actionCountMap = new HashMap<>();
        long totalCount = 0;
        for (Object[] row : actionCounts) {
            HistoryAction action = (HistoryAction) row[0];
            Long count = (Long) row[1];
            actionCountMap.put(action, count);
            totalCount += count;
        }

        List<Object[]> entityTypeCounts = historyRepository.countByEntityTypeAndDateRange(
                serverRoomId, startDate, endDate);
        Map<EntityType, Long> entityTypeCountMap = new HashMap<>();
        for (Object[] row : entityTypeCounts) {
            EntityType entityType = (EntityType) row[0];
            Long count = (Long) row[1];
            entityTypeCountMap.put(entityType, count);
        }

        Pageable topEntitiesPageable = PageRequest.of(0, 10);
        List<Object[]> topEntitiesData = historyRepository.findTopActiveEntities(
                serverRoomId, startDate, endDate, topEntitiesPageable);
        List<HistoryStatisticsResponse.TopActiveEntity> topActiveEntities = topEntitiesData.stream()
                .map(row -> HistoryStatisticsResponse.TopActiveEntity.builder()
                        .entityType((EntityType) row[0])
                        .entityId((Long) row[1])
                        .entityName((String) row[2])
                        .changeCount((Long) row[3])
                        .build())
                .collect(Collectors.toList());

        Pageable topUsersPageable = PageRequest.of(0, 10);
        List<Object[]> topUsersData = historyRepository.findTopActiveUsers(
                serverRoomId, startDate, endDate, topUsersPageable);
        List<HistoryStatisticsResponse.TopActiveUser> topActiveUsers = topUsersData.stream()
                .map(row -> HistoryStatisticsResponse.TopActiveUser.builder()
                        .userId((Long) row[0])
                        .userName((String) row[1])
                        .changeCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        return HistoryStatisticsResponse.builder()
                .serverRoomId(serverRoomId)
                .serverRoomName(serverRoom.getName())
                .startDate(startDate)
                .endDate(endDate)
                .totalCount(totalCount)
                .actionCounts(actionCountMap)
                .entityTypeCounts(entityTypeCountMap)
                .topActiveEntities(topActiveEntities)
                .topActiveUsers(topActiveUsers)
                .build();
    }

    /**
     * 내 히스토리 조회
     */
    public Page<HistoryResponse> getMyHistory(Integer page, Integer size) {
        Member currentMember = getCurrentMember();

        if (page == null) page = 0;
        if (size == null) size = 20;

        Pageable pageable = PageRequest.of(page, size);
        Page<History> historyPage = historyRepository.findByChangedByOrderByChangedAtDesc(
                currentMember.getId(), pageable);

        return historyPage.map(HistoryResponse::from);
    }

    /**
     * 특정 사용자 히스토리 조회
     */
    public Page<HistoryResponse> getUserHistory(Long userId, Integer page, Integer size) {
        Member currentMember = getCurrentMember();

        if (currentMember.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자만 다른 사용자의 히스토리를 조회할 수 있습니다.");
        }

        if (page == null) page = 0;
        if (size == null) size = 20;

        Pageable pageable = PageRequest.of(page, size);
        Page<History> historyPage = historyRepository.findByChangedByOrderByChangedAtDesc(userId, pageable);

        return historyPage.map(HistoryResponse::from);
    }

    /**
     * 히스토리 상세 조회
     */
    public HistoryDetailResponse getHistoryDetail(Long id) {
        log.info("Fetching history detail for id: {}", id);

        History history = historyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("히스토리", id));

        return HistoryDetailResponse.from(history);
    }

    /**
     * 엔티티별 히스토리 목록 조회
     */
    public Page<HistoryDetailResponse> getHistoryByEntity(
            EntityType entityType,
            Long entityId,
            int page,
            int size) {

        log.info("Fetching history for entity: {} id: {}", entityType, entityId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("changedAt").descending());

        Page<History> histories = historyRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(
                entityType, entityId, pageable);

        return histories.map(HistoryDetailResponse::from);
    }

    /**
     * 서버실별 최근 히스토리 조회
     */
    public Page<HistoryDetailResponse> getRecentHistoriesWithDetails(
            Long serverRoomId,
            int page,
            int size) {

        log.info("Fetching recent histories with details for serverroom: {}", serverRoomId);

        Member currentMember = getCurrentMember();
        validateServerRoomAccess(currentMember, serverRoomId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("changedAt").descending());

        Page<History> histories = historyRepository.findByServerRoomIdOrderByChangedAtDesc(
                serverRoomId, pageable);

        return histories.map(HistoryDetailResponse::from);
    }

    /**
     * 사용자가 접근 가능한 모든 서버실 ID 조회
     */
    public List<Long> getAccessibleServerRoomIds(Member member) {
        if (member.getRole() == Role.ADMIN) {
            return serverRoomRepository.findByDelYn(DelYN.N).stream()
                    .map(ServerRoom::getId)
                    .collect(Collectors.toList());
        }

        List<Long> serverRoomIds = companyServerRoomRepository
                .findByCompanyId(member.getCompany().getId())
                .stream()
                .map(csr -> csr.getServerRoom().getId())
                .collect(Collectors.toList());

        log.info("User {} (company {}) can access {} serverrooms",
                member.getId(), member.getCompany().getId(), serverRoomIds.size());

        return serverRoomIds;
    }

    /**
     * 사용자의 접근 가능한 서버실 목록 조회
     */
    public List<ServerRoomAccessResponse> getMyAccessibleServerRooms() {
        Member currentMember = getCurrentMember();
        List<Long> accessibleServerRoomIds = getAccessibleServerRoomIds(currentMember);

        if (accessibleServerRoomIds.isEmpty()) {
            log.warn("User {} has no accessible serverrooms", currentMember.getId());
            return List.of();
        }

        return serverRoomRepository.findAllById(accessibleServerRoomIds).stream()
                .map(sr -> ServerRoomAccessResponse.builder()
                        .serverRoomId(sr.getId())
                        .serverRoomName(sr.getName())
                        .serverRoomCode(sr.getCode())
                        .location(sr.getLocation())
                        .build())
                .collect(Collectors.toList());
    }

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
     * 서버실 접근 권한 검증
     */
    private void validateServerRoomAccess(Member member, Long serverRoomId) {
        if (serverRoomId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        if (member.getRole() == Role.ADMIN) {
            log.debug("Admin user {} accessing serverroom {}", member.getId(), serverRoomId);
            return;
        }

        boolean hasAccess = companyServerRoomRepository
                .existsByCompanyIdAndServerRoomId(member.getCompany().getId(), serverRoomId);

        if (!hasAccess) {
            log.warn("User {} (company {}) denied access to serverroom {}",
                    member.getId(), member.getCompany().getId(), serverRoomId);
            throw new AccessDeniedException(
                    "해당 전산실의 히스토리 조회 권한이 없습니다. (회사가 관리하는 서버실이 아닙니다)");
        }

        log.debug("User {} granted access to serverroom {} through company {}",
                member.getId(), serverRoomId, member.getCompany().getId());
    }
}