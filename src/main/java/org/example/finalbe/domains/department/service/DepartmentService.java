package org.example.finalbe.domains.department.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.department.domain.Department;
import org.example.finalbe.domains.department.domain.MemberDepartment;
import org.example.finalbe.domains.department.domain.RackDepartment;
import org.example.finalbe.domains.department.dto.*;
import org.example.finalbe.domains.department.repository.DepartmentRepository;
import org.example.finalbe.domains.department.repository.MemberDepartmentRepository;
import org.example.finalbe.domains.department.repository.RackDepartmentRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.dto.RackListResponse;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Department 비즈니스 로직 Service (서비스 계층)
 *
 * - 부서(Department) 관련 비즈니스 로직 처리
 * - 회원-부서(MemberDepartment) 매핑 관리
 * - 랙-부서(RackDepartment) 매핑 관리
 * - 트랜잭션 관리 및 권한 검증
 *
 * Service 설계 특징:
 * 1. @Transactional(readOnly = true): 기본 읽기 전용 (조회 성능 최적화)
 * 2. CUD 메서드만 @Transactional: 쓰기 트랜잭션 명시적 선언
 * 3. 권한 검증 로직 통합 (getCurrentMember, validateWritePermission)
 * 4. 비즈니스 규칙 검증 (중복 체크, 삭제 가능 여부 등)
 */
@Slf4j // 로깅 기능 자동 생성 (log.info(), log.debug() 등 사용 가능)
@Service // Spring Bean으로 등록 (Component Scan 대상)
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Transactional(readOnly = true) // 클래스 레벨 트랜잭션 설정 (읽기 전용)
// readOnly = true: SELECT 쿼리만 실행, Dirty Checking 비활성화 (성능 최적화)
// CUD 메서드는 @Transactional로 재정의하여 쓰기 트랜잭션 활성화
public class DepartmentService {

    // === Repository 의존성 주입 ===
    private final DepartmentRepository departmentRepository; // 부서 저장소
    private final MemberDepartmentRepository memberDepartmentRepository; // 회원-부서 매핑 저장소
    private final RackDepartmentRepository rackDepartmentRepository; // 랙-부서 매핑 저장소
    private final CompanyRepository companyRepository; // 회사 저장소 (부서 생성 시 필요)
    private final MemberRepository memberRepository; // 회원 저장소 (매핑 생성 시 필요)
    private final RackRepository rackRepository; // 랙 저장소 (매핑 생성 시 필요)
    // @RequiredArgsConstructor로 생성자 자동 생성 및 의존성 주입

