// 작성자: 최산하
// 디스크 I/O 속도 시계열 포인트 DTO

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
public class DiskIoPointDto {

    // 데이터 시간
    private LocalDateTime timestamp;

    // 읽기 속도 (bytes/sec)
    private Double readBps;

    // 쓰기 속도 (bytes/sec)
    private Double writeBps;
}
