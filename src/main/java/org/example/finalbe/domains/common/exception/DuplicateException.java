package org.example.finalbe.domains.common.exception;

/**
 * 중복된 데이터가 존재할 때 발생하는 예외
 * HTTP 상태 코드: 409 Conflict
 *
 * 사용 시점:
 * - 회원가입 시 이미 존재하는 아이디
 * - 이미 존재하는 이메일
 * - UNIQUE 제약조건 위반
 */
public class DuplicateException extends RuntimeException {

    /**
     * 필드 이름과 값을 받아 메시지 생성
     *
     * @param fieldName 중복된 필드 이름 (예: "아이디", "이메일")
     * @param value 중복된 값
     *
     * 사용 예시:
     * throw new DuplicateException("아이디", "user123");
     * → "이미 존재하는 아이디입니다: user123"
     */
    public DuplicateException(String fieldName, String value) {
        super("이미 존재하는 " + fieldName + "입니다: " + value);
        // 사용자가 어떤 값이 중복되었는지 명확히 알 수 있도록 메시지 생성
    }

    /**
     * 직접 메시지를 받는 생성자
     *
     * @param message 에러 메시지
     *
     * 사용 예시:
     * throw new DuplicateException("이미 등록된 회사입니다.");
     */
    public DuplicateException(String message) {
        super(message);
    }

    // === 사용 예시 ===
    // if (memberRepository.existsByUserName(userName)) {
    //     throw new DuplicateException("아이디", userName);
    // }

    // === GlobalExceptionHandler에서 처리 ===
    // @ExceptionHandler(DuplicateException.class)
    // public ResponseEntity<CommonErrorDto> handleDuplicateException(DuplicateException e) {
    //     return ResponseEntity.status(409).body(new CommonErrorDto(HttpStatus.CONFLICT, e.getMessage()));
    // }
}