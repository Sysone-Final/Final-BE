package org.example.finalbe.domains.common.exception;

public class DuplicateException extends RuntimeException {
    public DuplicateException(String message) {
        super(message);
    }

    public DuplicateException(String fieldName, String value) {
        super("이미 존재하는 " + fieldName + "입니다: " + value);
    }
}