package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메모리 구성 요소 포인트 DTO
 * 그래프 2.2: 메모리 구성 (적층 영역 차트)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryCompositionPointDto {

    private LocalDateTime timestamp;
    private Long active;
    private Long inactive;
    private Long buffers;
    private Long cached;
    private Long free; // '사용됨'이 아닌 '구성'을 보여주기 위함
}