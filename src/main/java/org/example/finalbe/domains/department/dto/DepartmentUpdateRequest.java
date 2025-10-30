package org.example.finalbe.domains.department.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 부서 수정 요청 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 요청 데이터 전달
 * - Validation: Bean Validation으로 필드 검증 자동화
 * - 부분 수정 지원: 모든 필드가 선택 사항 (null 허용)
 *
 * Update DTO 특징:
 * 1. @NotBlank/@NotNull 없음 (모든 필드 선택 입력)
 * 2. null이 아닌 필드만 업데이트 (부분 수정)
 * 3. 부서 코드(departmentCode)는 수정 불가 (포함 안 함)
 * 4. 회사(companyId)는 수정 불가 (포함 안 함)
 *
 * Create DTO와의 차이점:
 * - Create: 필수 필드 존재 (@NotBlank, @NotNull)
 * - Update: 모든 필드 선택 (null 허용, 부분 수정)
 */
@Builder // 빌더 패턴 지원 (테스트 코드 작성 시 유용)
public record DepartmentUpdateRequest(
        // === 부서 기본 정보 ===
        @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다.") // 최대 길이 제한 (null 허용)
        String departmentName, // 부서명 (예: 개발팀, 인사팀, 영업팀)
        // null이면 변경 안 함, 값이 있으면 해당 값으로 변경
        // @NotBlank 없음 → 선택 입력 필드

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.") // 최대 길이 제한 (null 허용)
        String description, // 부서 설명 (예: 서버 개발 및 유지보수 담당)
        // null이면 변경 안 함, 값이 있으면 해당 값으로 변경

        // === 부서 위치 및 연락처 ===
        @Size(max = 200, message = "위치는 200자를 초과할 수 없습니다.") // 최대 길이 제한 (null 허용)
        String location, // 부서 위치 (예: 본사 3층, 서울 지사 2층)
        // null이면 변경 안 함, 값이 있으면 해당 값으로 변경

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", // 정규식으로 전화번호 형식 검증
                message = "전화번호 형식이 올바르지 않습니다. (예: 02-1234-5678)") // 검증 실패 메시지
        String phone, // 부서 전화번호 (예: 02-1234-5678)
        // @Pattern: 정규식 매칭 검증 (null은 허용, 값이 있을 때만 검증)
        // null이면 변경 안 함, 값이 있으면 형식 검증 후 변경
        // 형식: [2-3자리]-[3-4자리]-[4자리]

        @Email(message = "올바른 이메일 형식이 아닙니다.") // 이메일 형식 검증
        @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.") // 최대 길이 제한
        String email // 부서 이메일 (예: dev@company.com)
        // @Email: RFC 5322 표준 이메일 형식 검증 (null은 허용)
        // null이면 변경 안 함, 값이 있으면 형식 검증 후 변경

        // === 수정 불가 필드 (포함 안 함) ===
        // - departmentCode: 부서 코드는 생성 후 변경 불가 (시스템 식별자)
        // - companyId: 부서의 소속 회사는 변경 불가 (비즈니스 규칙)
        // - employeeCount: 시스템에서 자동 계산 (수동 수정 불가)
        // - delYn: 삭제는 별도 API로 처리 (softDelete 메서드)
) {
    // === 변환 메서드 없음 ===
    // Update는 Entity의 updateInfo() 메서드를 직접 호출
    // Service에서: department.updateInfo(
    //     request.departmentName(),
    //     request.description(),
    //     request.location(),
    //     request.phone(),
    //     request.email(),
    //     updatedBy
    // );
    // Entity의 Dirty Checking으로 변경된 필드만 UPDATE 쿼리 생성
}