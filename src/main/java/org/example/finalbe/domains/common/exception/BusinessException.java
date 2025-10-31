package org.example.finalbe.domains.common.exception;

/**
 * 비즈니스 로직에서 발생하는 일반적인 예외
 * HTTP 상태 코드: 400 Bad Request
 */
public class BusinessException extends RuntimeException {

    /**
     * 에러 메시지를 받는 생성자
     */
    public BusinessException(String message) {
        super(message);
    }
}