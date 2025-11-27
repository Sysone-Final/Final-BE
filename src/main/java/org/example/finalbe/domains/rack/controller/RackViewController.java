/**
 * 작성자: 황요한
 * 랙 카드 뷰 및 통계 정보를 제공하는 컨트롤러
 */
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

@RestController
@RequestMapping("/api/racks")
@RequiredArgsConstructor
@Validated
public class RackViewController {

    private final RackViewService rackViewService;

    // 랙 카드 목록 조회
    @GetMapping("/serverroom/{serverRoomId}/cards")
    public ResponseEntity<CommonResDto> getRackCards(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        List<RackCardResponse> cards = rackViewService.getRackCards(serverRoomId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 카드 조회 완료", cards));
    }

    // 랙 통계 조회
    @GetMapping("/serverroom/{serverRoomId}/statistics")
    public ResponseEntity<CommonResDto> getRackStatistics(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        RackStatisticsResponse statistics = rackViewService.getRackStatistics(serverRoomId);
        return ResponseEntity.ok(new CommonResDto(HttpStatus.OK, "랙 통계 조회 완료", statistics));
    }
}
