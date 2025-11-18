package org.example.finalbe.domains.prometheus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.prometheus.dto.AggregatedMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.EquipmentMetricsResponse;
import org.example.finalbe.domains.prometheus.service.PrometheusMetricQueryService;
import org.example.finalbe.domains.prometheus.service.PrometheusSSEService;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/prometheus/metrics")
@RequiredArgsConstructor
public class PrometheusMetricsController {

    private final PrometheusSSEService sseService;
    private final PrometheusMetricQueryService queryService;
    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final DataCenterRepository dataCenterRepository;

    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("^(\\d+)(m|h|d)$");
    private static final long MAX_DAYS = 7;

    /**
     * ✅ 과거 데이터 조회 (REST API) - SSE 연결 전 1회 호출용
     */
    @GetMapping("/historical")
    @Transactional(readOnly = true)
    public ResponseEntity<EquipmentMetricsResponse> getHistoricalMetrics(
            @RequestParam Long equipmentId,
            @RequestParam(defaultValue = "1h") String timeRange) {

        Instant since = parseTimeRange(timeRange);

        log.info("과거 데이터 조회 - equipmentId: {}, timeRange: {}, since: {}",
                equipmentId, timeRange, since);

        EquipmentMetricsResponse response =
                queryService.getHistoricalMetricsByEquipment(equipmentId, since);

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ 여러 장비 과거 데이터 일괄 조회 (REST API)
     */
    @GetMapping("/historical/batch")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EquipmentMetricsResponse>> getHistoricalMetricsBatch(
            @RequestParam String equipmentIds,
            @RequestParam(defaultValue = "1h") String timeRange) {

        Set<Long> idSet = parseEquipmentIds(equipmentIds);
        Instant since = parseTimeRange(timeRange);

        log.info("과거 데이터 일괄 조회 - equipmentIds: {}, timeRange: {}", idSet, timeRange);

        List<EquipmentMetricsResponse> responses =
                queryService.getHistoricalMetricsByEquipments(idSet, since);

        return ResponseEntity.ok(responses);
    }

    /**
     * ✅ 집계 과거 데이터 조회 (REST API) - Rack, ServerRoom, DataCenter
     */
    @GetMapping("/historical/aggregated")
    @Transactional(readOnly = true)
    public ResponseEntity<AggregatedMetricsResponse> getHistoricalAggregatedMetrics(
            @RequestParam(required = false) Long rackId,
            @RequestParam(required = false) Long serverRoomId,
            @RequestParam(required = false) Long dataCenterId,
            @RequestParam(defaultValue = "1h") String timeRange) {

        Instant since = parseTimeRange(timeRange);
        Set<Long> equipmentIds;
        String aggregationType;
        Long aggregationId;

        if (rackId != null) {
            equipmentIds = getEquipmentIdsByRack(rackId);
            aggregationType = "rack";
            aggregationId = rackId;
        } else if (serverRoomId != null) {
            equipmentIds = getEquipmentIdsByServerRoom(serverRoomId);
            aggregationType = "serverRoom";
            aggregationId = serverRoomId;
        } else if (dataCenterId != null) {
            equipmentIds = getEquipmentIdsByDataCenter(dataCenterId);
            aggregationType = "dataCenter";
            aggregationId = dataCenterId;
        } else {
            throw new IllegalArgumentException("rackId, serverRoomId, dataCenterId 중 하나를 입력해주세요");
        }

        log.info("집계 과거 데이터 조회 - type: {}, id: {}, timeRange: {}, 장비 수: {}",
                aggregationType, aggregationId, timeRange, equipmentIds.size());

        AggregatedMetricsResponse response =
                queryService.getHistoricalAggregatedMetrics(equipmentIds, aggregationType, aggregationId, since);

        return ResponseEntity.ok(response);
    }

    /**
     * SSE 실시간 스트리밍 연결 (실시간 업데이트만)
     * ✅ timeRange 파라미터 제거 - 과거 데이터는 /historical 엔드포인트 사용
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Transactional(readOnly = true)
    public SseEmitter streamMetrics(
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) String equipmentIds,
            @RequestParam(required = false) Long rackId,
            @RequestParam(required = false) Long serverRoomId,
            @RequestParam(required = false) Long dataCenterId,
            @RequestParam(required = false) String clientId) {

        String finalClientId = clientId != null ? clientId : UUID.randomUUID().toString();

        log.info("SSE 연결 요청 (실시간 전용) - clientId: {}", finalClientId);

        PrometheusSSEService.SubscriptionInfo subscriptionInfo;

        if (equipmentId != null) {
            log.info("SSE 연결 요청 - 단일 장비, equipmentId: {}", equipmentId);
            subscriptionInfo = PrometheusSSEService.SubscriptionInfo.forEquipment(equipmentId);
        }
        else if (equipmentIds != null) {
            Set<Long> equipmentIdSet = parseEquipmentIds(equipmentIds);
            log.info("SSE 연결 요청 - 여러 장비, equipmentIds: {}", equipmentIdSet);
            subscriptionInfo = PrometheusSSEService.SubscriptionInfo.forEquipments(equipmentIdSet);
        }
        else if (rackId != null) {
            Set<Long> equipmentIdSet = getEquipmentIdsByRack(rackId);
            log.info("SSE 연결 요청 - 랙 집계, rackId: {}, 장비 수: {}", rackId, equipmentIdSet.size());
            subscriptionInfo = PrometheusSSEService.SubscriptionInfo.forRack(rackId, equipmentIdSet);
        }
        else if (serverRoomId != null) {
            Set<Long> equipmentIdSet = getEquipmentIdsByServerRoom(serverRoomId);
            log.info("SSE 연결 요청 - 서버실 집계, serverRoomId: {}, 장비 수: {}", serverRoomId, equipmentIdSet.size());
            subscriptionInfo = PrometheusSSEService.SubscriptionInfo.forServerRoom(serverRoomId, equipmentIdSet);
        }
        else if (dataCenterId != null) {
            Set<Long> equipmentIdSet = getEquipmentIdsByDataCenter(dataCenterId);
            log.info("SSE 연결 요청 - 데이터센터 집계, dataCenterId: {}, 장비 수: {}", dataCenterId, equipmentIdSet.size());
            subscriptionInfo = PrometheusSSEService.SubscriptionInfo.forDataCenter(dataCenterId, equipmentIdSet);
        }
        else {
            throw new IllegalArgumentException("조회 조건을 입력해주세요 (equipmentId, equipmentIds, rackId, serverRoomId, dataCenterId 중 하나)");
        }

        return sseService.createEmitter(finalClientId, subscriptionInfo);
    }

    /**
     * SSE 연결 상태 조회
     */
    @GetMapping("/sse/status")
    public ResponseEntity<Map<String, Object>> getSseStatus() {
        Map<String, Object> status = sseService.getConnectionStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * timeRange 파싱
     * 지원 형식: 15m, 1h, 3h, 6h, 12h, 1d, 3d, 7d
     *
     * @param timeRange 시간 범위 문자열
     * @return 현재 시간 기준 과거 시점 (Instant)
     */
    private Instant parseTimeRange(String timeRange) {
        if (timeRange == null || timeRange.isBlank()) {
            return Instant.now().minus(1, ChronoUnit.HOURS); // 기본 1시간
        }

        Matcher matcher = TIME_RANGE_PATTERN.matcher(timeRange.toLowerCase().trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "잘못된 timeRange 형식입니다. 예: 15m, 1h, 3h, 1d, 7d");
        }

        int value = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2);

        Instant now = Instant.now();
        Instant since;

        switch (unit) {
            case "m": // 분
                if (value > 60) {
                    throw new IllegalArgumentException("분 단위는 최대 60분까지 가능합니다.");
                }
                since = now.minus(value, ChronoUnit.MINUTES);
                break;

            case "h": // 시간
                if (value > 24) {
                    throw new IllegalArgumentException("시간 단위는 최대 24시간까지 가능합니다.");
                }
                since = now.minus(value, ChronoUnit.HOURS);
                break;

            case "d": // 일
                if (value > MAX_DAYS) {
                    throw new IllegalArgumentException(
                            String.format("일 단위는 최대 %d일까지 가능합니다.", MAX_DAYS));
                }
                since = now.minus(value, ChronoUnit.DAYS);

                if (value > 1) {
                    log.warn("장기간 데이터 요청 - timeRange: {}, 데이터가 많을 수 있습니다", timeRange);
                }
                break;

            default:
                throw new IllegalArgumentException("지원하지 않는 시간 단위입니다: " + unit);
        }

        log.debug("timeRange 파싱 완료 - 입력: {}, 결과: {} ~ {}", timeRange, since, now);
        return since;
    }

