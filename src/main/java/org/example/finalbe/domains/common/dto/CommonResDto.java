/**
 * 작성자: 황요한
 * API 공통 성공 응답 DTO
 */
package org.example.finalbe.domains.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;

@Data
@NoArgsConstructor
public class CommonResDto {

    private int status_code;          // HTTP 상태 코드
    private String status_message;    // 응답 메시지
    private Object result = new ArrayList<>(); // 응답 데이터

    public CommonResDto(HttpStatus status, String message, Object result) {
        this.status_code = status.value();
        this.status_message = message;
        this.result = result;
    }
}
