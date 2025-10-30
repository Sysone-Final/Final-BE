package org.example.finalbe.domains.department.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.department.domain.Department;

/**
 * 부서 생성 요청 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 요청 데이터 전달
 * - Validation: Bean Validation으로 필드 검증 자동화
 * - toEntity(): DTO → Entity 변환 메서드 제공
 *
 * Request DTO를 사용하는 이유:
 * 1. Controller에서 받는 요청 데이터의 형식 정의
 * 2. Validation 애노테이션으로 입력값 검증 자동화
 * 3. Entity와 분리하여 API 변경에 유연하게 대응
 * 4. 불필요한 필드 노출 방지 (보안)
 */
@Builder // 빌더 패턴 지원 (테스트 코드 작성 시 유용)
public record DepartmentCreateRequest(
        // === 부서 기본 정보 ===
        @NotBlank(message = "부서 코드를 입력해주세요.") // 필수 입력, 공백 불가
        @Size(max = 50, message = "부서 코드는 50자를 초과할 수 없습니다.") // 최대 길이 제한
        String departmentCode, // 부서 코드 (예: DEV, HR, SALES)
        // @NotBlank: null, 빈 문자열(""), 공백 문자열("   ") 모두 불허
        // Validation 실패 시 MethodArgumentNotValidException 발생

        @NotBlank(message = "부서명을 입력해주세요.") // 필수 입력, 공백 불가
        @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다.") // 최대 길이 제한
        String departmentName, // 부서명 (예: 개발팀, 인사팀, 영업팀)
        // 사용자가 실제로 보게 되는 부서 이름

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.") // 최대 길이 제한 (null 허용)
        String description, // 부서 설명 (예: 서버 개발 및 유지보수 담당)
        // @NotBlank 없음 → 선택 입력 필드

        // === 조직도 관련 (현재 미사용) ===
        Long parentDepartmentId, // 상위 부서 ID (예: 1, 2, 3...)
        // 추후 계층 구조 구현 시 사용 (현재는 null 허용)

        Long managerId, // 부서장 ID (예: 1, 2, 3...)
        // 추후 부서장 지정 기능 구현 시 사용 (현재는 null 허용)

        // === 부서 위치 및 연락처 ===
        @Size(max = 200, message = "위치는 200자를 초과할 수 없습니다.") // 최대 길이 제한 (null 허용)
        String location, // 부서 위치 (예: 본사 3층, 서울 지사 2층)
        // 물리적 사무실 위치 정보

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", // 정규식으로 전화번호 형식 검증
                message = "전화번호 형식이 올바르지 않습니다. (예: 02-1234-5678)") // 검증 실패 메시지
        String phone, // 부서 전화번호 (예: 02-1234-5678)
        // @Pattern: 정규식 매칭 검증 (null은 허용, 값이 있을 때만 검증)
        // 형식: [2-3자리]-[3-4자리]-[4자리]

        @Email(message = "올바른 이메일 형식이 아닙니다.") // 이메일 형식 검증
        @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.") // 최대 길이 제한
        String email, // 부서 이메일 (예: dev@company.com)
        // @Email: RFC 5322 표준 이메일 형식 검증 (null은 허용)

        // === 소속 회사 (필수) ===
        @NotNull(message = "회사를 선택해주세요.") // 필수 입력, null 불가
        @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // 최소값 검증 (1 이상)
        Long companyId // 회사 ID (예: 1, 2, 3...)
        // 부서는 반드시 특정 회사에 속해야 함
        // @Min: 0 이하 값 방지 (잘못된 ID 차단)
) {
    /**
     * DTO → Entity 변환 (정적 팩토리 메서드)
     *
     * - DepartmentCreateRequest를 받아서 Department 엔티티로 변환
     * - Company 엔티티는 Service에서 조회 후 전달
     * - createdBy는 현재 로그인한 사용자 정보 (Service에서 전달)
     *
     * @param company 조회된 Company 엔티티 (Service에서 전달)
     * @param createdBy 생성자 정보 (현재 로그인 사용자)
     * @return Department 엔티티 객체
     *
     * 변환 메서드를 DTO에 두는 이유:
     * 1. DTO → Entity 변환 로직을 DTO에 캡슐화
     * 2. Service 코드 간소화 (department = request.toEntity(company, createdBy))
     * 3. 변환 로직 재사용 및 테스트 용이
     */
    public Department toEntity(Company company, String createdBy) {
        // === Builder 패턴으로 Department 엔티티 생성 ===
        return Department.builder() // Department 빌더 시작
                // === 요청 DTO의 값을 엔티티에 매핑 ===
                .departmentCode(this.departmentCode) // 부서 코드 설정
                .departmentName(this.departmentName) // 부서명 설정
                .description(this.description) // 설명 설정 (null 가능)
                .location(this.location) // 위치 설정 (null 가능)
                .phone(this.phone) // 전화번호 설정 (null 가능)
                .email(this.email) // 이메일 설정 (null 가능)
                // === 연관관계 설정 ===
                .company(company) // 조회된 Company 엔티티 설정
                // Service에서 findById()로 조회한 Company 객체 전달
                // === 생성자 정보 설정 ===
                .createdBy(createdBy) // 생성자 정보 설정
                // JWT에서 추출한 사용자 이메일 또는 ID
                .build(); // Department 엔티티 객체 최종 생성
        // 나머지 필드(id, employeeCount, delYn, createdAt 등)는 기본값 또는 자동 생성
    }
}