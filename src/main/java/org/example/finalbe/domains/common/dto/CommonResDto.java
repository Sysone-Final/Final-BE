package org.example.finalbe.domains.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;

/**
 * 공통 성공 응답 DTO
 * 모든 API의 성공 응답을 표준화
 */
@Data
@NoArgsConstructor
public class CommonResDto {

    private int status_code; // HTTP 상태 코드
    private String status_message; // 성공 메시지
    private Object result = new ArrayList<>(); // 실제 응답 데이터

    /**
     * 생성자: HTTP 상태, 메시지, 결과 데이터를 받아 초기화
     */
    public CommonResDto(HttpStatus httpStatus, String message, Object result) {
        this.status_code = httpStatus.value();
        this.status_message = message;
        this.result = result;
    }
}