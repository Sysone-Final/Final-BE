/**
 * 작성자: 황요한
 * 비즈니스 로직 예외 클래스
 */
package org.example.finalbe.domains.common.exception;

public class BusinessException extends RuntimeException {

    /**
     * 에러 메시지를 받는 생성자
     */
    public BusinessException(String message) {
        super(message);
    }
}