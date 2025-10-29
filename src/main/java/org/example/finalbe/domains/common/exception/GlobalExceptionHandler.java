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
 *
 * 사용 기술:
 * - @RestControllerAdvice: 모든 컨트롤러에서 발생하는 예외를 처리
 * - @ExceptionHandler: 특정 예외 타입을 처리하는 메서드 지정
 * - CommonErrorDto: 일관된 에러 응답 형식
 *
 * 장점:
 * - 예외 처리 로직 중앙화
 * - 일관된 에러 응답 형식
 * - 로깅을 통한 에러 추적
 * - 클라이언트 친화적인 에러 메시지
 */
@Slf4j // Lombok의 로깅 기능
@RestControllerAdvice // 모든 @RestController에서 발생하는 예외를 처리
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// @ControllerAdvice: 컨트롤러 전역에서 발생하는 예외를 처리
// @ResponseBody: 반환값을 HTTP 응답 바디에 JSON으로 직렬화
public class GlobalExceptionHandler {

    /**
     * EntityNotFoundException 처리
     * 엔티티를 찾을 수 없는 경우 (404 Not Found)
     *
     * @param e EntityNotFoundException 예외 객체
     * @return 404 상태 코드와 에러 메시지
     */
    @ExceptionHandler(EntityNotFoundException.class)
    // @ExceptionHandler: 이 메서드가 EntityNotFoundException을 처리함을 선언
    // Spring이 EntityNotFoundException 발생 시 이 메서드를 자동으로 호출
    public ResponseEntity<CommonErrorDto> handleEntityNotFoundException(
            EntityNotFoundException e) {
        // === 로깅 ===
        log.warn("EntityNotFoundException: {}", e.getMessage());
        // log.warn(): 경고 수준의 로그 (예상 가능한 오류)
        // 스택 트레이스는 출력하지 않음 (비즈니스 로직 오류이므로)

        // === 에러 응답 생성 ===
        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.NOT_FOUND, // 404 상태 코드
                e.getMessage() // 예외 메시지 ("회원을 찾을 수 없습니다. ID: 123")
        );

