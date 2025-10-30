package org.example.finalbe.domains.department.dto;

import lombok.Builder;
import org.example.finalbe.domains.department.domain.Department;

import java.time.LocalDateTime;

/**
 * 부서 상세 조회 응답 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 응답 데이터 전달
 * - Entity → DTO 변환: from() 정적 팩토리 메서드 제공
 * - 모든 부서 정보 포함 (상세 조회용)
 * - 연관된 회사 정보 포함 (companyId, companyName)
 *
 * Response DTO를 사용하는 이유:
 * 1. Entity 직접 노출 방지 (보안 및 순환 참조 방지)
 * 2. 필요한 정보만 선택적으로 반환
 * 3. JSON 직렬화 최적화
 * 4. API 응답 형식 일관성 유지
 */
@Builder // 빌더 패턴 지원
public record DepartmentDetailResponse(
        // === 식별자 ===
        Long id, // 부서 고유 식별자 (예: 1, 2, 3...)

        // === 부서 기본 정보 ===
        String departmentCode, // 부서 코드 (예: DEV, HR, SALES)
        String departmentName, // 부서명 (예: 개발팀, 인사팀, 영업팀)
        String description, // 부서 설명 (예: 서버 개발 및 유지보수 담당)

        // === 부서 위치 및 연락처 ===
        String location, // 부서 위치 (예: 본사 3층, 서울 지사 2층)
        String phone, // 부서 전화번호 (예: 02-1234-5678)
        String email, // 부서 이메일 (예: dev@company.com)

        // === 부서 통계 ===
        Integer employeeCount, // 소속 직원 수 (예: 5, 10, 15...)

        // === 연관된 회사 정보 ===
        Long companyId, // 소속 회사 ID (예: 1, 2, 3...)
        String companyName, // 소속 회사명 (예: 테크놀로지 주식회사)
        // Entity의 company를 바로 노출하지 않고, 필요한 정보만 추출
        // 순환 참조 방지 및 응답 크기 최적화

        // === 생성/수정자 정보 ===
        String createdBy, // 생성자 (예: "admin", "user@company.com")
        String updatedBy, // 수정자 (예: "admin", "user@company.com")

        // === 메타 정보 (생성/수정 시간) ===
        LocalDateTime createdAt, // 생성 시간 (BaseTimeEntity에서 자동 관리)
        LocalDateTime updatedAt // 수정 시간 (BaseTimeEntity에서 자동 관리)

        // delYn은 포함하지 않음 (내부 관리용이므로 외부에 노출할 필요 없음)
) {
    /**
     * Entity → DTO 변환 (정적 팩토리 메서드)
     *
     * - Department 엔티티를 받아서 DepartmentDetailResponse DTO로 변환
     * - 연관된 Company 정보도 함께 추출 (companyId, companyName)
     *
     * @param department Department 엔티티 객체
     * @return DepartmentDetailResponse DTO 객체
     *
     * 정적 팩토리 메서드를 사용하는 이유:
     * 1. 생성자보다 의미 전달이 명확 (from이라는 이름으로 변환 의도 표현)
     * 2. DTO 생성 로직을 DTO 내부에 캡슐화
     * 3. Service에서 변환 로직 중복 제거
     */
    public static DepartmentDetailResponse from(Department department) {
        // === null 체크 ===
        // department가 null이면 예외 발생 (방어적 프로그래밍)
        if (department == null) {
            throw new IllegalArgumentException("Department 엔티티가 null입니다.");
        }

        // === Builder 패턴으로 DTO 생성 ===
        return DepartmentDetailResponse.builder() // DepartmentDetailResponse 빌더 시작
                // === 식별자 설정 ===
                .id(department.getId()) // Entity의 id를 DTO에 설정
                // department.getId(): Entity의 getter 호출

                // === 부서 기본 정보 설정 ===
                .departmentCode(department.getDepartmentCode()) // 부서 코드 설정
                .departmentName(department.getDepartmentName()) // 부서명 설정
                .description(department.getDescription()) // 설명 설정 (null 가능)

                // === 부서 위치 및 연락처 설정 ===
                .location(department.getLocation()) // 위치 설정 (null 가능)
                .phone(department.getPhone()) // 전화번호 설정 (null 가능)
                .email(department.getEmail()) // 이메일 설정 (null 가능)

                // === 부서 통계 설정 ===
                .employeeCount(department.getEmployeeCount()) // 직원 수 설정

                // === 연관된 회사 정보 설정 ===
                .companyId(department.getCompany().getId()) // 회사 ID 설정
                // department.getCompany(): LAZY 로딩된 Company 엔티티 조회
                // 이 시점에 SELECT * FROM company WHERE company_id = ? 쿼리 실행 (LAZY 로딩)
                .companyName(department.getCompany().getName()) // 회사명 설정
                // 이미 로딩된 Company에서 name 추출 (추가 쿼리 없음)

                // === 생성/수정자 정보 설정 ===
                .createdBy(department.getCreatedBy()) // 생성자 설정
                .updatedBy(department.getUpdatedBy()) // 수정자 설정 (null 가능)

                // === 메타 정보 설정 ===
                .createdAt(department.getCreatedAt()) // 생성 시간 설정
                // BaseTimeEntity에서 상속받은 필드
                .updatedAt(department.getUpdatedAt()) // 수정 시간 설정

                .build(); // DepartmentDetailResponse DTO 객체 최종 생성

        // 모든 필드를 그대로 복사 (Entity의 모든 정보를 DTO에 포함)
        // 상세 조회이므로 가능한 많은 정보를 제공
    }
}