    /**
     * 현재 로그인한 사용자 조회 (공통 메서드)
     *
     * - Spring Security의 SecurityContext에서 인증 정보 추출
     * - Authentication의 getName()으로 사용자 ID 획득
     * - MemberRepository에서 Member 엔티티 조회
     *
     * @return Member - 현재 로그인한 회원 엔티티
     * @throws IllegalStateException 인증되지 않은 사용자이거나 ID가 없는 경우
     * @throws EntityNotFoundException 사용자 ID로 회원을 찾을 수 없는 경우
     * @throws IllegalArgumentException 사용자 ID가 숫자가 아닌 경우
     *
     * SecurityContext 동작 방식:
     * 1. JWT 필터에서 토큰 검증 후 Authentication 객체 생성
     * 2. SecurityContextHolder에 저장
     * 3. Service에서 getContext().getAuthentication()으로 조회
     */
    private Member getCurrentMember() {
        // === SecurityContext에서 인증 정보 조회 ===
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // SecurityContextHolder: ThreadLocal 기반으로 현재 스레드의 인증 정보 저장
        // getContext(): 현재 스레드의 SecurityContext 조회
        // getAuthentication(): 인증된 사용자 정보 조회

        // === 인증 여부 확인 ===
        if (authentication == null || !authentication.isAuthenticated()) {
            // authentication == null: JWT 필터를 거치지 않았거나 인증 실패
            // !isAuthenticated(): 익명 사용자 (anonymous)
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        // === 사용자 ID 추출 ===
        String userId = authentication.getName();
        // getName(): JWT 토큰에서 추출한 subject (보통 사용자 ID)
        // 예: JWT payload의 sub 필드 → "123"
        if (userId == null || userId.trim().isEmpty()) {
            // 사용자 ID가 비어있는 경우 (토큰 생성 오류)
            throw new IllegalStateException("사용자 ID를 찾을 수 없습니다.");
        }

        // === Member 엔티티 조회 ===
        try {
            return memberRepository.findById(Long.parseLong(userId))
                    // String userId를 Long으로 변환 후 조회
                    // 예: "123" → 123L
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
            // Optional이 empty이면 EntityNotFoundException 발생
        } catch (NumberFormatException e) {
            // userId가 숫자가 아닌 경우 (예: "abc")
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
        // 조회 성공 시 Member 엔티티 반환
        // 실패 시 예외 발생 (컨트롤러의 GlobalExceptionHandler에서 처리)
    }

    /**
     * 쓰기 권한 확인 (공통 메서드)
     *
     * - ADMIN 또는 OPERATOR만 CUD 작업 가능
     * - VIEWER는 조회만 가능
     *
     * @param member 권한 확인할 회원 엔티티
     * @throws AccessDeniedException ADMIN/OPERATOR가 아닌 경우
     *
     * Role 종류:
     * - ADMIN: 모든 권한 (시스템 관리자)
     * - OPERATOR: 데이터 수정 권한 (운영자)
     * - VIEWER: 조회만 가능 (일반 사용자)
     */
    private void validateWritePermission(Member member) {
        // === 권한 확인 ===
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            // Role이 ADMIN도 아니고 OPERATOR도 아니면 (즉, VIEWER)
            throw new AccessDeniedException("관리자 또는 운영자만 수정할 수 있습니다.");
            // AccessDeniedException: Spring Security 예외 (HTTP 403 Forbidden)
        }
        // ADMIN 또는 OPERATOR면 통과 (아무 동작 안 함)
    }

    /**
     * 회사별 부서 목록 조회
     *
     * - 특정 회사에 속한 활성 부서 목록 조회
     * - ADMIN: 모든 회사의 부서 조회 가능
     * - ADMIN 아님: 자신의 회사 부서만 조회 가능
     * - 삭제된(delYn = 'Y') 부서는 제외
     *
     * @param companyId 회사 ID
     * @return List<DepartmentListResponse> - 부서 목록 (간소화된 DTO)
     * @throws AccessDeniedException 다른 회사의 부서를 조회하려는 경우
     *
     * 권한 검증 로직:
     * - ADMIN: 모든 회사 접근 가능
     * - 일반 사용자: 자신의 회사만 접근 가능
     */
    public List<DepartmentListResponse> getDepartmentsByCompany(Long companyId) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        // SecurityContext에서 인증된 사용자 정보 추출
        log.debug("Fetching departments for company: {}", companyId);
        // 디버그 로그 출력 (개발 환경에서만 활성화)

        // === 권한 검증 (회사별 접근 제어) ===
        if (currentMember.getRole() != Role.ADMIN && !currentMember.getCompany().getId().equals(companyId)) {
            // ADMIN이 아니면서 + 다른 회사의 부서를 조회하려는 경우
            // currentMember.getCompany().getId(): 사용자의 소속 회사 ID
            // companyId: 조회하려는 회사 ID
            throw new AccessDeniedException("해당 회사의 부서 목록을 조회할 권한이 없습니다.");
        }
        // ADMIN이거나 자신의 회사면 통과

        // === 부서 목록 조회 (Repository) ===
        List<Department> departments = departmentRepository.findByCompanyIdAndDelYn(companyId, DelYN.N);
        // findByCompanyIdAndDelYn: company_id = ? AND del_yn = 'N' 조건으로 조회
        // 삭제되지 않은 부서만 조회 (Soft Delete)

        // === Entity → DTO 변환 (Stream API) ===
        return departments.stream() // List를 Stream으로 변환
                .map(DepartmentListResponse::from) // 각 Department를 DepartmentListResponse로 변환
                // from(): DTO의 정적 팩토리 메서드 호출
                .collect(Collectors.toList()); // Stream을 List로 수집
        // 결과: List<DepartmentListResponse> 반환
    }

