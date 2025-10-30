package org.example.finalbe.domains.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 공통 에러 응답 DTO
 * 모든 API의 에러 응답을 표준화
 */
@Data
@NoArgsConstructor
public class CommonErrorDto {

    private int status_code; // HTTP 에러 상태 코드
    private String status_message; // 에러 메시지

    /**
     * 생성자: HTTP 상태와 에러 메시지를 받아 초기화
     */
    public CommonErrorDto(HttpStatus httpStatus, String status_message) {
        this.status_code = httpStatus.value();
        this.status_message = status_message;
    }
}