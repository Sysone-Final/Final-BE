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
     */
    public List<RackCardResponse> getRackCards(Long serverRoomId) {
        log.debug("Fetching rack cards for serverRoom: {}", serverRoomId);

        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

        log.debug("Found {} rack cards for serverRoom: {}", racks.size(), serverRoomId);

        return racks.stream()
                .map(RackCardResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 랙 통계 조회
     */
    public RackStatisticsResponse getRackStatistics(Long serverRoomId) {
        log.debug("Fetching rack statistics for serverRoom: {}", serverRoomId);

        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

        log.debug("Calculating statistics for {} racks in serverRoom: {}", racks.size(), serverRoomId);

        return RackStatisticsResponse.from(racks);
    }
}