    /**
     * 부서 상세 조회
     *
     * - ID로 부서 단건 조회
     * - 삭제된(delYn = 'Y') 부서는 조회 불가
     * - 모든 정보 포함 (DetailResponse)
     *
     * @param id 부서 ID
     * @return DepartmentDetailResponse - 부서 상세 정보 DTO
     * @throws EntityNotFoundException 부서를 찾을 수 없는 경우
     *
     * 상세 조회 vs 목록 조회:
     * - 상세: 모든 필드 포함 (phone, email, createdBy 등)
     * - 목록: 핵심 필드만 포함 (성능 최적화)
     */
    public DepartmentDetailResponse getDepartmentById(Long id) {
        log.debug("Fetching department details for id: {}", id);
        // 디버그 로그 출력

        // === 부서 조회 (Repository) ===
        Department department = departmentRepository.findActiveById(id)
                // findActiveById: id = ? AND del_yn = 'N' 조건으로 조회
                .orElseThrow(() -> new EntityNotFoundException("부서", id));
        // Optional이 empty이면 예외 발생
        // EntityNotFoundException: 커스텀 예외 (HTTP 404 Not Found)

        // === Entity → DTO 변환 ===
        return DepartmentDetailResponse.from(department);
        // from(): DTO의 정적 팩토리 메서드 호출
        // 모든 필드를 DTO에 매핑 (상세 조회)
    }

    /**
     * 부서 생성
     *
     * - 새로운 부서 생성
     * - ADMIN/OPERATOR만 가능 (권한 검증)
     * - 부서 코드 중복 체크 (같은 회사 내)
     * - 생성자 정보 자동 기록 (createdBy)
     *
     * @param request 부서 생성 요청 DTO
     * @return DepartmentDetailResponse - 생성된 부서 상세 정보
     * @throws AccessDeniedException 권한이 없는 경우
     * @throws EntityNotFoundException 회사를 찾을 수 없는 경우
     * @throws DuplicateException 부서 코드가 중복되는 경우
     *
     * 트랜잭션 처리:
     * - @Transactional: 쓰기 트랜잭션 활성화
     * - save() 호출 시 INSERT 쿼리 실행
     * - 메서드 정상 종료 시 commit, 예외 발생 시 rollback
     */
    @Transactional // 쓰기 트랜잭션 활성화 (readOnly = false)
    // CUD 작업이므로 클래스 레벨 readOnly = true를 재정의
    public DepartmentDetailResponse createDepartment(DepartmentCreateRequest request) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Creating new department: {} by user: {}", request.departmentName(), currentMember.getId());
        // 정보 로그 출력 (운영 환경에서도 활성화)

        // === 권한 검증 ===
        validateWritePermission(currentMember);
        // ADMIN 또는 OPERATOR가 아니면 AccessDeniedException 발생

