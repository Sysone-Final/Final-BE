package org.example.finalbe.domains.common.exception;

/**
 * JWT 토큰이 유효하지 않을 때 발생하는 예외
 * HTTP 상태 코드: 401 Unauthorized
 */
public class InvalidTokenException extends RuntimeException {

    /**
     * 에러 메시지를 받는 생성자
     */
    public InvalidTokenException(String message) {
        super(message);
    }

    /**
     * 기본 메시지를 사용하는 생성자
     */
    public InvalidTokenException() {
        super("유효하지 않은 토큰입니다.");
    }
}