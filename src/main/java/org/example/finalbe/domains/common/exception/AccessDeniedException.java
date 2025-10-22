package org.example.finalbe.domains.common.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("접근 권한이 없습니다.");
    }
}