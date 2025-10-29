package org.example.finalbe.domains.datacenter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;

import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전산실(데이터센터) 서비스
 * 전산실 생성, 조회, 수정, 삭제 기능 제공
 *
 * - Spring Security: 현재 인증된 사용자 정보 조회
 * - 권한 기반 접근 제어: ADMIN, OPERATOR, VIEWER 역할별 권한 분리
 * - Soft Delete: 실제 삭제가 아닌 논리 삭제 처리
 * - @Transactional: 데이터 일관성 보장
 */
@Slf4j // Lombok의 로깅 기능 (log.info(), log.error() 등 사용 가능)
@Service // Spring의 Service Layer Bean으로 등록
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 (성능 최적화)
// 읽기 전용 트랜잭션은 Dirty Checking을 하지 않아 성능이 향상됨
public class DataCenterService {

    // === 의존성 주입 (생성자 주입) ===
    private final DataCenterRepository dataCenterRepository; // 전산실 데이터 접근
    private final MemberRepository memberRepository; // 회원 데이터 접근

    /**
     * 현재 인증된 사용자 조회
     * Spring Security의 SecurityContext에서 인증 정보를 가져옴
     */
    private Member getCurrentMember() {
        // SecurityContextHolder: Spring Security의 인증 정보를 저장하는 ThreadLocal 기반 저장소
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Authentication: 현재 사용자의 인증 정보 (Principal, Credentials, Authorities 포함)

        // === 1단계: 인증 정보 존재 여부 확인 ===
        if (authentication == null || !authentication.isAuthenticated()) {
            // 인증 정보가 없거나 인증되지 않은 경우 예외 발생
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        // === 2단계: 사용자 ID 추출 ===
        String userId = authentication.getName(); // Principal(주체)의 이름 (JWT의 subject에 저장된 회원 ID)
        // JwtAuthenticationFilter에서 JWT 토큰을 파싱하여 SecurityContext에 저장한 사용자 ID

        // === 3단계: 익명 사용자 확인 ===
        if (userId == null || userId.equals("anonymousUser")) {
            // anonymousUser: Spring Security의 기본 익명 사용자 이름
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        // === 4단계: 회원 조회 ===
        try {
            // userId를 Long으로 변환하여 회원 조회
            return memberRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 예외 발생
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 확인
     * ADMIN, OPERATOR만 생성/수정/삭제 가능
     * VIEWER는 읽기 전용
     */
    private void validateWritePermission(Member member) {
        // Role Enum의 ADMIN, OPERATOR가 아니면 예외 발생
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("관리자 또는 운영자만 수정할 수 있습니다.");
        }
    }

    /**
     * 전산실 접근 권한 확인
     * ADMIN: 모든 전산실 접근 가능
     * OPERATOR/VIEWER: 자기 회사에 할당된 전산실만 접근 가능
     *
     * @param member 현재 사용자
     * @param dataCenterId 전산실 ID
     */
    private void validateDataCenterAccess(Member member, Long dataCenterId) {
        // === 1단계: ID 유효성 검증 ===
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // === 2단계: ADMIN은 모든 전산실 접근 가능 ===
        if (member.getRole() == Role.ADMIN) {
            return; // 조기 반환 (추가 검증 불필요)
        }

        // === 3단계: OPERATOR, VIEWER는 회사-전산실 매핑 확인 ===
        // CompanyDataCenter 테이블을 통해 회사가 해당 전산실에 접근 가능한지 확인
        if (!dataCenterRepository.hasAccessToDataCenter(member.getCompany().getId(), dataCenterId)) {
            throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 사용자가 접근 가능한 전산실 목록 조회
     * ADMIN: 모든 전산실 조회
     * OPERATOR/VIEWER: 자기 회사에 할당된 전산실만 조회
     */
    public List<DataCenterListResponse> getAccessibleDataCenters() {
        // === 1단계: 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Fetching accessible data centers for user: {} (role: {}, company: {})",
                currentMember.getId(), currentMember.getRole(), currentMember.getCompany().getId());
        // 로그: 사용자 ID, 권한, 회사 ID 기록

        List<DataCenter> dataCenters;

        // === 2단계: 권한별 조회 ===
        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN: 삭제되지 않은 모든 전산실 조회
            dataCenters = dataCenterRepository.findByDelYn(DelYN.N);
            log.info("Admin user - returning all {} data centers", dataCenters.size());
        } else {
            // OPERATOR, VIEWER: 자기 회사에 할당된 전산실만 조회
            dataCenters = dataCenterRepository.findAccessibleDataCentersByCompanyId(
                    currentMember.getCompany().getId());
            log.info("Non-admin user - returning {} accessible data centers", dataCenters.size());
        }

        // === 3단계: Entity → DTO 변환 ===
        return dataCenters.stream() // Stream API 사용
                .map(DataCenterListResponse::from) // 각 DataCenter를 DataCenterListResponse로 변환
                .collect(Collectors.toList()); // List로 수집
    }

    /**
     * 전산실 상세 조회
     * 권한 검증 후 전산실 정보 반환
     *
     * @param id 전산실 ID
     * @return 전산실 상세 정보
     */
    public DataCenterDetailResponse getDataCenterById(Long id) {
        // === 1단계: 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Fetching data center by id: {} for user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        // === 2단계: ID 유효성 검증 ===
        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // === 3단계: 접근 권한 확인 ===
        // ADMIN은 자동 통과, OPERATOR/VIEWER는 회사-전산실 매핑 확인
        validateDataCenterAccess(currentMember, id);

        // === 4단계: 전산실 조회 ===
        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));
        // findActiveById: delYn이 'N'인 전산실만 조회 (Soft Delete 적용)

        // === 5단계: Entity → DTO 변환 및 반환 ===
        return DataCenterDetailResponse.from(dataCenter);
    }

    /**
     * 전산실 생성
     * ADMIN, OPERATOR만 가능
     *
     * @param request 전산실 생성 요청 DTO
     * @return 생성된 전산실 정보
     */
    @Transactional // 쓰기 작업이므로 readOnly = false (기본값)
    // @Transactional은 메서드 실행 전에 트랜잭션을 시작하고, 정상 종료 시 commit, 예외 발생 시 rollback
    public DataCenterDetailResponse createDataCenter(DataCenterCreateRequest request) {
        // === 1단계: 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Creating data center with code: {} by user: {} (role: {})",
                request.code(), currentMember.getId(), currentMember.getRole());

        // === 2단계: 쓰기 권한 확인 ===
        // ADMIN, OPERATOR만 생성 가능
        validateWritePermission(currentMember);

        // === 3단계: 입력값 검증 ===
        // Bean Validation(@NotBlank 등)으로 검증되지만, 추가 비즈니스 검증 수행
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("전산실 이름을 입력해주세요.");
        }
        if (request.code() == null || request.code().trim().isEmpty()) {
            throw new IllegalArgumentException("전산실 코드를 입력해주세요.");
        }
        if (request.managerId() == null) {
            throw new IllegalArgumentException("담당자를 지정해주세요.");
        }

