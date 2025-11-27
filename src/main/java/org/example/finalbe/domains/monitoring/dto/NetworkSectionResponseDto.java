/**
 * 작성자: 최산하
 * 네트워크 대시보드 섹션 전체 데이터를 담는 DTO
 */
package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkSectionResponseDto {

    // 현재 네트워크 상태(게이지)
    private NetworkCurrentStatsDto currentStats;

    // 트래픽 변화 추이
    private List<NetworkTrafficPointDto> trafficTrend;

    // RX/TX 사용률 추이
    private List<NetworkUsagePointDto> usageTrend;

    // 에러/드롭 패킷 추이
    private List<NetworkErrorPointDto> errorTrend;
}
