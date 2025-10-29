package org.example.finalbe.domains.common.exception;

/**
 * 엔티티를 찾을 수 없을 때 발생하는 예외
 * HTTP 상태 코드: 404 Not Found
 *
 * 사용 시점:
 * - ID로 조회했는데 데이터가 없는 경우
 * - 삭제된 데이터를 조회하려는 경우
 * - 존재하지 않는 리소스에 접근하는 경우
 */
public class EntityNotFoundException extends RuntimeException {
    // RuntimeException: Unchecked Exception (컴파일러가 예외 처리를 강제하지 않음)
    // Checked Exception과 달리 throws 선언이나 try-catch 불필요
    // Spring의 @Transactional에서 RuntimeException은 자동으로 롤백

    /**
     * 엔티티 타입과 ID를 받아 메시지 생성
     *
     * @param entityName 엔티티 이름 (예: "회원", "회사", "전산실")
     * @param id 조회하려던 ID
     *
     * 사용 예시:
     * throw new EntityNotFoundException("회원", 123L);
     * → "회원을 찾을 수 없습니다. ID: 123"
     */
    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + "을(를) 찾을 수 없습니다. ID: " + id);
        // super(): 부모 클래스(RuntimeException)의 생성자 호출
        // RuntimeException의 생성자는 메시지를 받아 저장하고, getMessage()로 조회 가능
    }

    /**
     * 직접 메시지를 받는 생성자
     *
     * @param message 에러 메시지
     *
     * 사용 예시:
     * throw new EntityNotFoundException("해당 전산실을 찾을 수 없습니다.");
     */
    public EntityNotFoundException(String message) {
        super(message); // 메시지를 그대로 사용
    }

    // === GlobalExceptionHandler에서 처리 ===
    // @ExceptionHandler(EntityNotFoundException.class)
    // public ResponseEntity<CommonErrorDto> handleEntityNotFoundException(EntityNotFoundException e) {
    //     return ResponseEntity.status(404).body(new CommonErrorDto(HttpStatus.NOT_FOUND, e.getMessage()));
    // }
}