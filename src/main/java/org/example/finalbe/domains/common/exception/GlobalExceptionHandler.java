package org.example.finalbe.domains.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.dto.CommonErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * 애플리케이션에서 발생하는 모든 예외를 일관된 형식으로 처리
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * EntityNotFoundException 처리
     * 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<CommonErrorDto> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("EntityNotFoundException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    /**
     * DuplicateException 처리
     * 409 Conflict
     */
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<CommonErrorDto> handleDuplicateException(DuplicateException e) {
        log.warn("DuplicateException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.CONFLICT, e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDto);
    }

    /**
     * 커스텀 AccessDeniedException 처리
     * 401 Unauthorized 또는 403 Forbidden
     */
    @ExceptionHandler(org.example.finalbe.domains.common.exception.AccessDeniedException.class)
    public ResponseEntity<CommonErrorDto> handleCustomAccessDeniedException(
            org.example.finalbe.domains.common.exception.AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        HttpStatus status = e.getMessage().contains("인증") ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        CommonErrorDto errorDto = new CommonErrorDto(status, e.getMessage());
        return ResponseEntity.status(status).body(errorDto);
    }

    /**
     * Spring Security AccessDeniedException 처리
     * 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonErrorDto> handleSpringAccessDeniedException(AccessDeniedException e) {
        log.warn("Spring AccessDeniedException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDto);
    }

    /**
     * InvalidTokenException 처리
     * 401 Unauthorized
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<CommonErrorDto> handleInvalidTokenException(InvalidTokenException e) {
        log.warn("InvalidTokenException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDto);
    }

    /**
     * AuthenticationException 처리
     * 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonErrorDto> handleAuthenticationException(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDto);
    }

    /**
     * BusinessException 처리
     * 400 Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonErrorDto> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * IllegalArgumentException 처리
     * 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonErrorDto> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * IllegalStateException 처리
     * 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CommonErrorDto> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * MethodArgumentNotValidException 처리
     * @Valid 검증 실패 (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonErrorDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = errors.isEmpty()
                ? "입력값 검증에 실패했습니다."
                : errors.values().stream().collect(Collectors.joining(", "));

        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * ConstraintViolationException 처리
     * @Validated 검증 실패 (400 Bad Request)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonErrorDto> handleConstraintViolationException(
            ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());

        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * HttpMessageNotReadableException 처리
     * JSON 파싱 오류 (400 Bad Request)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonErrorDto> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST, "요청 데이터 형식이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * MissingServletRequestParameterException 처리
     * 필수 파라미터 누락 (400 Bad Request)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonErrorDto> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                "필수 파라미터가 누락되었습니다: " + e.getParameterName()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * MethodArgumentTypeMismatchException 처리
     * 타입 불일치 (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonErrorDto> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                "파라미터 타입이 올바르지 않습니다: " + e.getName()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * NullPointerException 처리
     * 500 Internal Server Error
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<CommonErrorDto> handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException occurred", e);
        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    /**
     * 일반 Exception 처리
     * 예상하지 못한 모든 예외 (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonErrorDto> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }
}