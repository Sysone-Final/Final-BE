package org.example.finalbe.domains.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 공통 에러 응답 DTO
 * 모든 API의 에러 응답을 표준화
 *
 * 응답 구조:
 * {
 *   "status_code": 400,
 *   "status_message": "에러 메시지"
 * }
 *
 * 사용 이유:
 * - 에러 응답 형식을 통일하여 클라이언트가 일관되게 에러 처리 가능
 * - GlobalExceptionHandler에서 모든 예외를 이 DTO로 변환하여 반환
 * - 프론트엔드에서 에러 메시지를 쉽게 추출하여 사용자에게 표시
 */
@Data // Lombok: getter, setter, toString, equals, hashCode 자동 생성
@NoArgsConstructor // 기본 생성자 자동 생성
public class CommonErrorDto {

    // === 에러 응답 필드 ===

    private int status_code; // HTTP 에러 상태 코드
    // 400: Bad Request (잘못된 요청)
    // 401: Unauthorized (인증 실패)
    // 403: Forbidden (권한 없음)
    // 404: Not Found (리소스 없음)
    // 409: Conflict (중복 등 충돌)
    // 500: Internal Server Error (서버 오류)

    private String status_message; // 에러 메시지 (사용자에게 표시)
    // "아이디 또는 비밀번호가 일치하지 않습니다."
    // "해당 리소스를 찾을 수 없습니다."
    // "접근 권한이 없습니다."

    /**
     * 생성자: HTTP 상태와 에러 메시지를 받아 초기화
     *
     * @param httpStatus HTTP 상태 (HttpStatus Enum)
     * @param status_message 에러 메시지
     *
     * 사용 예시:
     * return new CommonErrorDto(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다.");
     */
    public CommonErrorDto(HttpStatus httpStatus, String status_message) {
        this.status_code = httpStatus.value(); // HttpStatus Enum을 숫자로 변환
        // 예: HttpStatus.BAD_REQUEST.value() → 400

        this.status_message = status_message; // 에러 메시지 설정
    }

    // === 에러 응답 예시 ===

    // 예시 1: 유효성 검증 실패 (400)
    // {
    //   "status_code": 400,
    //   "status_message": "아이디를 입력해주세요."
    // }

    // 예시 2: 인증 실패 (401)
    // {
    //   "status_code": 401,
    //   "status_message": "유효하지 않은 토큰입니다."
    // }

    // 예시 3: 권한 없음 (403)
    // {
    //   "status_code": 403,
    //   "status_message": "관리자만 수정할 수 있습니다."
    // }

    // 예시 4: 리소스 없음 (404)
    // {
    //   "status_code": 404,
    //   "status_message": "회원을 찾을 수 없습니다."
    // }

    // 예시 5: 중복 (409)
    // {
    //   "status_code": 409,
    //   "status_message": "이미 존재하는 아이디입니다."
    // }

    // === 프론트엔드 에러 처리 예시 (JavaScript) ===
    // try {
    //   const response = await fetch('/api/auth/login', { ... });
    //   if (!response.ok) {
    //     const error = await response.json();
    //     alert(error.status_message); // "아이디 또는 비밀번호가 일치하지 않습니다."
    //   }
    // } catch (error) {
    //   console.error('Network error', error);
    // }
}