package org.example.finalbe.domains.common.enumdir;

/**
 * 히스토리 작업 타입
 */
public enum HistoryAction {
    CREATE,         // 생성
    UPDATE,         // 수정
    DELETE,         // 삭제
    STATUS_CHANGE,  // 상태 변경
    MOVE,           // 이동 (장비/장치 위치 변경)
    BULK_UPLOAD     // 대량 업로드 (엑셀 등)
}