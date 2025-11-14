package org.example.finalbe.domains.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 네트워크 섹션 전체 응답 DTO
 * 네트워크 대시보드 섹션의 모든 그래프 데이터 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkSectionResponseDto {

    /**
     * 현재 네트워크 상태 (게이지 및 요약 정보)
     */
    private NetworkCurrentStatsDto currentStats;

    /**
     * 그래프 3.7: 초당 전송량 (트래픽) 추이
     */
    private List<NetworkTrafficPointDto> trafficTrend;

    /**
     * 그래프 3.1, 3.2: RX/TX 사용률 추이
     */
    private List<NetworkUsagePointDto> usageTrend;

    /**
     * 그래프 3.8: 에러/드롭 패킷 추이
     */
    private List<NetworkErrorPointDto> errorTrend;

    // 참고: 3.3~3.6(누적 패킷/바이트) 그래프는 3.8(에러)과 유사하게
    // NetworkErrorPointDto처럼 DTO를 만들어 Raw 데이터에서 매핑해주면 됩니다.
    // 여기서는 3개의 핵심 그래프만 구현합니다.
}