package org.example.finalbe.domains.department.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 랙-부서 매핑 생성 요청 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 요청 데이터 전달
 * - Validation: Bean Validation으로 필드 검증 자동화
 * - 중간 테이블 생성용 DTO (Rack ↔ Department 연결)
 *
 * Request DTO를 사용하는 이유:
 * 1. 랙을 특정 부서에 배정하는 요청 처리
 * 2. 공동 관리 지원 (한 랙이 여러 부서에 의해 관리될 수 있음)
 * 3. 주 담당 부서/부 담당 부서 구분
 * 4. 부서별 책임 범위 및 배정일 관리
 */
@Builder // 빌더 패턴 지원 (테스트 코드 작성 시 유용)
public record RackDepartmentCreateRequest(
        // === 랙 정보 (필수) ===
        @NotNull(message = "랙 ID를 입력해주세요.") // 필수 입력, null 불가
        @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") // 최소값 검증 (1 이상)
        Long rackId, // 배정할 랙의 ID (예: 1, 2, 3...)
        // 어떤 랙을 부서에 배정할 것인지 지정
        // @Min: 0 이하 값 방지 (잘못된 ID 차단)

        // === 부서 정보 (필수) ===
        @NotNull(message = "부서 ID를 입력해주세요.") // 필수 입력, null 불가
        @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") // 최소값 검증 (1 이상)
        Long departmentId, // 배정받을 부서의 ID (예: 1, 2, 3...)
        // 어느 부서가 랙을 관리할 것인지 지정
        // @Min: 0 이하 값 방지 (잘못된 ID 차단)

        // === 주 담당 부서 여부 (선택) ===
        Boolean isPrimary, // 주 담당 부서 여부 (true: 주 담당, false: 부 담당)
        // null 허용 (Service에서 기본값 false 처리)
        // 랙은 여러 부서가 관리할 수 있지만, 주 담당 부서는 1개만 가능
        // 예: 서버실 랙 → 개발팀(주 담당), 인프라팀(부 담당)

        // === 책임 범위 (선택) ===
        @Size(max = 200, message = "담당 업무는 200자를 초과할 수 없습니다.") // 최대 길이 제한 (null 허용)
        String responsibility, // 담당 업무/책임 범위 (예: 일상 점검 및 유지보수, 장애 대응)
        // 해당 부서가 이 랙에 대해 어떤 업무를 담당하는지 명시
        // 예: "하드웨어 유지보수", "네트워크 모니터링", "보안 점검"
        // @NotBlank 없음 → 선택 입력 필드

        // === 배정일 (선택) ===
        LocalDate assignedDate // 배정일 (예: 2023-01-15)
        // 언제부터 해당 부서가 이 랙을 담당했는지 기록
        // null 허용 (Service에서 현재 날짜로 처리 가능)
        // ISO 8601 형식으로 전달 (예: "2023-01-15")

        // === 변환 메서드 없음 ===
        // Service에서 rackId와 departmentId로 Entity를 조회한 후
        // RackDepartment.builder()로 직접 생성
        // 예:
        // RackDepartment.builder()
        //     .rack(rackRepository.findById(rackId))
        //     .department(departmentRepository.findById(departmentId))
        //     .isPrimary(isPrimary != null ? isPrimary : false)
        //     .responsibility(responsibility)
        //     .assignedDate(assignedDate != null ? assignedDate : LocalDate.now())
        //     .build();
) {
}