        // === HTTP 응답 반환 ===
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
        // ResponseEntity.status(): HTTP 상태 코드 설정
        // .body(): 응답 바디에 errorDto를 JSON으로 변환하여 전달
    }

    /**
     * DuplicateException 처리
     * 중복된 데이터가 존재하는 경우 (409 Conflict)
     *
     * @param e DuplicateException 예외 객체
     * @return 409 상태 코드와 에러 메시지
     */
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<CommonErrorDto> handleDuplicateException(DuplicateException e) {
        log.warn("DuplicateException: {}", e.getMessage());
        // 중복 오류는 사용자 입력 문제이므로 warn 레벨

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.CONFLICT, // 409 상태 코드 (충돌)
                e.getMessage() // "이미 존재하는 아이디입니다: user123"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDto);
    }

    /**
     * 커스텀 AccessDeniedException 처리
     * 접근 권한이 없는 경우 (403 Forbidden 또는 401 Unauthorized)
     *
     * @param e AccessDeniedException 예외 객체
     * @return 401 또는 403 상태 코드와 에러 메시지
     *
     * 401 vs 403:
     * - 401 Unauthorized: 인증 필요 ("로그인이 필요합니다")
     * - 403 Forbidden: 권한 부족 ("관리자만 접근 가능합니다")
     */
    @ExceptionHandler(org.example.finalbe.domains.common.exception.AccessDeniedException.class)
    // 패키지명을 명시하여 Spring Security의 AccessDeniedException과 구분
    public ResponseEntity<CommonErrorDto> handleCustomAccessDeniedException(
            org.example.finalbe.domains.common.exception.AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());

        // === 메시지 기반으로 401과 403 구분 ===
        HttpStatus status = e.getMessage().contains("인증")
                ? HttpStatus.UNAUTHORIZED // 401: "인증이 필요합니다"
                : HttpStatus.FORBIDDEN;   // 403: "권한이 없습니다"
        // 메시지에 "인증"이라는 단어가 포함되면 401, 아니면 403

        CommonErrorDto errorDto = new CommonErrorDto(status, e.getMessage());
        return ResponseEntity.status(status).body(errorDto);
    }

    /**
     * Spring Security AccessDeniedException 처리
     * Spring Security에서 발생하는 권한 부족 (403 Forbidden)
     *
     * @param e Spring Security의 AccessDeniedException
     * @return 403 상태 코드와 에러 메시지
     */
    @ExceptionHandler(AccessDeniedException.class)
    // Spring Security의 AccessDeniedException 처리
    // 이 예외는 @PreAuthorize, @Secured 등에서 권한 검증 실패 시 발생
    public ResponseEntity<CommonErrorDto> handleSpringAccessDeniedException(AccessDeniedException e) {
        log.warn("Spring AccessDeniedException: {}", e.getMessage());

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.FORBIDDEN,
                "접근 권한이 없습니다." // 일반적인 권한 오류 메시지
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDto);
    }

    /**
     * InvalidTokenException 처리
     * 유효하지 않은 JWT 토큰 (401 Unauthorized)
     *
     * @param e InvalidTokenException 예외 객체
     * @return 401 상태 코드와 에러 메시지
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<CommonErrorDto> handleInvalidTokenException(InvalidTokenException e) {
        log.warn("InvalidTokenException: {}", e.getMessage());
        // JWT 토큰 오류는 경고 수준 (인증 실패)

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.UNAUTHORIZED, // 401 상태 코드
                e.getMessage() // "만료된 토큰입니다" 등
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDto);
    }

    /**
     * AuthenticationException 처리
     * Spring Security 인증 실패 (401 Unauthorized)
     *
     * @param e AuthenticationException 예외 객체
     * @return 401 상태 코드와 에러 메시지
     */
    @ExceptionHandler(AuthenticationException.class)
    // Spring Security에서 인증 실패 시 발생
    // 예: 잘못된 비밀번호, 존재하지 않는 사용자
    public ResponseEntity<CommonErrorDto> handleAuthenticationException(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.UNAUTHORIZED,
                "인증에 실패했습니다." // 일반적인 인증 실패 메시지
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDto);
    }

    /**
     * BusinessException 처리
     * 비즈니스 로직 오류 (400 Bad Request)
     *
     * @param e BusinessException 예외 객체
     * @return 400 상태 코드와 에러 메시지
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonErrorDto> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST, // 400 상태 코드
                e.getMessage() // "랙에 장비가 존재하여 삭제할 수 없습니다" 등
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * IllegalArgumentException 처리
     * 잘못된 인자 (400 Bad Request)
     *
     * @param e IllegalArgumentException 예외 객체
     * @return 400 상태 코드와 에러 메시지
     */
    @ExceptionHandler(IllegalArgumentException.class)
    // Java 표준 예외: 메서드에 잘못된 인자가 전달된 경우
    public ResponseEntity<CommonErrorDto> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                e.getMessage() // "아이디를 입력해주세요" 등
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * IllegalStateException 처리
     * 잘못된 상태 (400 Bad Request)
     *
     * @param e IllegalStateException 예외 객체
     * @return 400 상태 코드와 에러 메시지
     */
    @ExceptionHandler(IllegalStateException.class)
    // Java 표준 예외: 객체의 상태가 메서드 호출에 적합하지 않은 경우
    public ResponseEntity<CommonErrorDto> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: {}", e.getMessage());

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                e.getMessage() // "비활성화된 계정입니다" 등
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * MethodArgumentNotValidException 처리
     * @Valid 검증 실패 (RequestBody) (400 Bad Request)
     *
     * @param e MethodArgumentNotValidException 예외 객체
     * @return 400 상태 코드와 검증 실패 메시지
     *
     * 발생 시점:
     * - @Valid @RequestBody로 DTO를 받을 때
     * - @NotBlank, @Size, @Email 등의 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonErrorDto> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("MethodArgumentNotValidException: {}", e.getMessage());

        // === 검증 실패한 필드와 메시지를 Map으로 수집 ===
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            // getBindingResult(): 검증 결과 객체
            // getAllErrors(): 모든 검증 오류 목록

            String fieldName = ((FieldError) error).getField(); // 실패한 필드명
            String errorMessage = error.getDefaultMessage(); // 검증 실패 메시지
            errors.put(fieldName, errorMessage);
            // 예: {"userName": "아이디를 입력해주세요", "password": "비밀번호는 8자 이상이어야 합니다"}
        });

        // === 에러 메시지 생성 ===
        String errorMessage = errors.isEmpty()
                ? "입력값 검증에 실패했습니다." // 기본 메시지
                : errors.values().stream().collect(Collectors.joining(", ")); // 모든 오류 메시지를 쉼표로 연결
        // 예: "아이디를 입력해주세요, 비밀번호는 8자 이상이어야 합니다"

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                errorMessage
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * ConstraintViolationException 처리
     * @Validated 검증 실패 (PathVariable, RequestParam) (400 Bad Request)
     *
     * @param e ConstraintViolationException 예외 객체
     * @return 400 상태 코드와 검증 실패 메시지
     *
     * 발생 시점:
     * - @Validated가 클래스 레벨에 있을 때
     * - @PathVariable, @RequestParam에 @Min, @NotNull 등의 검증 실패
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonErrorDto> handleConstraintViolationException(
            ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());

        // === 검증 실패 메시지 수집 ===
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage) // 각 위반의 메시지 추출
                .collect(Collectors.joining(", ")); // 쉼표로 연결

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                errorMessage
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * HttpMessageNotReadableException 처리
     * JSON 파싱 오류 (400 Bad Request)
     *
     * @param e HttpMessageNotReadableException 예외 객체
     * @return 400 상태 코드와 에러 메시지
     *
     * 발생 시점:
     * - 잘못된 JSON 형식
     * - 타입 불일치 (문자열에 숫자 입력 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonErrorDto> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                "요청 데이터 형식이 올바르지 않습니다." // 일반적인 JSON 오류 메시지
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * MissingServletRequestParameterException 처리
     * 필수 파라미터 누락 (400 Bad Request)
     *
     * @param e MissingServletRequestParameterException 예외 객체
     * @return 400 상태 코드와 에러 메시지
     *
     * 발생 시점:
     * - @RequestParam(required = true)인 파라미터가 누락된 경우
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonErrorDto> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("MissingServletRequestParameterException: {}", e.getMessage());

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                "필수 파라미터가 누락되었습니다: " + e.getParameterName()
                // 어떤 파라미터가 누락되었는지 명시
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * MethodArgumentTypeMismatchException 처리
     * 타입 불일치 (400 Bad Request)
     *
     * @param e MethodArgumentTypeMismatchException 예외 객체
     * @return 400 상태 코드와 에러 메시지
     *
     * 발생 시점:
     * - @PathVariable이나 @RequestParam의 타입 변환 실패
     * - 예: Long 타입인데 "abc" 문자열 전달
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonErrorDto> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.BAD_REQUEST,
                "파라미터 타입이 올바르지 않습니다: " + e.getName()
                // 어떤 파라미터의 타입이 잘못되었는지 명시
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * NullPointerException 처리
     * Null 참조 오류 (500 Internal Server Error)
     *
     * @param e NullPointerException 예외 객체
     * @return 500 상태 코드와 에러 메시지
     *
     * 주의:
     * - NullPointerException은 대부분 코드 버그
     * - 운영 환경에서 이 예외가 발생하면 즉시 수정 필요
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<CommonErrorDto> handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException occurred", e);
        // log.error(): 에러 수준의 로그 (스택 트레이스 포함)
        // 서버 오류이므로 error 레벨로 기록

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR, // 500 상태 코드
                "서버 내부 오류가 발생했습니다." // 구체적인 오류 내용은 클라이언트에 노출하지 않음
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    /**
     * 일반 Exception 처리
     * 예상하지 못한 모든 예외 (500 Internal Server Error)
     *
     * @param e Exception 예외 객체
     * @return 500 상태 코드와 에러 메시지
     *
     * 역할:
     * - 위에서 처리하지 못한 모든 예외를 여기서 처리
     * - 마지막 방어선 (Fallback Handler)
     * - 예상치 못한 오류로부터 애플리케이션 보호
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonErrorDto> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        // 예상치 못한 예외이므로 error 레벨로 스택 트레이스 전체 기록

        CommonErrorDto errorDto = new CommonErrorDto(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 내부 오류가 발생했습니다." // 보안을 위해 구체적인 오류 내용 숨김
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }

    // === 예외 처리 우선순위 ===
    // Spring은 @ExceptionHandler를 위에서 아래로 매칭
    // 더 구체적인 예외를 먼저 처리하고, 일반적인 예외는 나중에 처리
    // 예: EntityNotFoundException → RuntimeException → Exception 순서로 체크

    // === 장점 ===
    // 1. 중앙 집중식 예외 처리로 코드 중복 제거
    // 2. 일관된 에러 응답 형식
    // 3. 로깅을 통한 에러 추적 가능
    // 4. 클라이언트 친화적인 에러 메시지
    // 5. 보안: 내부 오류 상세 정보를 클라이언트에 노출하지 않음
}