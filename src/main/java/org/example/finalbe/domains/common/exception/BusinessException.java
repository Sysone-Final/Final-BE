package org.example.finalbe.domains.common.exception;

/**
 * 비즈니스 로직에서 발생하는 일반적인 예외
 * HTTP 상태 코드: 400 Bad Request
 *
 * 사용 시점:
 * - 비즈니스 규칙 위반
 * - 상태 전이 불가능
 * - 조건 불만족
 * - 기타 비즈니스 로직 오류
 *
 * 예시:
 * - 랙에 장비가 있어서 삭제 불가
 * - 부서에 회원이 있어서 삭제 불가
 * - 전산실의 최대 랙 수 초과
 * - 이미 처리된 요청
 */
public class BusinessException extends RuntimeException {

    /**
     * 에러 메시지를 받는 생성자
     *
     * @param message 비즈니스 로직 오류 메시지
     *
     * 사용 예시:
     * throw new BusinessException("랙에 장비가 존재하여 삭제할 수 없습니다.");
     */
    public BusinessException(String message) {
        super(message);
        // 비즈니스 규칙 위반에 대한 명확한 메시지 전달
    }

    // === 사용 예시 ===

    // 예시 1: 삭제 전 의존 관계 체크
    // Long memberCount = memberDepartmentRepository.countByDepartmentId(departmentId);
    // if (memberCount > 0) {
    //     throw new BusinessException("부서에 소속된 회원이 있어 삭제할 수 없습니다.");
    // }

    // 예시 2: 용량 체크
    // if (dataCenter.getCurrentRackCount() >= dataCenter.getMaxRackCount()) {
    //     throw new BusinessException("전산실의 최대 랙 수를 초과했습니다.");
    // }

    // 예시 3: 상태 전이 체크
    // if (equipment.getStatus() == EquipmentStatus.DECOMMISSIONED) {
    //     throw new BusinessException("폐기된 장비는 수정할 수 없습니다.");
    // }

    // 예시 4: 중복 작업 방지
    // if (orderRepository.existsByOrderNumberAndStatus(orderNumber, OrderStatus.COMPLETED)) {
    //     throw new BusinessException("이미 완료된 주문입니다.");
    // }

    // === GlobalExceptionHandler에서 처리 ===
    // @ExceptionHandler(BusinessException.class)
    // public ResponseEntity<CommonErrorDto> handleBusinessException(BusinessException e) {
    //     return ResponseEntity.status(400)
    //         .body(new CommonErrorDto(HttpStatus.BAD_REQUEST, e.getMessage()));
    // }
}