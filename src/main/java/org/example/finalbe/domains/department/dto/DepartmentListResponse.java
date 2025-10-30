package org.example.finalbe.domains.department.dto;

import lombok.Builder;
import org.example.finalbe.domains.department.domain.Department;

import java.time.LocalDateTime;

/**
 * 부서 목록 조회 응답 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 응답 데이터 전달
 * - Entity → DTO 변환: from() 정적 팩토리 메서드 제공
 * - 필수 정보만 포함 (목록 조회용)
 * - DetailResponse보다 간소화된 응답
 *
 * List DTO를 별도로 만드는 이유:
 * 1. 목록 조회 시 불필요한 정보 제외 (응답 크기 최적화)
 * 2. 페이징 처리 시 성능 향상 (적은 데이터 전송)
 * 3. 네트워크 트래픽 감소
 * 4. 프론트엔드에서 필요한 최소한의 정보만 제공
 *
 * DetailResponse와의 차이점:
 * - Detail: 모든 정보 포함 (phone, email, createdBy, updatedBy 등)
 * - List: 핵심 정보만 포함 (code, name, location, count 등)
 */
@Builder // 빌더 패턴 지원
public record DepartmentListResponse(
        // === 식별자 ===
        Long id, // 부서 고유 식별자 (예: 1, 2, 3...)

        // === 부서 핵심 정보 ===
        String departmentCode, // 부서 코드 (예: DEV, HR, SALES)
        String departmentName, // 부서명 (예: 개발팀, 인사팀, 영업팀)
        String description, // 부서 설명 (예: 서버 개발 및 유지보수 담당)
        // 목록에서도 간단한 설명은 표시 (사용자 편의성)

        // === 부서 위치 ===
        String location, // 부서 위치 (예: 본사 3층, 서울 지사 2층)
        // 목록에서 위치 정보는 유용 (빠른 확인)

        // === 부서 통계 ===
        Integer employeeCount, // 소속 직원 수 (예: 5, 10, 15...)
        // 목록에서 직원 수는 중요 (부서 규모 파악)

        // === 연관된 회사 정보 ===
        String companyName, // 소속 회사명 (예: 테크놀로지 주식회사)
        // 회사명만 포함 (companyId는 제외)
        // 목록 조회 시 어느 회사의 부서인지 표시

        // === 메타 정보 ===
        LocalDateTime createdAt // 생성 시간 (BaseTimeEntity에서 자동 관리)
        // 생성 시간만 포함 (updatedAt은 제외)
        // 최신 부서 순으로 정렬할 때 유용

        // === List DTO에서 제외된 필드 ===
        // - phone: 목록에서는 불필요 (상세 조회에서 확인)
        // - email: 목록에서는 불필요 (상세 조회에서 확인)
        // - companyId: 목록에서는 회사명만 표시
        // - createdBy/updatedBy: 목록에서는 불필요 (관리자용 정보)
        // - updatedAt: 목록에서는 생성 시간만 표시
) {
    /**
     * Entity → DTO 변환 (정적 팩토리 메서드)
     *
     * - Department 엔티티를 받아서 DepartmentListResponse DTO로 변환
     * - 목록 조회에 필요한 핵심 정보만 추출
     * - 불필요한 필드는 제외하여 응답 크기 최적화
     *
     * @param department Department 엔티티 객체
     * @return DepartmentListResponse DTO 객체
     *
     * 정적 팩토리 메서드를 사용하는 이유:
     * 1. 생성자보다 의미 전달이 명확 (from이라는 이름으로 변환 의도 표현)
     * 2. DTO 생성 로직을 DTO 내부에 캡슐화
     * 3. Service에서 변환 로직 중복 제거
     */
    public static DepartmentListResponse from(Department department) {
        // === null 체크 ===
        // department가 null이면 예외 발생 (방어적 프로그래밍)
        if (department == null) {
            throw new IllegalArgumentException("Department 엔티티가 null입니다.");
        }

        // === Builder 패턴으로 DTO 생성 ===
        return DepartmentListResponse.builder() // DepartmentListResponse 빌더 시작
                // === 식별자 설정 ===
                .id(department.getId()) // Entity의 id를 DTO에 설정

                // === 부서 핵심 정보 설정 ===
                .departmentCode(department.getDepartmentCode()) // 부서 코드 설정
                .departmentName(department.getDepartmentName()) // 부서명 설정
                .description(department.getDescription()) // 설명 설정 (null 가능)

                // === 부서 위치 설정 ===
                .location(department.getLocation()) // 위치 설정 (null 가능)

                // === 부서 통계 설정 ===
                .employeeCount(department.getEmployeeCount()) // 직원 수 설정

                // === 연관된 회사 정보 설정 ===
                .companyName(department.getCompany().getName()) // 회사명 설정
                // department.getCompany(): LAZY 로딩된 Company 엔티티 조회
                // 이 시점에 SELECT * FROM company WHERE company_id = ? 쿼리 실행 (LAZY 로딩)
                // N+1 문제 발생 가능 → Service에서 Fetch Join 사용 권장
                // 예: @Query("SELECT d FROM Department d JOIN FETCH d.company WHERE d.delYn = 'N'")

                // === 메타 정보 설정 ===
                .createdAt(department.getCreatedAt()) // 생성 시간 설정
                // BaseTimeEntity에서 상속받은 필드

                .build(); // DepartmentListResponse DTO 객체 최종 생성

        // 목록 조회에 필요한 최소한의 정보만 포함
        // 응답 크기 감소 및 네트워크 성능 향상
    }
}