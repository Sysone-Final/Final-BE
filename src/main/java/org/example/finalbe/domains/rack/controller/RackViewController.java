package org.example.finalbe.domains.rack.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.rack.dto.RackCardResponse;
import org.example.finalbe.domains.rack.dto.RackStatisticsResponse;
import org.example.finalbe.domains.rack.service.RackViewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 랙 뷰 & 통계 컨트롤러
 * 대시보드 및 통계 분석용
 */
@RestController
@RequestMapping("/racks")
@RequiredArgsConstructor
public class RackViewController {

    private final RackViewService rackViewService;

    @GetMapping("/datacenter/{dataCenterId}/cards")
    public ResponseEntity<CommonResDto> getRackCards(@PathVariable Long dataCenterId) {
        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        List<RackCardResponse> cards = rackViewService.getRackCards(dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 카드 뷰 조회 완료", cards));
    }

    @GetMapping("/datacenter/{dataCenterId}/statistics")
    public ResponseEntity<CommonResDto> getRackStatistics(@PathVariable Long dataCenterId) {
        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        RackStatisticsResponse statistics = rackViewService.getRackStatistics(dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 통계 조회 완료", statistics));
    }
}