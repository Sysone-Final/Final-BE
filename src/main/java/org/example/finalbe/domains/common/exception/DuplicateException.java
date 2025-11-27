/**
 * 작성자: 황요한
 * 중복된 데이터가 존재할 때 발생하는 예외
 * HTTP 상태 코드: 409 (Conflict)
 */
package org.example.finalbe.domains.common.exception;

public class DuplicateException extends RuntimeException {

    /**
     * 필드 이름과 값을 받아 자동으로 메시지를 구성하는 생성자
     */
    public DuplicateException(String fieldName, String value) {
        super("이미 존재하는 " + fieldName + "입니다: " + value);
    }

    /**
     * 직접 메시지를 전달하는 생성자
     */
    public DuplicateException(String message) {
        super(message);
    }
}
