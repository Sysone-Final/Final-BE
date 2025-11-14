package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.AggregationLevel;
import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.example.finalbe.domains.monitoring.dto.*;
import org.example.finalbe.domains.monitoring.repository.EnvironmentMetricRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 환경 메트릭 서비스
 * 환경(온도/습도) 관련 대시보드 데이터 제공 (랙 기준)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvironmentMetricService {

    private final EnvironmentMetricRepository environmentMetricRepository;
    private final RackRepository rackRepository; // 랙 이름 조회를 위해 추가
    private final CpuMetricService cpuMetricService; // AggregationLevel 헬퍼용

    /**
     * 환경 섹션 전체 데이터 조회
     */
    public EnvironmentSectionResponseDto getEnvironmentSectionData(
            Long rackId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 환경 섹션 데이터 조회 시작 - 랙 ID: {}, 기간: {} ~ {}, 집계: {}",
                rackId, startTime, endTime, aggregationLevel);

        // 1. 현재 상태 조회
        EnvironmentCurrentStatsDto currentStats = getCurrentEnvironmentStats(rackId, startTime, endTime);

        // 2. 집계 레벨에 따른 데이터 조회
        List<EnvironmentMetric> metrics;
        List<EnvironmentAggregatedStatsDto> aggregatedData;

        switch (aggregationLevel) {
            case MIN:
                aggregatedData = getEnvironmentAggregatedData1Minute(rackId, startTime, endTime);
                return buildEnvironmentSectionFromAggregated(currentStats, aggregatedData);
            case MIN5:
                aggregatedData = getEnvironmentAggregatedData5Minutes(rackId, startTime, endTime);
                return buildEnvironmentSectionFromAggregated(currentStats, aggregatedData);
            case HOUR:
                aggregatedData = getEnvironmentAggregatedData1Hour(rackId, startTime, endTime);
                return buildEnvironmentSectionFromAggregated(currentStats, aggregatedData);
            case RAW:
            default:
                metrics = environmentMetricRepository.findByRackIdAndTimeRange(
                        rackId, startTime, endTime);
                return buildEnvironmentSectionFromRaw(currentStats, metrics);
        }
    }

    /**
     * 현재 환경 상태 조회 (게이지용)
     */
    public EnvironmentCurrentStatsDto getCurrentEnvironmentStats(
            Long rackId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // 최신 메트릭 조회
        EnvironmentMetric latest = environmentMetricRepository
                .findLatestByRackId(rackId)
                .orElseThrow(() -> new RuntimeException("메트릭 데이터가 없습니다."));

        // 통계 조회
        Object[] stats = environmentMetricRepository.getEnvironmentStats(rackId, startTime, endTime);

        Double avgTemp = 0.0, maxTemp = 0.0, minTemp = 0.0;

        if (stats != null && stats.length > 0) {
            Object firstElement = stats[0];
            if (firstElement instanceof Object[]) {
                Object[] innerArray = (Object[]) firstElement;
                if (innerArray.length >= 3) { // avg, max, min
                    avgTemp = convertToDouble(innerArray[0]);
                    maxTemp = convertToDouble(innerArray[1]);
                    minTemp = convertToDouble(innerArray[2]);
                }
            } else if (stats.length >= 3) {
                avgTemp = convertToDouble(stats[0]);
                maxTemp = convertToDouble(stats[1]);
                minTemp = convertToDouble(stats[2]);
            }
        } else {
            log.warn("환경 통계 쿼리 결과가 null이거나 비어있습니다.");
        }

        return EnvironmentCurrentStatsDto.builder()
                .currentTemperature(latest.getTemperature())
                .avgTemperature(avgTemp)
                .maxTemperature(maxTemp)
                .minTemperature(minTemp)
                .currentHumidity(latest.getHumidity())
                .temperatureWarning(latest.getTemperatureWarning())
                .humidityWarning(latest.getHumidityWarning())
                .lastUpdated(latest.getGenerateTime())
                .build();
    }

    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 1분 단위 집계 데이터 조회
     */
    private List<EnvironmentAggregatedStatsDto> getEnvironmentAggregatedData1Minute(
            Long rackId, LocalDateTime startTime, LocalDateTime endTime) {
        return environmentMetricRepository.getEnvironmentAggregatedStats1Minute(rackId, startTime, endTime)
                .stream()
                .map(this::mapToEnvironmentAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 5분 단위 집계 데이터 조회
     */
    private List<EnvironmentAggregatedStatsDto> getEnvironmentAggregatedData5Minutes(
            Long rackId, LocalDateTime startTime, LocalDateTime endTime) {
        return environmentMetricRepository.getEnvironmentAggregatedStats5Minutes(rackId, startTime, endTime)
                .stream()
                .map(this::mapToEnvironmentAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * 1시간 단위 집계 데이터 조회
     */
    private List<EnvironmentAggregatedStatsDto> getEnvironmentAggregatedData1Hour(
            Long rackId, LocalDateTime startTime, LocalDateTime endTime) {
        return environmentMetricRepository.getEnvironmentAggregatedStats1Hour(rackId, startTime, endTime)
                .stream()
                .map(this::mapToEnvironmentAggregatedStats)
                .collect(Collectors.toList());
    }

    /**
     * Object[] → EnvironmentAggregatedStatsDto 매핑
     */
    private EnvironmentAggregatedStatsDto mapToEnvironmentAggregatedStats(Object[] row) {
        return EnvironmentAggregatedStatsDto.builder()
                .timestamp(((Timestamp) row[0]).toLocalDateTime())
                .avgTemperature(convertToDouble(row[1]))
                .maxTemperature(convertToDouble(row[2]))
                .minTemperature(convertToDouble(row[3]))
                .avgHumidity(convertToDouble(row[4]))
                .sampleCount(row[5] != null ? ((Number) row[5]).intValue() : 0)
                .build();
    }

    /**
     * RAW 데이터로부터 환경 섹션 응답 생성
     */
    private EnvironmentSectionResponseDto buildEnvironmentSectionFromRaw(
            EnvironmentCurrentStatsDto currentStats,
            List<EnvironmentMetric> metrics) {

        List<TemperaturePointDto> temperatureTrend = new ArrayList<>();
        List<HumidityPointDto> humidityTrend = new ArrayList<>();

        for (EnvironmentMetric metric : metrics) {
            // 온도 그래프
            temperatureTrend.add(TemperaturePointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .temperature(metric.getTemperature())
                    .build());

            // 습도 그래프
            humidityTrend.add(HumidityPointDto.builder()
                    .timestamp(metric.getGenerateTime())
                    .humidity(metric.getHumidity())
                    .build());
        }

        return EnvironmentSectionResponseDto.builder()
                .currentStats(currentStats)
                .temperatureTrend(temperatureTrend)
                .humidityTrend(humidityTrend)
                .build();
    }

    /**
     * 집계 데이터로부터 환경 섹션 응답 생성
     */
    private EnvironmentSectionResponseDto buildEnvironmentSectionFromAggregated(
            EnvironmentCurrentStatsDto currentStats,
            List<EnvironmentAggregatedStatsDto> aggregatedData) {

        List<TemperaturePointDto> temperatureTrend = aggregatedData.stream()
                .map(agg -> TemperaturePointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .temperature(agg.getAvgTemperature())
                        .build())
                .collect(Collectors.toList());

        List<HumidityPointDto> humidityTrend = aggregatedData.stream()
                .map(agg -> HumidityPointDto.builder()
                        .timestamp(agg.getTimestamp())
                        .humidity(agg.getAvgHumidity())
                        .build())
                .collect(Collectors.toList());

        return EnvironmentSectionResponseDto.builder()
                .currentStats(currentStats)
                .temperatureTrend(temperatureTrend)
                .humidityTrend(humidityTrend)
                .build();
    }

    /**
     * (CpuMetricService의 로직 재활용)
     */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
    }

    /**
     * 여러 랙의 현재 환경 상태 일괄 조회
     */
    public EnvironmentCurrentStatsBatchDto getCurrentEnvironmentStatsBatch(List<Long> rackIds) {

        log.info("📊 일괄 환경 상태 조회 시작 - 랙 개수: {}", rackIds.size());

        List<EnvironmentStatsWithRackDto> rackStatsList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // 랙 이름 조회를 위한 랙 정보
        Map<Long, Rack> rackMap = rackRepository.findAllById(rackIds).stream()
                .collect(Collectors.toMap(Rack::getId, Function.identity()));

        // 1. 모든 랙의 최신 메트릭 일괄 조회
        List<EnvironmentMetric> latestMetrics = environmentMetricRepository
                .findLatestByRackIds(rackIds);

        Map<Long, EnvironmentMetric> latestMetricMap = latestMetrics.stream()
                .collect(Collectors.toMap(EnvironmentMetric::getRackId, metric -> metric));

        // 2. 모든 랙의 통계 일괄 조회 (최근 60개 데이터 기준)
        List<Object[]> statsResults = environmentMetricRepository
                .getEnvironmentStatsBatch(rackIds, 60);

        Map<Long, Object[]> statsMap = statsResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),  // rack_id
                        row -> new Object[]{row[1], row[2], row[3]}  // avgT, maxT, minT
                ));

        // 3. 각 랙별 데이터 조합
        for (Long rackId : rackIds) {
//            String rackName = rackMap.getOrDefault(rackId, new Rack(rackId, "Unknown Rack " + rackId)).getName();
            Rack rack = rackMap.get(rackId);
            String rackName = (rack != null) ? rack.getRackName() : ("Unknown Rack " + rackId);
            try {
                EnvironmentMetric latest = latestMetricMap.get(rackId);

                if (latest == null) {
                    rackStatsList.add(EnvironmentStatsWithRackDto.builder()
                            .rackId(rackId)
                            .rackName(rackName)
                            .success(false)
                            .errorMessage("메트릭 데이터가 없습니다.")
                            .build());
                    failureCount++;
                    continue;
                }

                Object[] stats = statsMap.get(rackId);
                Double currentTemp = latest.getTemperature();
                Double avgTemp = currentTemp, maxTemp = currentTemp, minTemp = currentTemp;

                if (stats != null && stats[0] != null) {
                    avgTemp = convertToDouble(stats[0]);
                    maxTemp = convertToDouble(stats[1]);
                    minTemp = convertToDouble(stats[2]);
                } else {
                    log.warn("⚠️ 랙 {}의 환경 통계 데이터 없음, 현재값으로 대체", rackId);
                }

                EnvironmentCurrentStatsDto envStats = EnvironmentCurrentStatsDto.builder()
                        .currentTemperature(currentTemp)
                        .avgTemperature(avgTemp)
                        .maxTemperature(maxTemp)
                        .minTemperature(minTemp)
                        .currentHumidity(latest.getHumidity())
                        .temperatureWarning(latest.getTemperatureWarning())
                        .humidityWarning(latest.getHumidityWarning())
                        .lastUpdated(latest.getGenerateTime())
                        .build();

                rackStatsList.add(EnvironmentStatsWithRackDto.builder()
                        .rackId(rackId)
                        .rackName(rackName)
                        .success(true)
                        .environmentStats(envStats)
                        .build());

                successCount++;

            } catch (Exception e) {
                log.error("❌ 랙 {} 환경 상태 조회 실패: {}", rackId, e.getMessage());
                rackStatsList.add(EnvironmentStatsWithRackDto.builder()
                        .rackId(rackId)
                        .rackName(rackName)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        log.info("✅ 일괄 환경 상태 조회 완료 - 성공: {}, 실패: {}", successCount, failureCount);

        return EnvironmentCurrentStatsBatchDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .rackStats(rackStatsList)
                .build();
    }
}