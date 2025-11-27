/**
 * 작성자: 황요한
 * 공통 에러 응답 DTO
 */
package org.example.finalbe.domains.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
public class CommonErrorDto {

    private int status_code;
    private String status_message;

    /**
     * 생성자: HTTP 상태와 에러 메시지를 받아 초기화
     */
    public CommonErrorDto(HttpStatus httpStatus, String status_message) {
        this.status_code = httpStatus.value();
        this.status_message = status_message;
    }
}