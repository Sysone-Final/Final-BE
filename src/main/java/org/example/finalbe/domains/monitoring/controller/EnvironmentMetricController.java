// 작성자: 최산하
// 환경 모니터링 API 제공 (온도/습도 섹션, 추이, 현재 상태, 일괄 조회)

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


@Slf4j
@RestController
@RequestMapping("/api/monitoring/environment")
@RequiredArgsConstructor
@Validated
public class EnvironmentMetricController {

    private final EnvironmentMetricService environmentMetricService;

    /**
     * 환경 섹션 전체 데이터 조회
     */
    @GetMapping("/section")
    public ResponseEntity<CommonResDto> getEnvironmentSection(
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
                "환경 섹션 데이터 조회 완료",
                response
        ));
    }

    /**
     * 현재 환경 상태 조회
     */
    @GetMapping("/current")
    public ResponseEntity<CommonResDto> getCurrentEnvironmentStats(
            @RequestParam @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") Long rackId) {

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
     * 온도 추이 조회
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
     * 습도 추이 조회
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
     */
    @GetMapping("/current/batch")
    public ResponseEntity<CommonResDto> getCurrentEnvironmentStatsBatch(
            @RequestParam @NotBlank(message = "랙 ID를 입력해주세요.") String rackIds) {

        log.info("📥 일괄 환경 상태 조회 요청 - rackIds: {}", rackIds);

        List<Long> rackIdList;
        try {
            rackIdList = parseRackIds(rackIds);
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
                    "한 번에 최대 50개의 랙만 조회 가능합니다.",
                    null
            ));
        }

        rackIdList = rackIdList.stream()
                .distinct()
                .collect(Collectors.toList());

        EnvironmentCurrentStatsBatchDto result =
                environmentMetricService.getCurrentEnvironmentStatsBatch(rackIdList);

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
    private List<Long> parseRackIds(String rackIds) {
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
