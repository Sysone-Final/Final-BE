package org.example.finalbe.domains.common.exception;

/**
 * 접근 권한이 없을 때 발생하는 예외
 * HTTP 상태 코드: 403 Forbidden 또는 401 Unauthorized
 */
public class AccessDeniedException extends RuntimeException {

    /**
     * 에러 메시지를 받는 생성자
     */
    public AccessDeniedException(String message) {
        super(message);
    }

    /**
     * 기본 메시지를 사용하는 생성자
     */
    public AccessDeniedException() {
        super("접근 권한이 없습니다.");
    }
}