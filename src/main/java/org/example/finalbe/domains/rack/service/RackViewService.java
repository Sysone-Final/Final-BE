package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.dto.RackCardResponse;
import org.example.finalbe.domains.rack.dto.RackStatisticsResponse;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 랙 뷰 & 통계 서비스
 * 대시보드 및 통계 분석용
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackViewService {

    private final RackRepository rackRepository;

    /**
     * 랙 카드 뷰 조회
     * 대시보드용 그리드 레이아웃에 표시할 랙 요약 정보
     */
    public List<RackCardResponse> getRackCards(Long dataCenterId) {
        log.debug("Fetching rack cards for datacenter: {}", dataCenterId);

        List<Rack> racks = rackRepository.findByDatacenterIdAndDelYn(dataCenterId, DelYN.N);

        log.debug("Found {} rack cards for datacenter: {}", racks.size(), dataCenterId);

        return racks.stream()
                .map(RackCardResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 랙 통계 조회
     * 전산실 내 모든 랙의 통계 데이터 집계
     *
     * 포함 정보:
     * - 기본 통계 (전체 랙 수, 활성 랙 수, 점검중 랙 수)
     * - 평균 사용률 및 전력 사용률
     * - 랙별 사용 데이터 (차트용)
     * - 부서별/담당자별 랙 분포
     */
    public RackStatisticsResponse getRackStatistics(Long dataCenterId) {
        log.debug("Fetching rack statistics for datacenter: {}", dataCenterId);

        List<Rack> racks = rackRepository.findByDatacenterIdAndDelYn(dataCenterId, DelYN.N);

        log.debug("Calculating statistics for {} racks in datacenter: {}", racks.size(), dataCenterId);

        return RackStatisticsResponse.from(racks);
    }
}