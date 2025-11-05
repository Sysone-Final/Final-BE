package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 컨텍스트 스위치 포인트 DTO
 * 그래프 1.4: 컨텍스트 스위치 추이 (라인 차트)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextSwitchPointDto {

    private LocalDateTime timestamp;

    /**
     * 초당 컨텍스트 스위치 횟수
     */
    private Long contextSwitchesPerSec;
}