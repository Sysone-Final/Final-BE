package org.example.finalbe.domains.department.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 회원-부서 매핑 생성 요청 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 요청 데이터 전달
 * - Validation: Bean Validation으로 필드 검증 자동화
 * - 중간 테이블 생성용 DTO (Member ↔ Department 연결)
 *
 * Request DTO를 사용하는 이유:
 * 1. 회원을 특정 부서에 배치하는 요청 처리
 * 2. 겸직 지원 (한 회원이 여러 부서에 속할 수 있음)
 * 3. 주 부서/부 부서 구분
 * 4. 부서별 직급 및 배치일 관리
 */
@Builder // 빌더 패턴 지원 (테스트 코드 작성 시 유용)
public record MemberDepartmentCreateRequest(
        // === 회원 정보 (필수) ===
        @NotNull(message = "회원 ID를 입력해주세요.") // 필수 입력, null 불가
        @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") // 최소값 검증 (1 이상)
        Long memberId, // 배치할 회원의 ID (예: 1, 2, 3...)
        // 누구를 부서에 배치할 것인지 지정
        // @Min: 0 이하 값 방지 (잘못된 ID 차단)

        // === 부서 정보 (필수) ===
        @NotNull(message = "부서 ID를 입력해주세요.") // 필수 입력, null 불가
        @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") // 최소값 검증 (1 이상)
        Long departmentId, // 배치할 부서의 ID (예: 1, 2, 3...)
        // 어느 부서에 배치할 것인지 지정
        // @Min: 0 이하 값 방지 (잘못된 ID 차단)

        // === 주 부서 여부 (선택) ===
        Boolean isPrimary, // 주 부서 여부 (true: 주 부서, false: 부 부서)
        // null 허용 (Service에서 기본값 false 처리)
        // 회원은 여러 부서에 속할 수 있지만, 주 부서는 1개만 가능
        // 예: 개발팀(주 부서), 기획팀(부 부서)

        // === 부서 내 직급 (선택) ===
        @Size(max = 100, message = "직급은 100자를 초과할 수 없습니다.") // 최대 길이 제한 (null 허용)
        String position, // 해당 부서에서의 직급 (예: 팀장, 매니저, 사원)
        // 같은 회원이라도 부서마다 다른 직급을 가질 수 있음
        // 예: 개발팀에서는 "시니어 개발자", 기획팀에서는 "기획 매니저"
        // @NotBlank 없음 → 선택 입력 필드

        // === 부서 배치일 (선택) ===
        LocalDate joinDate // 부서 배치일 (예: 2023-01-15)
        // 언제부터 해당 부서에 속했는지 기록
        // null 허용 (Service에서 현재 날짜로 처리 가능)
        // ISO 8601 형식으로 전달 (예: "2023-01-15")

        // === 변환 메서드 없음 ===
        // Service에서 memberId와 departmentId로 Entity를 조회한 후
        // MemberDepartment.builder()로 직접 생성
        // 예:
        // MemberDepartment.builder()
        //     .member(memberRepository.findById(memberId))
        //     .department(departmentRepository.findById(departmentId))
        //     .isPrimary(isPrimary != null ? isPrimary : false)
        //     .position(position)
        //     .joinDate(joinDate != null ? joinDate : LocalDate.now())
        //     .build();
) {
}