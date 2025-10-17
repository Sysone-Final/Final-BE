package org.example.finalbe.domains.common.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + "을(를) 찾을 수 없습니다. ID: " + id);
    }

    public EntityNotFoundException(String entityName, String identifier) {
        super(entityName + "을(를) 찾을 수 없습니다: " + identifier);
    }
}