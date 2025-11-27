/**
 * 작성자: 황요한
 * 랙 카드 뷰 및 통계 조회 서비스
 */
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackViewService {

    private final RackRepository rackRepository;

    // 랙 카드 뷰 조회
    public List<RackCardResponse> getRackCards(Long serverRoomId) {
        log.debug("Fetching rack cards for serverRoom: {}", serverRoomId);

        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);
        return racks.stream()
                .map(RackCardResponse::from)
                .toList();
    }

    // 랙 통계 조회
    public RackStatisticsResponse getRackStatistics(Long serverRoomId) {
        log.debug("Fetching rack statistics for serverRoom: {}", serverRoomId);

        List<Rack> racks = rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);
        return RackStatisticsResponse.from(racks);
    }
}
