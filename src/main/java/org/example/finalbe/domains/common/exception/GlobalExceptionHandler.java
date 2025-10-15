package org.example.finalbe.domains.common.exception;

import org.example.finalbe.domains.common.dto.CommonErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * IllegalArgumentException 처리
     * - 비즈니스 로직 검증 실패 (중복 아이디, 잘못된 비밀번호 등)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonErrorDto> handleIllegalArgumentException(IllegalArgumentException e) {
        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * 일반 Exception 처리
     * - 예상하지 못한 서버 에러
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonErrorDto> handleException(Exception e) {
        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    /**
     * NullPointerException 처리
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<CommonErrorDto> handleNullPointerException(NullPointerException e) {
        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }
}