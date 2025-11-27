// 작성자: 황요한
// 컨텍스트 스위치 추이 데이터를 담는 DTO

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
public class ContextSwitchPointDto {

    // 데이터 생성 시각
    private LocalDateTime timestamp;

    // 초당 컨텍스트 스위치 수
    private Long contextSwitchesPerSec;
}
