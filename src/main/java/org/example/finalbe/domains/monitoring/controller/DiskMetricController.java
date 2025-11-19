package org.example.finalbe.domains.monitoring.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.common.enumdir.AggregationLevel;
import org.example.finalbe.domains.monitoring.dto.DiskCurrentStatsBatchDto;
import org.example.finalbe.domains.monitoring.dto.DiskCurrentStatsDto;
import org.example.finalbe.domains.monitoring.dto.DiskSectionResponseDto;
import org.example.finalbe.domains.monitoring.service.DiskMetricService;
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
 * 디스크 메트릭 컨트롤러
 * 디스크 대시보드 데이터 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring/disk") // 경로 변경
@RequiredArgsConstructor
@Validated
public class DiskMetricController {

    private final DiskMetricService diskMetricService; // 서비스 주입 변경

    /**
     * 디스크 섹션 전체 데이터 조회
     * GET /api/monitoring/disk/section
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param aggregationLevel 집계 레벨
     * @return 디스크 섹션 데이터
     */
    @GetMapping("/section")
    public ResponseEntity<CommonResDto> getDiskSection(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        // 기본값 설정
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        if (startTime == null) {
            startTime = endTime.minusHours(1);
        }

        // 집계 레벨 자동 선택
        if (aggregationLevel == null) {
            // CpuMetricService의 헬퍼 메소드를 사용 (DiskMetricService가 주입받고 있음)
            aggregationLevel = diskMetricService.determineOptimalAggregationLevel(startTime, endTime);
        }

        DiskSectionResponseDto response = diskMetricService.getDiskSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "디스크 섹션 데이터 조회 완료",
                response
        ));
    }

    /**
     * 현재 디스크 상태만 조회 (게이지용)
     * GET /api/monitoring/disk/current
     *
     * @param equipmentId 장비 ID
     * @return 현재 디스크 상태
     */
    @GetMapping("/current")
    public ResponseEntity<CommonResDto> getCurrentDiskStats(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId) {

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(1);

        DiskCurrentStatsDto currentStats = diskMetricService.getCurrentDiskStats(
                equipmentId, startTime, endTime);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "현재 디스크 상태 조회 완료",
                currentStats
        ));
    }

    /**
     * 디스크 사용률 추이만 조회 (그래프 4.1)
     * GET /api/monitoring/disk/usage-trend
     */
    @GetMapping("/usage-trend")
    public ResponseEntity<CommonResDto> getDiskUsageTrend(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        if (startTime == null) {
            startTime = endTime.minusHours(1);
        }
        if (aggregationLevel == null) {
            aggregationLevel = diskMetricService.determineOptimalAggregationLevel(startTime, endTime);
        }

        DiskSectionResponseDto response = diskMetricService.getDiskSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "디스크 사용률 추이 조회 완료",
                response.getDiskUsageTrend()
        ));
    }

    /**
     * 디스크 I/O 추이만 조회 (그래프 4.2)
     * GET /api/monitoring/disk/io-trend
     */
    @GetMapping("/io-trend")
    public ResponseEntity<CommonResDto> getDiskIoTrend(
            @RequestParam @Min(value = 1, message = "유효하지 않은 장비 ID입니다.") Long equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) AggregationLevel aggregationLevel) {

        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        if (startTime == null) {
            startTime = endTime.minusHours(1);
        }
        if (aggregationLevel == null) {
            aggregationLevel = diskMetricService.determineOptimalAggregationLevel(startTime, endTime);
        }

        DiskSectionResponseDto response = diskMetricService.getDiskSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "디스크 I/O 추이 조회 완료",
                response.getDiskIoTrend()
        ));
    }

    /**
     * 여러 장비의 현재 디스크 상태 일괄 조회
     * GET /api/monitoring/disk/current/batch
     *
     * @param equipmentIds 장비 ID 리스트 (쉼표로 구분, 예: "1,2,3,4,5")
     * @return 각 장비별 현재 디스크 상태
     */
    @GetMapping("/current/batch")
    public ResponseEntity<CommonResDto> getCurrentDiskStatsBatch(
            @RequestParam @NotBlank(message = "장비 ID를 입력해주세요.") String equipmentIds) {

        log.info("📥 일괄 디스크 상태 조회 요청 - equipmentIds: {}", equipmentIds);

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

        DiskCurrentStatsBatchDto result = diskMetricService.getCurrentDiskStatsBatch(equipmentIdList);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                String.format("일괄 디스크 상태 조회 완료 (성공: %d, 실패: %d)",
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