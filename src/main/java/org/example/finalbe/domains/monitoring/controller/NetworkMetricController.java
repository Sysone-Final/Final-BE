package org.example.finalbe.domains.monitoring.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.common.enumdir.AggregationLevel;
import org.example.finalbe.domains.monitoring.dto.NetworkCurrentStatsBatchDto;
import org.example.finalbe.domains.monitoring.dto.NetworkCurrentStatsDto;
import org.example.finalbe.domains.monitoring.dto.NetworkSectionResponseDto;
import org.example.finalbe.domains.monitoring.service.NetworkMetricService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 네트워크 메트릭 컨트롤러
 * 네트워크 대시보드 데이터 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring/network") // 경로 변경
@RequiredArgsConstructor
@Validated
public class NetworkMetricController {

    private final NetworkMetricService networkMetricService; // 서비스 주입 변경

    /**
     * 네트워크 섹션 전체 데이터 조회
     * GET /api/monitoring/network/section
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param aggregationLevel 집계 레벨
     * @return 네트워크 섹션 데이터
     */
    @GetMapping("/section")
    public ResponseEntity<CommonResDto> getNetworkSection(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);

        if (aggregationLevel == null) {
            aggregationLevel = networkMetricService.determineOptimalAggregationLevel(startTime, endTime);
        }

        NetworkSectionResponseDto response = networkMetricService.getNetworkSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "네트워크 섹션 데이터 조회 완료",
                response
        ));
    }

    /**
     * 현재 네트워크 상태만 조회 (게이지용)
     * GET /api/monitoring/network/current
     *
     * @param equipmentId 장비 ID
     * @return 현재 네트워크 상태
     */
    @GetMapping("/current")
    public ResponseEntity<CommonResDto> getCurrentNetworkStats(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId) {

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(1);

        NetworkCurrentStatsDto currentStats = networkMetricService.getCurrentNetworkStats(
                equipmentId, startTime, endTime);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "현재 네트워크 상태 조회 완료",
                currentStats
        ));
    }

    /**
     * 네트워크 트래픽 추이만 조회 (그래프 3.7)
     * GET /api/monitoring/network/traffic-trend
     */
    @GetMapping("/traffic-trend")
    public ResponseEntity<CommonResDto> getNetworkTrafficTrend(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);
        if (aggregationLevel == null)
            aggregationLevel = networkMetricService.determineOptimalAggregationLevel(startTime, endTime);

        NetworkSectionResponseDto response = networkMetricService.getNetworkSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "네트워크 트래픽 추이 조회 완료",
                response.getTrafficTrend()
        ));
    }

    /**
     * 네트워크 사용률 추이만 조회 (그래프 3.1, 3.2)
     * GET /api/monitoring/network/usage-trend
     */
    @GetMapping("/usage-trend")
    public ResponseEntity<CommonResDto> getNetworkUsageTrend(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);
        if (aggregationLevel == null)
            aggregationLevel = networkMetricService.determineOptimalAggregationLevel(startTime, endTime);

        NetworkSectionResponseDto response = networkMetricService.getNetworkSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "네트워크 사용률 추이 조회 완료",
                response.getUsageTrend()
        ));
    }


    /**
     * 여러 장비의 현재 네트워크 상태 일괄 조회
     * GET /api/monitoring/network/current/batch
     *
     * @param equipmentIds 장비 ID 리스트 (쉼표로 구분)
     * @return 각 장비별 현재 네트워크 상태
     */
    @GetMapping("/current/batch")
    public ResponseEntity<CommonResDto> getCurrentNetworkStatsBatch(
            @RequestParam @NotBlank(message = "장비 ID를 입력해주세요.") String equipmentIds) {

        log.info("📥 일괄 네트워크 상태 조회 요청 - equipmentIds: {}", equipmentIds);

        List<Long> equipmentIdList;
        try {
            equipmentIdList = parseEquipmentIds(equipmentIds);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CommonResDto(
                    HttpStatus.BAD_REQUEST,
                    "잘못된 장비 ID 형식입니다: " + e.getMessage(),
                    null
            ));
        }

        if (equipmentIdList.size() > 50) {
            return ResponseEntity.badRequest().body(new CommonResDto(
                    HttpStatus.BAD_REQUEST,
                    "한 번에 최대 50개의 장비만 조회 가능합니다. (요청: " + equipmentIdList.size() + "개)",
                    null
            ));
        }

        equipmentIdList = equipmentIdList.stream()
                .distinct()
                .collect(Collectors.toList());

        NetworkCurrentStatsBatchDto result = networkMetricService.getCurrentNetworkStatsBatch(equipmentIdList);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                String.format("일괄 네트워크 상태 조회 완료 (성공: %d, 실패: %d)",
                        result.getSuccessCount(), result.getFailureCount()),
                result
        ));
    }

    /**
     * (CPU 컨트롤러에서 복사)
     * equipmentIds 문자열 파싱
     */
    private List<Long> parseEquipmentIds(String equipmentIds) {
        if (equipmentIds == null || equipmentIds.trim().isEmpty()) {
            throw new IllegalArgumentException("장비 ID가 비어있습니다.");
        }

        try {
            return Arrays.stream(equipmentIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("숫자가 아닌 값이 포함되어 있습니다: " + e.getMessage());
        }
    }
}