package org.example.finalbe.domains.rack.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.rack.dto.RackCardResponse;
import org.example.finalbe.domains.rack.dto.RackStatisticsResponse;
import org.example.finalbe.domains.rack.service.RackViewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 랙 뷰 및 통계 컨트롤러
 * 대시보드 및 통계 분석 API 제공
 */
@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
@Validated
public class RackViewController {

    private final RackViewService rackViewService;

    /**
     * 랙 카드 뷰 조회
     * GET /api/racks/datacenter/{dataCenterId}/cards
     *
     * @param dataCenterId 전산실 ID
     * @return 랙 카드 목록 (요약 정보)
     */
    @GetMapping("/datacenter/{dataCenterId}/cards")
    public ResponseEntity<CommonResDto> getRackCards(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {

        List<RackCardResponse> cards = rackViewService.getRackCards(dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 카드 뷰 조회 완료", cards));
    }

    /**
     * 랙 통계 조회
     * GET /api/racks/datacenter/{dataCenterId}/statistics
     *
     * @param dataCenterId 전산실 ID
     * @return 랙 통계 정보 (전체 개수, 평균 사용률, 상태별 분포 등)
     */
    @GetMapping("/datacenter/{dataCenterId}/statistics")
    public ResponseEntity<CommonResDto> getRackStatistics(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {

        RackStatisticsResponse statistics = rackViewService.getRackStatistics(dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 통계 조회 완료", statistics));
    }
}