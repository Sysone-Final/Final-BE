/**
 * 작성자: 황요한
 * 접근 권한 예외 클래스
 */
package org.example.finalbe.domains.common.exception;

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