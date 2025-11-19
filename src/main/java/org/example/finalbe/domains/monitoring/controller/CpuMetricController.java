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

/**
 * CPU 메트릭 컨트롤러
 * CPU 대시보드 데이터 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring/cpu")
@RequiredArgsConstructor
@Validated
public class CpuMetricController {

    private final CpuMetricService cpuMetricService;

    /**
     * CPU 섹션 전체 데이터 조회
     * GET /api/monitoring/cpu/section
     *
     * 1. CPU 사용률 추이 - 서버가 시간별로 얼마나 바쁜지 (0~100%)
     * 2. CPU 모드별 분포 - CPU가 무슨 일(사용자 프로그램, 시스템 작업, 대기 등)로 바쁜지
     * 3. 시스템 부하 - 처리해야 할 작업이 얼마나 쌓여있는지 (대기 중인 일의 양)
     * 4. 컨텍스트 스위치 - CPU가 작업을 얼마나 자주 전환하는지 (초당 전환 횟수)
     * 5. 현재 상태 - 지금 이 순간의 CPU 사용률과 최근 통계(평균/최대/최소)
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간 (선택, 기본값: 1시간 전)
     * @param endTime 종료 시간 (선택, 기본값: 현재)
     * @param aggregationLevel 집계 레벨 (선택, 기본값: 자동 선택)
     * @return CPU 섹션 데이터 (4개 그래프 + 현재 상태)
     */
    @GetMapping("/section")
    public ResponseEntity<CommonResDto> getCpuSection(
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
            startTime = endTime.minusHours(1);  // 기본 1시간
        }

        // 집계 레벨 자동 선택
        if (aggregationLevel == null) {
            aggregationLevel = cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
        }

        CpuSectionResponseDto response = cpuMetricService.getCpuSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "CPU 섹션 데이터 조회 완료",
                response
        ));
    }

    /**
     * 현재 CPU 상태만 조회 (게이지용)
     * GET /api/monitoring/cpu/current
     *
     * - 현재 CPU 사용률: 지금 이 순간 서버가 얼마나 바쁜지 (실시간 값)
     * - 평균 CPU 사용률: 최근 1시간 동안 평균적으로 얼마나 바빴는지
     * - 최대 CPU 사용률: 최근 1시간 중 가장 바빴던 순간의 값
     * - 최소 CPU 사용률: 최근 1시간 중 가장 한가했던 순간의 값
     *
     * 예시: 식당의 현재 테이블 사용률과 오늘 평균/최대/최소 테이블 사용률
     *
     * @param equipmentId 장비 ID
     * @return 현재 CPU 상태
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
     * CPU 사용률 추이만 조회 (그래프 1.1)
     * GET /api/monitoring/cpu/usage-trend
     *
     * 보여주는 통계:
     * - CPU 사용률 추이: 시간대별로 서버가 얼마나 바빴는지 변화 추이
     * - 0%에 가까우면 거의 쉬는 상태, 100%에 가까우면 한계치에 도달한 상태
     *
     * 예시: 식당의 시간대별 테이블 점유율 그래프
     *         (점심시간에는 90%, 오후 3시에는 20% 이런 식으로)
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param aggregationLevel 집계 레벨
     * @return CPU 사용률 추이 데이터
     */
    @GetMapping("/usage-trend")
    public ResponseEntity<CommonResDto> getCpuUsageTrend(
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
            aggregationLevel = cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
        }

        CpuSectionResponseDto response = cpuMetricService.getCpuSectionData(
                equipmentId, startTime, endTime, aggregationLevel);

        return ResponseEntity.ok(new CommonResDto(
                HttpStatus.OK,
                "CPU 사용률 추이 조회 완료",
                response.getCpuUsageTrend()
        ));
    }

    /**
     * 시스템 부하 추이만 조회 (그래프 1.3)
     * GET /api/monitoring/cpu/load-average
     *
     * 보여주는 통계:
     * - 시스템 부하(Load Average): CPU가 처리해야 할 작업이 얼마나 대기 중인지
     * - 1분/5분/15분 평균으로 최근 부하 상태와 추세를 함께 파악
     * - CPU 코어 수와 비교하여 과부하 여부 판단 가능
     *   (예: 4코어 서버에서 Load Average가 8이면 작업이 2배 밀려있는 상태)
     *
     * 예시: 은행 창구 4개인데 대기 손님이 평균 10명이면 과부하
     *         창구 수보다 대기자가 많으면 고객들이 오래 기다려야 함
     *
     * @param equipmentId 장비 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param aggregationLevel 집계 레벨
     * @return 시스템 부하 추이 데이터
     */
    @GetMapping("/load-average")
    public ResponseEntity<CommonResDto> getLoadAverageTrend(
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
            aggregationLevel = cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
        }

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
     * GET /api/monitoring/cpu/current/batch
     *
     * 보여주는 통계:
     * - 여러 서버의 CPU 상태를 한 번에 조회
     * - 각 장비별로 현재 사용률, 평균/최대/최소 값 제공
     * - 전체 서버 목록을 대시보드에 표시할 때 사용
     *
     * 예시: 체인점 여러 개의 테이블 사용률을 한눈에 보는 것
     *         (강남점 80%, 홍대점 45%, 신촌점 92% 이런 식으로)
     *
     * @param equipmentIds 장비 ID 리스트 (쉼표로 구분, 예: "1,2,3,4,5")
     * @return 각 장비별 현재 CPU 상태
     */
    @GetMapping("/current/batch")
    public ResponseEntity<CommonResDto> getCurrentCpuStatsBatch(
            @RequestParam @NotBlank(message = "장비 ID를 입력해주세요.") String equipmentIds) {

        log.info("📥 일괄 CPU 상태 조회 요청 - equipmentIds: {}", equipmentIds);

        // 1. 파라미터 파싱 및 검증
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

        // 2. 최대 조회 개수 제한 (성능 보호)
        if (equipmentIdList.size() > 50) {
            return ResponseEntity.badRequest().body(new CommonResDto(
                    HttpStatus.BAD_REQUEST,
                    "한 번에 최대 50개의 장비만 조회 가능합니다. (요청: " + equipmentIdList.size() + "개)",
                    null
            ));
        }

        // 3. 중복 제거
        equipmentIdList = equipmentIdList.stream()
                .distinct()
                .collect(Collectors.toList());

        // 4. 일괄 조회 실행
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
     * "1,2,3,4,5" -> [1L, 2L, 3L, 4L, 5L]
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