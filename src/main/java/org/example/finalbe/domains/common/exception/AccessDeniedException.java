package org.example.finalbe.domains.common.exception;

/**
 * 접근 권한이 없을 때 발생하는 예외
 * HTTP 상태 코드: 403 Forbidden (권한 부족) 또는 401 Unauthorized (인증 필요)
 *
 * 사용 시점:
 * - 권한이 부족한 사용자가 관리자 기능 접근
 * - VIEWER 권한으로 수정/삭제 시도
 * - 다른 회사의 데이터에 접근 시도
 * - 인증되지 않은 사용자가 보호된 리소스 접근
 */
public class AccessDeniedException extends RuntimeException {

    /**
     * 에러 메시지를 받는 생성자
     *
     * @param message 에러 메시지
     *
     * 사용 예시:
     * throw new AccessDeniedException("관리자만 수정할 수 있습니다.");
     */
    public AccessDeniedException(String message) {
        super(message);
        // 메시지에 따라 401과 403을 구분할 수 있음
        // "인증이 필요합니다" → 401 Unauthorized
        // "권한이 없습니다" → 403 Forbidden
    }

    /**
     * 기본 메시지를 사용하는 생성자
     *
     * 사용 예시:
     * throw new AccessDeniedException();
     * → "접근 권한이 없습니다."
     */
    public AccessDeniedException() {
        super("접근 권한이 없습니다.");
        // 기본 메시지로 일반적인 권한 오류 표현
    }

    // === 사용 예시 ===

    // 권한 체크:
    // if (member.getRole() != Role.ADMIN) {
    //     throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");
    // }

    // 인증 체크:
    // if (authentication == null || !authentication.isAuthenticated()) {
    //     throw new AccessDeniedException("인증이 필요합니다.");
    // }

    // 회사별 데이터 접근 제한:
    // if (!member.getCompany().getId().equals(companyId)) {
    //     throw new AccessDeniedException("해당 회사의 데이터에 접근할 권한이 없습니다.");
    // }

    // === GlobalExceptionHandler에서 처리 ===
    // @ExceptionHandler(AccessDeniedException.class)
    // public ResponseEntity<CommonErrorDto> handleAccessDeniedException(AccessDeniedException e) {
    //     // 메시지에 "인증"이 포함되면 401, 아니면 403
    //     HttpStatus status = e.getMessage().contains("인증") ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
    //     return ResponseEntity.status(status).body(new CommonErrorDto(status, e.getMessage()));
    // }
}