        // === 회사 조회 (외래키 참조 무결성) ===
        Company company = companyRepository.findActiveById(request.companyId())
                // findActiveById: company_id = ? AND del_yn = 'N' 조건으로 조회
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));
        // 회사가 없거나 삭제된 경우 예외 발생

        // === 부서 코드 중복 체크 (비즈니스 규칙 검증) ===
        if (departmentRepository.existsByCompanyIdAndDepartmentCodeAndDelYn(
                request.companyId(), request.departmentCode(), DelYN.N)) {
            // 같은 회사 내에서 동일한 부서 코드가 이미 존재하는지 확인
            // 예: 회사A의 "DEV" 코드가 이미 있으면 중복
            throw new DuplicateException("부서 코드", request.departmentCode());
            // DuplicateException: 커스텀 예외 (HTTP 409 Conflict)
        }

        // === 부서 생성 (DTO → Entity 변환) ===
        Department department = request.toEntity(company, currentMember.getUserName());
        // toEntity(): DTO의 변환 메서드 호출
        // company: 조회한 Company 엔티티 전달
        // currentMember.getUserName(): 생성자 정보 전달

        // === 부서 저장 (Repository) ===
        Department savedDepartment = departmentRepository.save(department);
        // save(): JPA의 persist() 호출 → INSERT 쿼리 실행
        // INSERT INTO department (...) VALUES (...)
        // savedDepartment: id가 자동 생성된 엔티티 (GeneratedValue)

        log.info("Department created successfully with id: {}", savedDepartment.getId());
        // 생성된 부서의 ID 로그 출력

        // === Entity → DTO 변환 후 반환 ===
        return DepartmentDetailResponse.from(savedDepartment);
        // 생성된 부서 정보를 DTO로 변환하여 Controller에 반환
    }

    /**
     * 부서 수정
     *
     * - 기존 부서 정보 수정
     * - ADMIN/OPERATOR만 가능 (권한 검증)
     * - null이 아닌 필드만 업데이트 (부분 수정)
     * - 수정자 정보 자동 기록 (updatedBy)
     *
     * @param id 수정할 부서 ID
     * @param request 부서 수정 요청 DTO
     * @return DepartmentDetailResponse - 수정된 부서 상세 정보
     * @throws AccessDeniedException 권한이 없는 경우
     * @throws EntityNotFoundException 부서를 찾을 수 없는 경우
     *
     * JPA Dirty Checking:
     * - Entity의 필드 변경만으로 UPDATE 쿼리 자동 생성
     * - 트랜잭션 커밋 시점에 변경 감지 (Dirty Checking)
     * - 변경된 필드만 UPDATE (전체 필드 아님)
     */
    @Transactional // 쓰기 트랜잭션 활성화
    public DepartmentDetailResponse updateDepartment(Long id, DepartmentUpdateRequest request) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Updating department with id: {} by user: {}", id, currentMember.getId());

        // === 권한 검증 ===
        validateWritePermission(currentMember);
        // ADMIN 또는 OPERATOR가 아니면 예외 발생

        // === 부서 조회 ===
        Department department = departmentRepository.findActiveById(id)
                // 삭제되지 않은 부서만 조회
                .orElseThrow(() -> new EntityNotFoundException("부서", id));

        // === 부서 정보 업데이트 (Entity 메서드 호출) ===
        department.updateInfo(
                request.departmentName(), // 부서명 (null이면 변경 안 함)
                request.description(), // 설명 (null이면 변경 안 함)
                request.location(), // 위치 (null이면 변경 안 함)
                request.phone(), // 전화번호 (null이면 변경 안 함)
                request.email(), // 이메일 (null이면 변경 안 함)
                currentMember.getUserName() // 수정자 정보 (항상 업데이트)
        );
        // updateInfo(): Entity의 비즈니스 메서드
        // null이 아닌 필드만 변경 (부분 수정 지원)
        // JPA Dirty Checking: 트랜잭션 커밋 시 변경 감지
        // UPDATE department SET department_name = ?, updated_by = ?, updated_at = ? WHERE department_id = ?

        log.info("Department updated successfully for id: {}", id);

        // === Entity → DTO 변환 후 반환 ===
        return DepartmentDetailResponse.from(department);
        // 수정된 부서 정보를 DTO로 변환
        // 트랜잭션 커밋 전에 영속성 컨텍스트의 최신 상태 반영
    }

    /**
     * 부서 삭제 (소프트 삭제)
     *
     * - 부서를 논리적으로 삭제 (delYn = 'Y')
     * - ADMIN만 가능 (엄격한 권한 제어)
     * - 소속 회원이 있으면 삭제 불가 (비즈니스 규칙)
     * - 담당 랙이 있으면 삭제 불가 (비즈니스 규칙)
     *
     * @param id 삭제할 부서 ID
     * @throws AccessDeniedException ADMIN이 아닌 경우
     * @throws EntityNotFoundException 부서를 찾을 수 없는 경우
     * @throws BusinessException 소속 회원 또는 담당 랙이 있는 경우
     *
     * Soft Delete vs Hard Delete:
     * - Soft Delete: delYn = 'Y'로 변경 (데이터 보존)
     * - Hard Delete: DELETE 쿼리 실행 (데이터 물리 삭제)
     * - 장점: 히스토리 보존, 복구 가능, 참조 무결성 유지
     */
    @Transactional // 쓰기 트랜잭션 활성화
    public void deleteDepartment(Long id) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Deleting department with id: {} by user: {}", id, currentMember.getId());

        // === 권한 검증 (ADMIN만 가능) ===
        if (currentMember.getRole() != Role.ADMIN) {
            // 삭제는 ADMIN만 가능 (OPERATOR도 불가)
            throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");
        }

        // === 부서 조회 ===
        Department department = departmentRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("부서", id));

        // === 소속 회원 확인 (비즈니스 규칙 검증) ===
        Long memberCount = memberDepartmentRepository.countByDepartmentId(id);
        // countByDepartmentId: 해당 부서에 속한 회원 수 조회
        if (memberCount > 0) {
            // 소속 회원이 있으면 삭제 불가
            throw new BusinessException("부서에 소속된 회원이 있어 삭제할 수 없습니다.");
            // BusinessException: 커스텀 예외 (HTTP 400 Bad Request)
        }

        // === 담당 랙 확인 (비즈니스 규칙 검증) ===
        Long rackCount = rackDepartmentRepository.countByDepartmentId(id);
        // countByDepartmentId: 해당 부서가 담당하는 랙 수 조회
        if (rackCount > 0) {
            // 담당 랙이 있으면 삭제 불가
            throw new BusinessException("부서가 담당하는 랙이 있어 삭제할 수 없습니다.");
        }

        // === 소프트 삭제 실행 ===
        department.softDelete();
        // softDelete(): Entity의 비즈니스 메서드
        // delYn을 'Y'로 변경
        // JPA Dirty Checking: UPDATE department SET del_yn = 'Y' WHERE department_id = ?
        // 물리 삭제(DELETE)가 아닌 논리 삭제(UPDATE)

        log.info("Department deleted successfully for id: {}", id);
        // 삭제된 부서는 findByCompanyIdAndDelYn(companyId, DelYN.N)으로 조회 불가
    }

    /**
     * 부서 검색 (같은 회사 내)
     *
     * - 부서명에 키워드가 포함된 부서 검색
     * - ADMIN: 모든 회사에서 검색 가능
     * - ADMIN 아님: 자신의 회사 내에서만 검색
     * - LIKE 검색 (부분 일치)
     *
     * @param companyId 회사 ID
     * @param keyword 검색 키워드
     * @return List<DepartmentListResponse> - 검색된 부서 목록
     * @throws AccessDeniedException 다른 회사에서 검색하려는 경우
     *
     * 검색 예시:
     * - keyword = "개발" → "개발팀", "백엔드 개발팀", "개발 지원팀" 매칭
     */
    public List<DepartmentListResponse> searchDepartments(Long companyId, String keyword) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.debug("Searching departments with keyword: {}", keyword);

        // === 권한 검증 (회사별 접근 제어) ===
        if (currentMember.getRole() != Role.ADMIN && !currentMember.getCompany().getId().equals(companyId)) {
            // ADMIN이 아니면서 다른 회사에서 검색하려는 경우
            throw new AccessDeniedException("해당 회사의 부서를 검색할 권한이 없습니다.");
        }

        // === 부서 검색 (Repository) ===
        List<Department> departments = departmentRepository.searchByNameInCompany(companyId, keyword);
        // searchByNameInCompany: LIKE 검색 (부분 일치)
        // WHERE d.company_id = ? AND d.department_name LIKE %keyword% AND d.del_yn = 'N'

        // === Entity → DTO 변환 ===
        return departments.stream()
                .map(DepartmentListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원-부서 매핑 생성 (회원을 부서에 배치)
     *
     * - 회원을 특정 부서에 배치
     * - ADMIN/OPERATOR만 가능
     * - 중복 배치 방지
     * - 주 부서 설정 시 기존 주 부서 해제
     * - 부서 직원 수 자동 증가
     *
     * @param request 회원-부서 매핑 생성 요청 DTO
     * @throws AccessDeniedException 권한이 없는 경우
     * @throws EntityNotFoundException 회원 또는 부서를 찾을 수 없는 경우
     * @throws BusinessException 이미 배치된 경우
     *
     * 주 부서 처리 로직:
     * 1. isPrimary = true로 요청하면
     * 2. 기존 주 부서를 찾아서 isPrimary = false로 변경
     * 3. 새 매핑을 isPrimary = true로 생성
     */
    @Transactional // 쓰기 트랜잭션 활성화
    public void addMemberToDepartment(MemberDepartmentCreateRequest request) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Adding member {} to department {}", request.memberId(), request.departmentId());

        // === 권한 검증 ===
        validateWritePermission(currentMember);

        // === 회원 조회 (외래키 참조 무결성) ===
        Member member = memberRepository.findActiveById(request.memberId())
                // findActiveById: member_id = ? AND del_yn = 'N' 조건으로 조회
                .orElseThrow(() -> new EntityNotFoundException("회원", request.memberId()));

        // === 부서 조회 (외래키 참조 무결성) ===
        Department department = departmentRepository.findActiveById(request.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("부서", request.departmentId()));

        // === 중복 체크 (비즈니스 규칙 검증) ===
        if (memberDepartmentRepository.existsByMemberIdAndDepartmentId(request.memberId(), request.departmentId())) {
            // 이미 동일한 매핑이 존재하는지 확인 (DB Unique 제약 + 어플리케이션 검증)
            throw new BusinessException("이미 해당 부서에 소속된 회원입니다.");
        }

        // === 주 부서 설정 처리 ===
        if (Boolean.TRUE.equals(request.isPrimary())) {
            // isPrimary가 true로 요청된 경우
            memberDepartmentRepository.findByMemberIdAndIsPrimary(request.memberId(), true)
                    // 기존 주 부서 조회 (회원의 isPrimary = true인 매핑)
                    .ifPresent(MemberDepartment::setSecondaryDepartment);
            // 존재하면 setSecondaryDepartment() 호출 (isPrimary = false로 변경)
            // JPA Dirty Checking: UPDATE member_department SET is_primary = false WHERE ...
        }
        // 주 부서는 1개만 가능하므로 기존 주 부서를 부 부서로 변경

        // === 매핑 생성 ===
        MemberDepartment memberDepartment = MemberDepartment.builder()
                .member(member) // 조회한 Member 엔티티 설정
                .department(department) // 조회한 Department 엔티티 설정
                .isPrimary(Boolean.TRUE.equals(request.isPrimary())) // 주 부서 여부 (null이면 false)
                .position(request.position()) // 직급 (null 가능)
                .joinDate(request.joinDate() != null ? request.joinDate() : LocalDate.now()) // 배치일 (null이면 현재 날짜)
                .createdBy(currentMember.getUserName()) // 생성자 정보
                .build(); // MemberDepartment 엔티티 생성

        // === 매핑 저장 ===
        memberDepartmentRepository.save(memberDepartment);
        // INSERT INTO member_department (...) VALUES (...)

        // === 부서 직원 수 증가 ===
        department.incrementEmployeeCount();
        // employeeCount++ (Entity 메서드)
        // JPA Dirty Checking: UPDATE department SET employee_count = employee_count + 1 WHERE department_id = ?

        log.info("Member {} added to department {} successfully", request.memberId(), request.departmentId());
    }

    /**
     * 회원-부서 매핑 삭제 (회원을 부서에서 제거)
     *
     * - 회원을 특정 부서에서 제거
     * - ADMIN/OPERATOR만 가능
     * - 부서 직원 수 자동 감소
     *
     * @param memberId 회원 ID
     * @param departmentId 부서 ID
     * @throws AccessDeniedException 권한이 없는 경우
     * @throws EntityNotFoundException 매핑을 찾을 수 없는 경우
     */
    @Transactional // 쓰기 트랜잭션 활성화
    public void removeMemberFromDepartment(Long memberId, Long departmentId) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Removing member {} from department {}", memberId, departmentId);

        // === 권한 검증 ===
        validateWritePermission(currentMember);

        // === 매핑 조회 ===
        MemberDepartment memberDepartment = memberDepartmentRepository.findByMemberIdAndDepartmentId(memberId, departmentId)
                // 회원 ID와 부서 ID로 매핑 조회
                .orElseThrow(() -> new EntityNotFoundException("회원-부서 매핑을 찾을 수 없습니다."));

        // === 부서 조회 (직원 수 감소를 위해) ===
        Department department = memberDepartment.getDepartment();
        // LAZY 로딩: 이 시점에 SELECT * FROM department WHERE department_id = ? 실행

        // === 매핑 삭제 ===
        memberDepartmentRepository.delete(memberDepartment);
        // DELETE FROM member_department WHERE member_department_id = ?
        // 물리 삭제 (Hard Delete)

        // === 부서 직원 수 감소 ===
        department.decrementEmployeeCount();
        // employeeCount-- (Entity 메서드, 0 미만으로 내려가지 않음)
        // JPA Dirty Checking: UPDATE department SET employee_count = employee_count - 1 WHERE department_id = ?

        log.info("Member {} removed from department {} successfully", memberId, departmentId);
    }

    /**
     * 랙-부서 매핑 생성 (랙을 부서에 배정)
     *
     * - 랙을 특정 부서에 배정
     * - ADMIN/OPERATOR만 가능
     * - 중복 배정 방지
     * - 주 담당 부서 설정 시 기존 주 담당 해제
     *
     * @param request 랙-부서 매핑 생성 요청 DTO
     * @throws AccessDeniedException 권한이 없는 경우
     * @throws EntityNotFoundException 랙 또는 부서를 찾을 수 없는 경우
     * @throws BusinessException 이미 배정된 경우
     */
    @Transactional // 쓰기 트랜잭션 활성화
    public void addRackToDepartment(RackDepartmentCreateRequest request) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Adding rack {} to department {}", request.rackId(), request.departmentId());

        // === 권한 검증 ===
        validateWritePermission(currentMember);

        // === 랙 조회 (외래키 참조 무결성) ===
        Rack rack = rackRepository.findActiveById(request.rackId())
                // findActiveById: rack_id = ? AND del_yn = 'N' 조건으로 조회
                .orElseThrow(() -> new EntityNotFoundException("랙", request.rackId()));

        // === 부서 조회 (외래키 참조 무결성) ===
        Department department = departmentRepository.findActiveById(request.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("부서", request.departmentId()));

        // === 중복 체크 (비즈니스 규칙 검증) ===
        if (rackDepartmentRepository.existsByRackIdAndDepartmentId(request.rackId(), request.departmentId())) {
            // 이미 동일한 매핑이 존재하는지 확인
            throw new BusinessException("이미 해당 부서가 담당하는 랙입니다.");
        }

        // === 주 담당 부서 설정 처리 ===
        if (Boolean.TRUE.equals(request.isPrimary())) {
            // isPrimary가 true로 요청된 경우
            rackDepartmentRepository.findByRackIdAndIsPrimary(request.rackId(), true)
                    // 기존 주 담당 부서 조회 (랙의 isPrimary = true인 매핑)
                    .ifPresent(RackDepartment::setSecondaryDepartment);
            // 존재하면 setSecondaryDepartment() 호출 (isPrimary = false로 변경)
            // JPA Dirty Checking: UPDATE rack_department SET is_primary = false WHERE ...
        }
        // 주 담당 부서는 1개만 가능하므로 기존 주 담당을 부 담당으로 변경

        // === 매핑 생성 ===
        RackDepartment rackDepartment = RackDepartment.builder()
                .rack(rack) // 조회한 Rack 엔티티 설정
                .department(department) // 조회한 Department 엔티티 설정
                .isPrimary(Boolean.TRUE.equals(request.isPrimary())) // 주 담당 여부 (null이면 false)
                .responsibility(request.responsibility()) // 책임 범위 (null 가능)
                .assignedDate(request.assignedDate() != null ? request.assignedDate() : LocalDate.now()) // 배정일 (null이면 현재 날짜)
                .createdBy(currentMember.getUserName()) // 생성자 정보
                .build(); // RackDepartment 엔티티 생성

        // === 매핑 저장 ===
        rackDepartmentRepository.save(rackDepartment);
        // INSERT INTO rack_department (...) VALUES (...)

        log.info("Rack {} added to department {} successfully", request.rackId(), request.departmentId());
    }

    /**
     * 랙-부서 매핑 삭제 (랙의 부서 배정 해제)
     *
     * - 랙을 특정 부서에서 배정 해제
     * - ADMIN/OPERATOR만 가능
     *
     * @param rackId 랙 ID
     * @param departmentId 부서 ID
     * @throws AccessDeniedException 권한이 없는 경우
     * @throws EntityNotFoundException 매핑을 찾을 수 없는 경우
     */
    @Transactional // 쓰기 트랜잭션 활성화
    public void removeRackFromDepartment(Long rackId, Long departmentId) {
        // === 현재 사용자 조회 ===
        Member currentMember = getCurrentMember();
        log.info("Removing rack {} from department {}", rackId, departmentId);

        // === 권한 검증 ===
        validateWritePermission(currentMember);

        // === 매핑 조회 ===
        RackDepartment rackDepartment = rackDepartmentRepository.findByRackIdAndDepartmentId(rackId, departmentId)
                // 랙 ID와 부서 ID로 매핑 조회
                .orElseThrow(() -> new EntityNotFoundException("랙-부서 매핑을 찾을 수 없습니다."));

        // === 매핑 삭제 ===
        rackDepartmentRepository.delete(rackDepartment);
        // DELETE FROM rack_department WHERE rack_department_id = ?
        // 물리 삭제 (Hard Delete)

        log.info("Rack {} removed from department {} successfully", rackId, departmentId);
    }

    /**
     * 부서별 랙 목록 조회
     *
     * - 특정 부서가 담당하는 모든 랙 조회
     * - 삭제된 랙은 제외
     * - 랙 이름 오름차순 정렬
     *
     * @param departmentId 부서 ID
     * @return List<RackListResponse> - 랙 목록 (간소화된 DTO)
     */
    public List<RackListResponse> getRacksByDepartment(Long departmentId) {
        log.debug("Fetching racks for department: {}", departmentId);

        // === 부서별 랙 조회 (Repository) ===
        List<Rack> racks = rackDepartmentRepository.findRacksByDepartmentId(departmentId);
        // findRacksByDepartmentId: 중간 테이블을 통해 Rack 엔티티 조회
        // SELECT r.* FROM rack r
        // JOIN rack_department rd ON r.rack_id = rd.rack_id
        // WHERE rd.department_id = ? AND r.del_yn = 'N'
        // ORDER BY r.rack_name ASC

        // === Entity → DTO 변환 ===
        return racks.stream()
                .map(RackListResponse::from) // Rack → RackListResponse 변환
                .collect(Collectors.toList());
    }
}