package org.example.finalbe.domains.monitoring.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.common.enumdir.AggregationLevel;
import org.example.finalbe.domains.monitoring.dto.EnvironmentCurrentStatsBatchDto;
import org.example.finalbe.domains.monitoring.dto.EnvironmentCurrentStatsDto;
import org.example.finalbe.domains.monitoring.dto.EnvironmentSectionResponseDto;
import org.example.finalbe.domains.monitoring.service.EnvironmentMetricService;
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
 * 환경 메트릭 컨트롤러
 * 환경(온도/습도) 대시보드 데이터 API 제공 (랙 기준)
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring/environment") // 경로 변경
@RequiredArgsConstructor
@Validated
public class EnvironmentMetricController {

    private final EnvironmentMetricService environmentMetricService; // 서비스 주입 변경

    /**
     * 환경 섹션 전체 데이터 조회
     * GET /api/monitoring/environment/section
     *
     * @param rackId 랙 ID (equipmentId 아님)
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param aggregationLevel 집계 레벨
     * @return 환경 섹션 데이터
     */
    @GetMapping("/section")
    public ResponseEntity<CommonResDto> getEnvironmentSection(
            @RequestParam @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId, // 파라미터 이름 변경
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);

        if (aggregationLevel == null) {
            aggregationLevel = environmentMetricService.determineOptimalAggregationLevel(startTime, endTime);
        }

        EnvironmentSectionResponseDto response = environmentMetricService.getEnvironmentSectionData(
                rackId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "환경 섹션 데이터 조회 완료",
                response
        ));
    }

    /**
     * 현재 환경 상태만 조회 (게이지용)
     * GET /api/monitoring/environment/current
     *
     * @param rackId 랙 ID
     * @return 현재 환경 상태
     */
    @GetMapping("/current")
    public ResponseEntity<CommonResDto> getCurrentEnvironmentStats(
            @RequestParam @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId) { // 파라미터 이름 변경

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(1);

        EnvironmentCurrentStatsDto currentStats = environmentMetricService.getCurrentEnvironmentStats(
                rackId, startTime, endTime);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "현재 환경 상태 조회 완료",
                currentStats
        ));
    }

    /**
     * 온도 추이만 조회
     * GET /api/monitoring/environment/temperature-trend
     */
    @GetMapping("/temperature-trend")
    public ResponseEntity<CommonResDto> getTemperatureTrend(
            @RequestParam @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);
        if (aggregationLevel == null)
            aggregationLevel = environmentMetricService.determineOptimalAggregationLevel(startTime, endTime);

        EnvironmentSectionResponseDto response = environmentMetricService.getEnvironmentSectionData(
                rackId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "온도 추이 조회 완료",
                response.getTemperatureTrend()
        ));
    }

    /**
     * 습도 추이만 조회
     * GET /api/monitoring/environment/humidity-trend
     */
    @GetMapping("/humidity-trend")
    public ResponseEntity<CommonResDto> getHumidityTrend(
            @RequestParam @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);
        if (aggregationLevel == null)
            aggregationLevel = environmentMetricService.determineOptimalAggregationLevel(startTime, endTime);

        EnvironmentSectionResponseDto response = environmentMetricService.getEnvironmentSectionData(
                rackId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "습도 추이 조회 완료",
                response.getHumidityTrend()
        ));
    }

    /**
     * 여러 랙의 현재 환경 상태 일괄 조회
     * GET /api/monitoring/environment/current/batch
     *
     * @param rackIds 랙 ID 리스트 (쉼표로 구분)
     * @return 각 랙별 현재 환경 상태
     */
    @GetMapping("/current/batch")
    public ResponseEntity<CommonResDto> getCurrentEnvironmentStatsBatch(
            @RequestParam @NotBlank(message = "랙 ID를 입력해주세요.") String rackIds) { // 파라미터 이름 변경

        log.info("📥 일괄 환경 상태 조회 요청 - rackIds: {}", rackIds);

        List<Long> rackIdList;
        try {
            rackIdList = parseRackIds(rackIds); // 헬퍼 메소드 이름 변경
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CommonResDto(
                    HttpStatus.BAD_REQUEST,
                    "잘못된 랙 ID 형식입니다: " + e.getMessage(),
                    null
            ));
        }

        if (rackIdList.size() > 50) {
            return ResponseEntity.badRequest().body(new CommonResDto(
                    HttpStatus.BAD_REQUEST,
                    "한 번에 최대 50개의 랙만 조회 가능합니다. (요청: " + rackIdList.size() + "개)",
                    null
            ));
        }

        rackIdList = rackIdList.stream()
                .distinct()
                .collect(Collectors.toList());

        EnvironmentCurrentStatsBatchDto result = environmentMetricService.getCurrentEnvironmentStatsBatch(rackIdList);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                String.format("일괄 환경 상태 조회 완료 (성공: %d, 실패: %d)",
                        result.getSuccessCount(), result.getFailureCount()),
                result
        ));
    }

    /**
     * rackIds 문자열 파싱
     */
    private List<Long> parseRackIds(String rackIds) { // 메소드 이름 변경
        if (rackIds == null || rackIds.trim().isEmpty()) {
            throw new IllegalArgumentException("랙 ID가 비어있습니다.");
        }

        try {
            return Arrays.stream(rackIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("숫자가 아닌 값이 포함되어 있습니다: " + e.getMessage());
        }
    }
}