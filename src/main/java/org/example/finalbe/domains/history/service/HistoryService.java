package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.history.domain.History;
import org.example.finalbe.domains.history.dto.*;


import org.example.finalbe.domains.history.repository.HistoryRepository;
import org.example.finalbe.domains.common.enumdir.Role;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 히스토리 서비스
 * 변경 이력 조회 및 통계 제공
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final DataCenterRepository dataCenterRepository;
    private final CompanyDataCenterRepository companyDataCenterRepository;
    private final MemberRepository memberRepository;

    /**
     * 히스토리 기록 (내부 사용)
     */
    @Transactional
    public void recordHistory(HistoryCreateRequest request) {
        try {
            History history = History.builder()
                    .dataCenterId(request.dataCenterId())
                    .dataCenterName(request.dataCenterName())
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
                    .description(request.generateDescription())
                    .build();

            historyRepository.save(history);
            log.info("History recorded: {} - {} - {}",
                    request.action(), request.entityType(), request.entityName());
        } catch (Exception e) {
            log.error("Failed to record history: {}", e.getMessage(), e);
            // 히스토리 기록 실패가 비즈니스 로직에 영향을 주지 않도록 예외를 던지지 않음
        }
    }

    /**
     * 히스토리 검색
     */
    public Page<HistoryResponse> searchHistory(HistorySearchRequest request) {
        Member currentMember = getCurrentMember();
        validateDataCenterAccess(currentMember, request.dataCenterId());

        Pageable pageable = PageRequest.of(request.page(), request.size());
        Page<History> historyPage;

        // 특정 엔티티의 히스토리 조회
        if (request.entityId() != null && request.entityType() != null) {
            historyPage = historyRepository.findByDataCenterIdAndEntityTypeAndEntityIdOrderByChangedAtDesc(
                    request.dataCenterId(),
                    request.entityType(),
                    request.entityId(),
                    pageable
            );
        }
        // 복합 검색
        else if (request.entityType() != null || request.action() != null ||
                request.changedBy() != null || request.startDate() != null || request.endDate() != null) {
            historyPage = historyRepository.searchHistory(
                    request.dataCenterId(),
                    request.entityType(),
                    request.action(),
                    request.changedBy(),
                    request.startDate(),
                    request.endDate(),
                    pageable
            );
        }
        // 서버실 전체 히스토리
        else if (request.startDate() != null && request.endDate() != null) {
            historyPage = historyRepository.findByDataCenterIdAndChangedAtBetweenOrderByChangedAtDesc(
                    request.dataCenterId(),
                    request.startDate(),
                    request.endDate(),
                    pageable
            );
        }
        else {
            historyPage = historyRepository.findByDataCenterIdOrderByChangedAtDesc(
                    request.dataCenterId(),
                    pageable
            );
        }

        return historyPage.map(HistoryResponse::from);
    }

    /**
     * 서버실 히스토리 통계
     */
    public HistoryStatisticsResponse getStatistics(Long dataCenterId, LocalDateTime startDate, LocalDateTime endDate) {
        Member currentMember = getCurrentMember();
        validateDataCenterAccess(currentMember, dataCenterId);

        DataCenter dataCenter = dataCenterRepository.findById(dataCenterId)
                .orElseThrow(() -> new EntityNotFoundException("전산실", dataCenterId));

        // 기본 기간 설정 (없으면 최근 30일)
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        // 작업 타입별 카운트
        List<Object[]> actionCounts = historyRepository.countByActionAndDateRange(
                dataCenterId, startDate, endDate);
        Map<HistoryAction, Long> actionCountMap = new HashMap<>();
        long totalCount = 0;
        for (Object[] row : actionCounts) {
            HistoryAction action = (HistoryAction) row[0];
            Long count = (Long) row[1];
            actionCountMap.put(action, count);
            totalCount += count;
        }

        // 엔티티 타입별 카운트
        List<Object[]> entityTypeCounts = historyRepository.countByEntityTypeAndDateRange(
                dataCenterId, startDate, endDate);
        Map<EntityType, Long> entityTypeCountMap = new HashMap<>();
        for (Object[] row : entityTypeCounts) {
            EntityType entityType = (EntityType) row[0];
            Long count = (Long) row[1];
            entityTypeCountMap.put(entityType, count);
        }

        // 최근 활동 많은 자산 TOP 10
        Pageable topEntitiesPageable = PageRequest.of(0, 10);
        List<Object[]> topEntitiesData = historyRepository.findTopActiveEntities(
                dataCenterId, startDate, endDate, topEntitiesPageable);
        List<HistoryStatisticsResponse.TopActiveEntity> topActiveEntities = topEntitiesData.stream()
                .map(row -> HistoryStatisticsResponse.TopActiveEntity.builder()
                        .entityType((EntityType) row[0])
                        .entityId((Long) row[1])
                        .entityName((String) row[2])
                        .changeCount((Long) row[3])
                        .build())
                .collect(Collectors.toList());

        // 최근 활동 많은 사용자 TOP 10
        Pageable topUsersPageable = PageRequest.of(0, 10);
        List<Object[]> topUsersData = historyRepository.findTopActiveUsers(
                dataCenterId, startDate, endDate, topUsersPageable);
        List<HistoryStatisticsResponse.TopActiveUser> topActiveUsers = topUsersData.stream()
                .map(row -> HistoryStatisticsResponse.TopActiveUser.builder()
                        .userId((Long) row[0])
                        .userName((String) row[1])
                        .changeCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        return HistoryStatisticsResponse.builder()
                .dataCenterId(dataCenterId)
                .dataCenterName(dataCenter.getName())
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
     * 내 히스토리 조회 (본인만)
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
     * 특정 사용자 히스토리 조회 (ADMIN 전용)
     */
    public Page<HistoryResponse> getUserHistory(Long userId, Integer page, Integer size) {
        Member currentMember = getCurrentMember();

        // ADMIN만 조회 가능
        if (currentMember.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자만 다른 사용자의 히스토리를 조회할 수 있습니다.");
        }

        if (page == null) page = 0;
        if (size == null) size = 20;

        Pageable pageable = PageRequest.of(page, size);
        Page<History> historyPage = historyRepository.findByChangedByOrderByChangedAtDesc(userId, pageable);

        return historyPage.map(HistoryResponse::from);
    }

    // === Private Helper Methods ===

    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
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

    private void validateDataCenterAccess(Member member, Long dataCenterId) {
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        if (member.getRole() == Role.ADMIN) {
            return; // ADMIN은 모든 전산실 히스토리 조회 가능
        }

        // 회사의 전산실 접근 권한 확인
        boolean hasAccess = companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                member.getCompany().getId(),
                dataCenterId
        );

        if (!hasAccess) {
            throw new AccessDeniedException("해당 전산실의 히스토리 조회 권한이 없습니다.");
        }
    }

    /**
     * 히스토리 상세 조회
     * 변경 내역을 상세하게 파싱하여 반환
     */
    public HistoryDetailResponse getHistoryDetail(Long id) {
        log.info("Fetching history detail for id: {}", id);

        History history = historyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("히스토리", id));

        return HistoryDetailResponse.from(history);
    }

    /**
     * 엔티티별 히스토리 목록 조회 (페이징)
     * 특정 엔티티(예: 특정 랙)의 모든 히스토리 조회
     */
    public Page<HistoryDetailResponse> getHistoryByEntity(
            EntityType entityType,
            Long entityId,
            int page,
            int size) {

        log.info("Fetching history for entity: {} id: {}", entityType, entityId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("changedAt").descending());

        // 엔티티의 dataCenterId를 먼저 조회해야 할 수도 있음
        // 여기서는 단순화를 위해 entityType과 entityId만으로 조회
        Page<History> histories = historyRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(
                entityType, entityId, pageable);

        return histories.map(HistoryDetailResponse::from);
    }

    /**
     * 서버실별 최근 히스토리 조회 (상세 정보 포함)
     */
    public Page<HistoryDetailResponse> getRecentHistoriesWithDetails(
            Long dataCenterId,
            int page,
            int size) {

        log.info("Fetching recent histories with details for datacenter: {}", dataCenterId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("changedAt").descending());

        Page<History> histories = historyRepository.findByDataCenterIdOrderByChangedAtDesc(
                dataCenterId, pageable);

        return histories.map(HistoryDetailResponse::from);
    }
}