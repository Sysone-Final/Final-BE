// 작성자: 황요한
// CPU 대시보드/모니터링 API 제공 (사용률 추이, 부하 추이, 현재 상태, 일괄 조회 등)

package org.example.finalbe.domains.monitoring.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.common.enumdir.AggregationLevel;
import org.example.finalbe.domains.monitoring.dto.CpuCurrentStatsBatchDto;
import org.example.finalbe.domains.monitoring.dto.CpuCurrentStatsDto;

import org.example.finalbe.domains.monitoring.dto.CpuSectionResponseDto;
import org.example.finalbe.domains.monitoring.service.CpuMetricService;
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
@RequestMapping("/api/monitoring/cpu")
@RequiredArgsConstructor
@Validated
public class CpuMetricController {

    private final CpuMetricService cpuMetricService;

    /**
     * CPU 섹션 전체 데이터 조회
     */
    @GetMapping("/section")
    public ResponseEntity<CommonResDto> getCpuSection(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);
        if (aggregationLevel == null)
            aggregationLevel = cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);

        CpuSectionResponseDto response = cpuMetricService.getCpuSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "CPU 섹션 데이터 조회 완료",
                response
        ));
    }

    /**
     * 현재 CPU 상태 조회
     */
    @GetMapping("/current")
    public ResponseEntity<CommonResDto> getCurrentCpuStats(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId) {

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(1);

        CpuCurrentStatsDto currentStats = cpuMetricService.getCurrentCpuStats(
                equipmentId, startTime, endTime);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "현재 CPU 상태 조회 완료",
                currentStats
        ));
    }

    /**
     * CPU 사용률 추이 조회
     */
    @GetMapping("/usage-trend")
    public ResponseEntity<CommonResDto> getCpuUsageTrend(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);
        if (aggregationLevel == null)
            aggregationLevel = cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);

        CpuSectionResponseDto response = cpuMetricService.getCpuSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "CPU 사용률 추이 조회 완료",
                response.getCpuUsageTrend()
        ));
    }

    /**
     * Load Average(시스템 부하) 추이 조회
     */
    @GetMapping("/load-average")
    public ResponseEntity<CommonResDto> getLoadAverageTrend(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) endTime = LocalDateTime.now();
        if (startTime == null) startTime = endTime.minusHours(1);
        if (aggregationLevel == null)
            aggregationLevel = cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);

        CpuSectionResponseDto response = cpuMetricService.getCpuSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "시스템 부하 추이 조회 완료",
                response.getLoadAverageTrend()
        ));
    }

    /**
     * 여러 장비의 현재 CPU 상태 일괄 조회
     */
    @GetMapping("/current/batch")
    public ResponseEntity<CommonResDto> getCurrentCpuStatsBatch(
            @RequestParam @NotBlank(message = "장비 ID를 입력해주세요.") String equipmentIds) {

        log.info("📥 일괄 CPU 상태 조회 요청 - equipmentIds: {}", equipmentIds);

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
                    "한 번에 최대 50개의 장비만 조회 가능합니다.",
                    null
            ));
        }

        equipmentIdList = equipmentIdList.stream().distinct().collect(Collectors.toList());

        CpuCurrentStatsBatchDto result = cpuMetricService.getCurrentCpuStatsBatch(equipmentIdList);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                String.format("일괄 CPU 상태 조회 완료 (성공: %d, 실패: %d)",
                        result.getSuccessCount(), result.getFailureCount()),
                result
        ));
    }

    /**
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