    /**
     * equipmentIds 파싱
     */
    private Set<Long> parseEquipmentIds(String equipmentIds) {
        try {
            return Arrays.stream(equipmentIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID 형식입니다: " + equipmentIds);
        }
    }

    /**
     * 랙에 속한 장비 ID 목록 조회
     */
    private Set<Long> getEquipmentIdsByRack(Long rackId) {
        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 랙입니다: " + rackId));

        return equipmentRepository.findByRackIdAndDelYn(rackId, DelYN.N).stream()
                .map(equipment -> equipment.getId())
                .collect(Collectors.toSet());
    }

    /**
     * 서버실에 속한 장비 ID 목록 조회
     */
    private Set<Long> getEquipmentIdsByServerRoom(Long serverRoomId) {
        ServerRoom serverRoom = serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 서버실입니다: " + serverRoomId));

        return rackRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N).stream()
                .flatMap(rack -> equipmentRepository.findByRackIdAndDelYn(rack.getId(), DelYN.N).stream())
                .map(equipment -> equipment.getId())
                .collect(Collectors.toSet());
    }

    /**
     * 데이터센터에 속한 장비 ID 목록 조회
     */
    private Set<Long> getEquipmentIdsByDataCenter(Long dataCenterId) {
        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 데이터센터입니다: " + dataCenterId));

        return serverRoomRepository.findByDataCenterIdAndDelYn(dataCenterId).stream()
                .flatMap(serverRoom -> rackRepository.findByServerRoomIdAndDelYn(serverRoom.getId(), DelYN.N).stream())
                .flatMap(rack -> equipmentRepository.findByRackIdAndDelYn(rack.getId(), DelYN.N).stream())
                .map(equipment -> equipment.getId())
                .collect(Collectors.toSet());
    }
}