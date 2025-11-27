/**
 * 작성자: 황요한
 * 유효하지 않은 토큰 예외 클래스
 */
package org.example.finalbe.domains.common.exception;

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