        // === 4단계: 코드 중복 체크 ===
        // 전산실 코드는 UNIQUE 제약조건이 있으므로 중복 확인 필요
        if (dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            throw new DuplicateException("전산실 코드", request.code());
        }

        // === 5단계: 담당자 조회 ===
        Member manager = memberRepository.findById(request.managerId())
                .orElseThrow(() -> new EntityNotFoundException("담당자", request.managerId()));
        // 담당자가 존재하지 않으면 예외 발생

        // === 6단계: Entity 생성 및 저장 ===
        DataCenter dataCenter = request.toEntity(manager, currentMember.getUserName());
        // DTO의 toEntity() 메서드로 Entity 생성
        // createdBy: 현재 로그인한 사용자의 userName

        DataCenter savedDataCenter = dataCenterRepository.save(dataCenter);
        // JpaRepository의 save() 메서드로 INSERT 쿼리 실행
        // save()는 영속성 컨텍스트에 엔티티를 저장하고, 트랜잭션 커밋 시 실제 DB에 반영

        log.info("Data center created successfully with id: {}", savedDataCenter.getId());

        // === 7단계: Entity → DTO 변환 및 반환 ===
        return DataCenterDetailResponse.from(savedDataCenter);
    }

    /**
     * 전산실 정보 수정
     * ADMIN, OPERATOR만 가능
     *
     * @param id 전산실 ID
     * @param request 전산실 수정 요청 DTO
     * @return 수정된 전산실 정보
     */
    @Transactional // 쓰기 작업이므로 readOnly = false
    public DataCenterDetailResponse updateDataCenter(Long id, DataCenterUpdateRequest request) {
        // === 1단계: 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Updating data center with id: {} by user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        // === 2단계: ID 유효성 검증 ===
        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // === 3단계: 쓰기 권한 확인 ===
        validateWritePermission(currentMember);

        // === 4단계: 접근 권한 확인 ===
        validateDataCenterAccess(currentMember, id);

        // === 5단계: 전산실 조회 ===
        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        // === 6단계: 코드 변경 시 중복 체크 ===
        if (request.code() != null
                && !request.code().trim().isEmpty()
                && !request.code().equals(dataCenter.getCode())) {
            // 코드가 변경되고, 기존 코드와 다른 경우 중복 확인
            if (dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
                throw new DuplicateException("전산실 코드", request.code());
            }
        }

        // === 7단계: 담당자 변경 ===
        Member manager = null;
        if (request.managerId() != null) {
            // 담당자 ID가 입력된 경우에만 조회
            manager = memberRepository.findById(request.managerId())
                    .orElseThrow(() -> new EntityNotFoundException("담당자", request.managerId()));
        }

        // === 8단계: Entity 정보 업데이트 ===
        dataCenter.updateInfo(
                request.name(),
                request.code(),
                request.location(),
                request.floor(),
                request.rows(),
                request.columns(),
                request.backgroundImageUrl(),
                request.status(),
                request.description(),
                request.totalArea(),
                request.totalPowerCapacity(),
                request.totalCoolingCapacity(),
                request.maxRackCount(),
                request.temperatureMin(),
                request.temperatureMax(),
                request.humidityMin(),
                request.humidityMax(),
                manager
        );
        // Entity의 updateInfo() 메서드로 부분 수정
        // null이 아닌 값만 업데이트됨
        // JPA의 Dirty Checking으로 트랜잭션 커밋 시 자동으로 UPDATE 쿼리 실행

        log.info("Data center updated successfully with id: {}", id);

        // === 9단계: Entity → DTO 변환 및 반환 ===
        return DataCenterDetailResponse.from(dataCenter);
    }

    /**
     * 전산실 삭제 (Soft Delete)
     * ADMIN, OPERATOR만 가능
     * 실제 DB에서 삭제하지 않고 delYn만 'Y'로 변경
     *
     * @param id 전산실 ID
     */
    @Transactional // 쓰기 작업이므로 readOnly = false
    public void deleteDataCenter(Long id) {
        // === 1단계: 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Deleting data center with id: {} by user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        // === 2단계: ID 유효성 검증 ===
        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // === 3단계: 쓰기 권한 확인 ===
        validateWritePermission(currentMember);

        // === 4단계: 접근 권한 확인 ===
        validateDataCenterAccess(currentMember, id);

        // === 5단계: 전산실 조회 ===
        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        // === 6단계: Soft Delete 실행 ===
        dataCenter.softDelete();
        // BaseTimeEntity의 softDelete() 메서드 호출
        // delYn을 'Y'로 변경하고, deletedAt에 현재 시간 설정
        // JPA Dirty Checking으로 UPDATE 쿼리 자동 실행

        log.info("Data center soft deleted successfully with id: {}", id);
    }

    /**
     * 전산실 이름으로 검색
     * ADMIN: 모든 전산실에서 검색
     * OPERATOR/VIEWER: 자기 회사에 할당된 전산실에서만 검색
     *
     * @param name 검색어
     * @return 검색 결과 목록
     */
    public List<DataCenterListResponse> searchDataCentersByName(String name) {
        // === 1단계: 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Searching data centers by name: {} for user: {} (role: {})",
                name, currentMember.getId(), currentMember.getRole());

        // === 2단계: 검색어 유효성 검증 ===
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<DataCenter> searchResults;

        // === 3단계: 권한별 검색 ===
        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN: 모든 전산실에서 검색
            searchResults = dataCenterRepository.searchByName(name);
            log.info("Admin user - searched all data centers, found: {}", searchResults.size());
        } else {
            // OPERATOR, VIEWER: 자기 회사에 할당된 전산실에서만 검색
            // 1) 먼저 접근 가능한 전산실 목록 조회
            List<DataCenter> accessibleDataCenters = dataCenterRepository
                    .findAccessibleDataCentersByCompanyId(currentMember.getCompany().getId());

            // 2) 접근 가능한 전산실 중에서 이름으로 필터링
            searchResults = accessibleDataCenters.stream()
                    .filter(dc -> dc.getName().contains(name)) // 전산실 이름에 검색어 포함 여부 확인
                    .collect(Collectors.toList());
            log.info("Non-admin user - searched accessible data centers, found: {}", searchResults.size());
        }

        // === 4단계: Entity → DTO 변환 및 반환 ===
        return searchResults.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }
}