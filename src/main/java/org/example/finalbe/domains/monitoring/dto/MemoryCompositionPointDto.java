// 작성자: 최산하
// 메모리 구성 요소 시계열 포인트 DTO

package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryCompositionPointDto {

    // 시점
    private LocalDateTime timestamp;

    // 활성 메모리
    private Long active;

    // 비활성 메모리
    private Long inactive;

    // 버퍼 메모리
    private Long buffers;

    // 캐시 메모리
    private Long cached;

    // 자유 메모리
    private Long free;
}
