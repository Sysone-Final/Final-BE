package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.department.domain.MemberDepartment;
import org.example.finalbe.domains.department.repository.MemberDepartmentRepository;
import org.example.finalbe.domains.department.repository.RackDepartmentRepository;
import org.example.finalbe.domains.history.domain.History;
import org.example.finalbe.domains.history.dto.*;
import org.example.finalbe.domains.history.repository.HistoryRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
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

/**
 * 히스토리 서비스
 * 변경 이력 조회 및 통계 제공
 *
 * 접근 제어: 부서 기준
 * - 사용자가 소속된 부서가 담당하는 랙이 있는 서버실만 접근 가능
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final MemberRepository memberRepository;

    // 부서 기준 접근 제어용 Repository
    private final RackDepartmentRepository rackDepartmentRepository;
    private final MemberDepartmentRepository memberDepartmentRepository;

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
                    .build();

            historyRepository.save(history);
            log.debug("History recorded: {} {} by {}",
                    request.action(), request.entityType(), request.changedByName());

        } catch (Exception e) {
            log.error("Failed to record history: {}", e.getMessage(), e);
            // 히스토리 기록 실패는 메인 트랜잭션을 롤백하지 않음 (선택사항)
        }
    }

    /**
     * 복합 검색
     */
    public Page<HistoryResponse> searchHistory(HistorySearchRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Searching history for datacenter: {}", request.dataCenterId());

        // 서버실 접근 권한 확인 (부서 기준)
        validateDataCenterAccess(currentMember, request.dataCenterId());

        Pageable pageable = PageRequest.of(
                request.page() != null ? request.page() : 0,
                request.size() != null ? request.size() : 20
        );

        Page<History> historyPage;

        // 복합 조건 검색 사용
        historyPage = historyRepository.searchHistory(
                request.dataCenterId(),
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
    public HistoryStatisticsResponse getStatistics(Long dataCenterId, LocalDateTime startDate, LocalDateTime endDate) {
        Member currentMember = getCurrentMember();
        validateDataCenterAccess(currentMember, dataCenterId);

        ServerRoom serverRoom = serverRoomRepository.findById(dataCenterId)
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
                .dataCenterName(serverRoom.getName())
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

        Member currentMember = getCurrentMember();
        validateDataCenterAccess(currentMember, dataCenterId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("changedAt").descending());

        Page<History> histories = historyRepository.findByDataCenterIdOrderByChangedAtDesc(
                dataCenterId, pageable);

        return histories.map(HistoryDetailResponse::from);
    }

    /**
     * 사용자가 접근 가능한 모든 서버실 ID 조회
     *
     * 접근 제어 로직:
     * - ADMIN: 모든 서버실 접근 가능
     * - 일반 사용자: 자신의 부서가 담당하는 랙이 있는 서버실만 접근 가능
     *
     * @param member 사용자
     * @return 접근 가능한 서버실 ID 목록
     */
    public List<Long> getAccessibleDataCenterIds(Member member) {
        if (member.getRole() == Role.ADMIN) {
            // ADMIN은 모든 서버실 접근 가능
            return serverRoomRepository.findAll().stream()
                    .map(ServerRoom::getId)
                    .collect(Collectors.toList());
        }

        // 사용자의 주 부서 조회
        Optional<MemberDepartment> memberDepartmentOpt = memberDepartmentRepository
                .findByMemberIdAndIsPrimary(member.getId(), true);

        if (memberDepartmentOpt.isEmpty()) {
            log.warn("User {} has no primary department assigned", member.getId());
            return List.of(); // 부서 없으면 접근 가능한 서버실 없음
        }

        Long departmentId = memberDepartmentOpt.get().getDepartment().getId();
        log.debug("User {} belongs to department {}", member.getId(), departmentId);

        // 부서가 담당하는 랙들이 속한 서버실 ID 수집 (중복 제거)
        List<Rack> departmentRacks = rackDepartmentRepository.findRacksByDepartmentId(departmentId);

        List<Long> dataCenterIds = departmentRacks.stream()
                .map(rack -> rack.getDatacenter().getId())
                .distinct()
                .collect(Collectors.toList());

        log.info("User {} can access {} datacenters through department {}",
                member.getId(), dataCenterIds.size(), departmentId);

        return dataCenterIds;
    }

    /**
     * 사용자의 접근 가능한 서버실 목록 조회 (API용)
     *
     * 사용 목적:
     * - 프론트엔드에서 히스토리 조회 시 서버실 선택 드롭다운에 사용
     * - 사용자가 소속된 부서가 담당하는 랙이 있는 서버실만 표시
     *
     * @return 접근 가능한 서버실 목록 (DTO)
     */
    public List<ServerRoomAccessResponse> getMyAccessibleDataCenters() {
        Member currentMember = getCurrentMember();
        List<Long> accessibleDataCenterIds = getAccessibleDataCenterIds(currentMember);

        if (accessibleDataCenterIds.isEmpty()) {
            log.warn("User {} has no accessible datacenters", currentMember.getId());
            return List.of();
        }

        return serverRoomRepository.findAllById(accessibleDataCenterIds).stream()
                .map(dc -> ServerRoomAccessResponse.builder()
                        .dataCenterId(dc.getId())
                        .dataCenterName(dc.getName())
                        .dataCenterCode(dc.getCode())
                        .location(dc.getLocation())
                        .build())
                .collect(Collectors.toList());
    }

    // ========== Private Helper Methods ==========

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
     * 서버실 접근 권한 검증 (부서 기준)
     *
     * 접근 제어 로직:
     * 1. ADMIN은 모든 서버실 접근 가능
     * 2. 일반 사용자는 자신의 부서가 담당하는 랙이 속한 서버실만 접근 가능
     * 3. 부서 소속이 없으면 접근 불가
     *
     * @param member 사용자
     * @param dataCenterId 서버실 ID
     * @throws AccessDeniedException 접근 권한이 없는 경우
     */
    private void validateDataCenterAccess(Member member, Long dataCenterId) {
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        if (member.getRole() == Role.ADMIN) {
            log.debug("Admin user {} accessing datacenter {}", member.getId(), dataCenterId);
            return; // ADMIN은 모든 전산실 접근 가능
        }

        // 사용자의 주 부서 조회
        MemberDepartment memberDepartment = memberDepartmentRepository
                .findByMemberIdAndIsPrimary(member.getId(), true)
                .orElseThrow(() -> new AccessDeniedException(
                        "부서에 소속되지 않아 히스토리를 조회할 수 없습니다."));

        Long departmentId = memberDepartment.getDepartment().getId();
        log.debug("User {} belongs to department {}, checking access to datacenter {}",
                member.getId(), departmentId, dataCenterId);

        // 부서가 담당하는 랙들 조회
        List<Rack> departmentRacks = rackDepartmentRepository.findRacksByDepartmentId(departmentId);

        // 해당 랙들이 속한 서버실 ID 확인
        boolean hasAccess = departmentRacks.stream()
                .anyMatch(rack -> rack.getDatacenter().getId().equals(dataCenterId));

        if (!hasAccess) {
            log.warn("User {} (department {}) denied access to datacenter {} - no assigned racks",
                    member.getId(), departmentId, dataCenterId);
            throw new AccessDeniedException(
                    "해당 전산실의 히스토리 조회 권한이 없습니다. (부서가 담당하는 랙이 없습니다)");
        }

        log.debug("User {} granted access to datacenter {} through department {}",
                member.getId(), dataCenterId, departmentId);
    }
}