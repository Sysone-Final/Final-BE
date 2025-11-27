/**
 * 작성자: 최산하
 * 환경(온도/습도) 메트릭 서비스를 제공하는 클래스
 */
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnvironmentMetricService {

    private final EnvironmentMetricRepository environmentMetricRepository;
    private final RackRepository rackRepository;
    private final CpuMetricService cpuMetricService;

    /** 환경 섹션 전체 데이터 조회 */
    public EnvironmentSectionResponseDto getEnvironmentSectionData(
            Long rackId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            AggregationLevel aggregationLevel) {

        log.info("📊 환경 섹션 조회 - rackId={}, 기간={}~{}, 집계={}",
                rackId, startTime, endTime, aggregationLevel);

        EnvironmentCurrentStatsDto currentStats = getCurrentEnvironmentStats(rackId, startTime, endTime);

        switch (aggregationLevel) {
            case MIN:
                return buildEnvironmentSectionFromAggregated(
                        currentStats,
                        getEnvironmentAggregatedData1Minute(rackId, startTime, endTime)
                );
            case MIN5:
                return buildEnvironmentSectionFromAggregated(
                        currentStats,
                        getEnvironmentAggregatedData5Minutes(rackId, startTime, endTime)
                );
            case HOUR:
                return buildEnvironmentSectionFromAggregated(
                        currentStats,
                        getEnvironmentAggregatedData1Hour(rackId, startTime, endTime)
                );
            case DAY:
                return buildEnvironmentSectionFromAggregated(
                        currentStats,
                        getEnvironmentAggregatedData1Day(rackId, startTime, endTime)
                );
            case RAW:
            default:
                return buildEnvironmentSectionFromRaw(
                        currentStats,
                        environmentMetricRepository.findByRackIdAndTimeRange(rackId, startTime, endTime)
                );
        }
    }

    /** 현재 환경 상태 조회 */
    public EnvironmentCurrentStatsDto getCurrentEnvironmentStats(
            Long rackId,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        EnvironmentMetric latest = environmentMetricRepository
                .findLatestByRackId(rackId)
                .orElseThrow(() -> new RuntimeException("메트릭 데이터가 없습니다."));

        Object[] stats = environmentMetricRepository.getEnvironmentStats(rackId, startTime, endTime);

        Double avgTemp = 0.0, maxTemp = 0.0, minTemp = 0.0;

        if (stats != null && stats.length > 0) {
            Object first = stats[0];
            if (first instanceof Object[]) {
                Object[] arr = (Object[]) first;
                if (arr.length >= 3) {
                    avgTemp = convertToDouble(arr[0]);
                    maxTemp = convertToDouble(arr[1]);
                    minTemp = convertToDouble(arr[2]);
                }
            } else if (stats.length >= 3) {
                avgTemp = convertToDouble(stats[0]);
                maxTemp = convertToDouble(stats[1]);
                minTemp = convertToDouble(stats[2]);
            }
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

    /** 숫자 변환 헬퍼 */
    private Double convertToDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (Exception e) { return 0.0; }
    }

    /** 1분 단위 집계 조회 */
    private List<EnvironmentAggregatedStatsDto> getEnvironmentAggregatedData1Minute(
            Long rackId, LocalDateTime start, LocalDateTime end) {

        return environmentMetricRepository.getEnvironmentAggregatedStats1Minute(rackId, start, end)
                .stream().map(this::mapToEnvironmentAggregatedStats).collect(Collectors.toList());
    }

    /** 5분 단위 집계 조회 */
    private List<EnvironmentAggregatedStatsDto> getEnvironmentAggregatedData5Minutes(
            Long rackId, LocalDateTime start, LocalDateTime end) {

        return environmentMetricRepository.getEnvironmentAggregatedStats5Minutes(rackId, start, end)
                .stream().map(this::mapToEnvironmentAggregatedStats).collect(Collectors.toList());
    }

    /** 1시간 단위 집계 조회 */
    private List<EnvironmentAggregatedStatsDto> getEnvironmentAggregatedData1Hour(
            Long rackId, LocalDateTime start, LocalDateTime end) {

        return environmentMetricRepository.getEnvironmentAggregatedStats1Hour(rackId, start, end)
                .stream().map(this::mapToEnvironmentAggregatedStats).collect(Collectors.toList());
    }

    /** 1일 단위 집계 조회 */
    private List<EnvironmentAggregatedStatsDto> getEnvironmentAggregatedData1Day(
            Long rackId, LocalDateTime start, LocalDateTime end) {

        return environmentMetricRepository.getEnvironmentAggregatedStats1Day(rackId, start, end)
                .stream().map(this::mapToEnvironmentAggregatedStats).collect(Collectors.toList());
    }

    /** Object[] → DTO 매핑 */
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

    /** RAW 데이터 기반 환경 섹션 생성 */
    private EnvironmentSectionResponseDto buildEnvironmentSectionFromRaw(
            EnvironmentCurrentStatsDto currentStats,
            List<EnvironmentMetric> metrics) {

        List<TemperaturePointDto> temperatureTrend = new ArrayList<>();
        List<HumidityPointDto> humidityTrend = new ArrayList<>();

        for (EnvironmentMetric m : metrics) {
            temperatureTrend.add(TemperaturePointDto.builder()
                    .timestamp(m.getGenerateTime())
                    .temperature(m.getTemperature())
                    .build());

            humidityTrend.add(HumidityPointDto.builder()
                    .timestamp(m.getGenerateTime())
                    .humidity(m.getHumidity())
                    .build());
        }

        return EnvironmentSectionResponseDto.builder()
                .currentStats(currentStats)
                .temperatureTrend(temperatureTrend)
                .humidityTrend(humidityTrend)
                .build();
    }

    /** 집계 데이터 기반 환경 섹션 생성 */
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

    /** 집계 레벨 자동 결정 (CPU 서비스 로직 재사용) */
    public AggregationLevel determineOptimalAggregationLevel(
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return cpuMetricService.determineOptimalAggregationLevel(startTime, endTime);
    }

    /** 여러 랙의 현재 환경 상태 일괄 조회 */
    public EnvironmentCurrentStatsBatchDto getCurrentEnvironmentStatsBatch(List<Long> rackIds) {

        log.info("📊 일괄 환경 상태 조회 시작 - {}개 랙", rackIds.size());

        List<EnvironmentStatsWithRackDto> rackStatsList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        Map<Long, Rack> rackMap = rackRepository.findAllById(rackIds).stream()
                .collect(Collectors.toMap(Rack::getId, Function.identity()));

        List<EnvironmentMetric> latestMetrics =
                environmentMetricRepository.findLatestByRackIds(rackIds);

        Map<Long, EnvironmentMetric> latestMetricMap = latestMetrics.stream()
                .collect(Collectors.toMap(EnvironmentMetric::getRackId, m -> m));

        List<Object[]> statsResults =
                environmentMetricRepository.getEnvironmentStatsBatch(rackIds, 60);

        Map<Long, Object[]> statsMap = statsResults.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> new Object[]{row[1], row[2], row[3]}
                ));

        for (Long rackId : rackIds) {

            Rack rack = rackMap.get(rackId);
            String rackName = (rack != null) ? rack.getRackName() : ("Rack " + rackId);

            try {
                EnvironmentMetric latest = latestMetricMap.get(rackId);

                if (latest == null) {
                    failureCount++;
                    rackStatsList.add(EnvironmentStatsWithRackDto.builder()
                            .rackId(rackId)
                            .rackName(rackName)
                            .success(false)
                            .errorMessage("메트릭 데이터 없음")
                            .build());
                    continue;
                }

                Object[] stats = statsMap.get(rackId);
                Double cur = latest.getTemperature();
                Double avg = cur, max = cur, min = cur;

                if (stats != null && stats[0] != null) {
                    avg = convertToDouble(stats[0]);
                    max = convertToDouble(stats[1]);
                    min = convertToDouble(stats[2]);
                }

                EnvironmentCurrentStatsDto envStats = EnvironmentCurrentStatsDto.builder()
                        .currentTemperature(cur)
                        .avgTemperature(avg)
                        .maxTemperature(max)
                        .minTemperature(min)
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
                failureCount++;
                rackStatsList.add(EnvironmentStatsWithRackDto.builder()
                        .rackId(rackId)
                        .rackName(rackName)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return EnvironmentCurrentStatsBatchDto.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .rackStats(rackStatsList)
                .build();
    }
}
