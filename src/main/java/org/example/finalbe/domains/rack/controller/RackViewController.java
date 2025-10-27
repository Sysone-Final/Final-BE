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
 * 랙 뷰 & 통계 컨트롤러
 * 대시보드 및 통계 분석용
 */
@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
@Validated
public class RackViewController {

    private final RackViewService rackViewService;

    /**
     * 데이터센터의 랙들을 카드 형태로 조회하는 기능
     * 각 랙의 요약 정보를 카드 뷰 형식으로 제공
     * 대시보드 화면에서 여러 랙을 한눈에 보기 좋게 표시할 때 사용
     * 예: 랙 이름, 사용률, 상태 등을 카드로 나열
     * 권한: 모든 사용자 접근 가능
     *
     * @param dataCenterId 전산실 ID (1 이상의 양수)
     */
    @GetMapping("/datacenter/{dataCenterId}/cards")
    public ResponseEntity<CommonResDto> getRackCards(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {

        List<RackCardResponse> cards = rackViewService.getRackCards(dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 카드 뷰 조회 완료", cards));
    }

    /**
     * 데이터센터의 랙 관련 통계 정보를 조회하는 기능
     * 전체 랙 개수, 평균 사용률, 상태별 랙 분포 등의 통계 데이터 제공
     * 대시보드에서 전체적인 현황 파악에 활용
     * 예: 총 50개 랙, 평균 사용률 65%, 정상 운영 중 40개, 점검 중 10개
     * 권한: 모든 사용자 접근 가능
     *
     * @param dataCenterId 전산실 ID (1 이상의 양수)
     */
    @GetMapping("/datacenter/{dataCenterId}/statistics")
    public ResponseEntity<CommonResDto> getRackStatistics(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {

        RackStatisticsResponse statistics = rackViewService.getRackStatistics(dataCenterId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 통계 조회 완료", statistics));
    }
}