/**
 * 작성자: 황요한
 * 엔티티를 찾을 수 없을 때 발생하는 예외
 * HTTP 상태 코드: 404 (Not Found)
 */
package org.example.finalbe.domains.common.exception;

public class EntityNotFoundException extends RuntimeException {

    /**
     * 엔티티 타입과 ID를 받아 메시지 생성
     */
    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + "을(를) 찾을 수 없습니다. ID: " + id);
    }

    /**
     * 엔티티 타입과 이름을 받아 메시지 생성
     */
    public EntityNotFoundException(String entityName, String name) {
        super(entityName + "을(를) 찾을 수 없습니다. name: " + name);
    }

    /**
     * 직접 메시지를 받는 생성자
     */
    public EntityNotFoundException(String message) {
        super(message);
    }
}
