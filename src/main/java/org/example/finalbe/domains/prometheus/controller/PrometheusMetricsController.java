package org.example.finalbe.domains.prometheus.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/prometheus/metrics")
@RequiredArgsConstructor
public class PrometheusMetricsController {

    private final PrometheusSSEService sseService;
    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final DataCenterRepository dataCenterRepository;

    /**
     * SSE 실시간 스트리밍 연결
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