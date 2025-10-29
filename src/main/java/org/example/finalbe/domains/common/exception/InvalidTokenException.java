package org.example.finalbe.domains.common.exception;

/**
 * JWT 토큰이 유효하지 않을 때 발생하는 예외
 * HTTP 상태 코드: 401 Unauthorized
 *
 * 사용 시점:
 * - 토큰이 만료된 경우
 * - 토큰 서명이 올바르지 않은 경우
 * - 토큰 형식이 잘못된 경우
 * - Refresh Token이 DB에 없는 경우
 * - 로그아웃된 토큰으로 요청한 경우
 */
public class InvalidTokenException extends RuntimeException {

    /**
     * 에러 메시지를 받는 생성자
     *
     * @param message 에러 메시지
     *
     * 사용 예시:
     * throw new InvalidTokenException("만료된 토큰입니다.");
     */
    public InvalidTokenException(String message) {
        super(message);
        // 토큰 관련 구체적인 에러 메시지 전달
    }

    /**
     * 기본 메시지를 사용하는 생성자
     *
     * 사용 예시:
     * throw new InvalidTokenException();
     * → "유효하지 않은 토큰입니다."
     */
    public InvalidTokenException() {
        super("유효하지 않은 토큰입니다.");
        // 일반적인 토큰 오류를 나타내는 기본 메시지
    }

    // === 사용 예시 ===

    // Access Token 검증 실패:
    // if (!jwtTokenProvider.validateToken(accessToken)) {
    //     throw new InvalidTokenException("유효하지 않은 Access Token입니다.");
    // }

    // Refresh Token 검증 실패:
    // if (!member.isRefreshTokenValid(refreshToken)) {
    //     throw new InvalidTokenException("만료되었거나 존재하지 않는 Refresh Token입니다.");
    // }

    // 토큰 형식 오류:
    // if (accessToken == null || accessToken.trim().isEmpty()) {
    //     throw new InvalidTokenException("Access Token이 제공되지 않았습니다.");
    // }

    // === GlobalExceptionHandler에서 처리 ===
    // @ExceptionHandler(InvalidTokenException.class)
    // public ResponseEntity<CommonErrorDto> handleInvalidTokenException(InvalidTokenException e) {
    //     return ResponseEntity.status(401)
    //         .body(new CommonErrorDto(HttpStatus.UNAUTHORIZED, e.getMessage()));
    // }

    // === 클라이언트 처리 예시 ===
    // 401 에러 수신 시 클라이언트는:
    // 1. 로컬 저장소의 Access Token 삭제
    // 2. Refresh Token으로 토큰 재발급 시도 (/api/auth/refresh)
    // 3. 재발급 실패 시 로그인 페이지로 리다이렉트
}