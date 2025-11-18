package org.example.finalbe.domains.prometheus.service;

import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.AggregatedMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.EquipmentMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.CpuMetricResponse;
import org.example.finalbe.domains.prometheus.dto.MemoryMetricResponse;
import org.example.finalbe.domains.prometheus.dto.NetworkMetricResponse;
import org.example.finalbe.domains.prometheus.dto.DiskMetricResponse;
import org.example.finalbe.domains.prometheus.dto.TemperatureMetricResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PrometheusSSEService {

    private final Map<String, SubscriptionInfo> subscriptions = new ConcurrentHashMap<>();
    private static final long SSE_TIMEOUT = 3600000L; // 1시간

    // 집계용 메트릭 임시 저장 (equipmentId -> EquipmentMetricsResponse)
    private final Map<Long, EquipmentMetricsResponse> latestEquipmentMetrics = new ConcurrentHashMap<>();

    /**
     * SSE 연결 생성
     */
    public SseEmitter createEmitter(String clientId, SubscriptionInfo subscriptionInfo) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        subscriptionInfo.setEmitter(emitter);
        subscriptions.put(clientId, subscriptionInfo);

        log.info("SSE 연결 생성 - clientId: {}, 구독 타입: {}", clientId, subscriptionInfo.getType());

        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료 - clientId: {}", clientId);
            removeSubscription(clientId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 타임아웃 - clientId: {}", clientId);
            removeSubscription(clientId);
        });

        emitter.onError((ex) -> {
            log.error("SSE 에러 - clientId: {}, error: {}", clientId, ex.getMessage());
            removeSubscription(clientId);
        });

        // 초기 연결 확인 메시지
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "message", "연결 성공",
                            "clientId", clientId,
                            "subscriptionType", subscriptionInfo.getType(),
                            "timestamp", System.currentTimeMillis()
                    ))
            );
        } catch (IOException e) {
            log.error("초기 메시지 전송 실패 - clientId: {}", clientId, e);
            removeSubscription(clientId);
        }

        return emitter;
    }

    /**
     * 장비별 메트릭 브로드캐스트 (집계 포함)
     */
    public void broadcastEquipmentMetrics(Long equipmentId, EquipmentMetricsResponse metrics) {
        // 최신 메트릭 저장
        latestEquipmentMetrics.put(equipmentId, metrics);

        // 1. 해당 장비를 직접 구독 중인 클라이언트에게 전송
        List<String> equipmentSubscribers = subscriptions.entrySet().stream()
                .filter(entry -> entry.getValue().containsEquipment(equipmentId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        equipmentSubscribers.forEach(clientId -> {
            SubscriptionInfo info = subscriptions.get(clientId);
            if (info != null) {
                try {
                    info.getEmitter().send(SseEmitter.event()
                            .name("equipment_metrics")
                            .data(metrics)
                    );
                } catch (IOException e) {
                    log.warn("SSE 전송 실패 - clientId: {}, 연결 제거", clientId);
                    removeSubscription(clientId);
                }
            }
        });

        // 2. 랙/서버실/데이터센터 집계 구독자에게 실시간 집계 전송
        broadcastAggregations(equipmentId);
    }

    /**
     * 집계 브로드캐스트 (랙, 서버실, 데이터센터)
     */
    private void broadcastAggregations(Long equipmentId) {
        subscriptions.values().stream()
                .filter(info -> info.getType() != SubscriptionType.EQUIPMENT &&
                        info.getType() != SubscriptionType.EQUIPMENTS)
                .filter(info -> info.containsEquipment(equipmentId))
                .forEach(info -> {
                    try {
                        // 해당 구독의 모든 장비 메트릭 수집
                        List<EquipmentMetricsResponse> equipmentMetrics = info.getEquipmentIds().stream()
                                .map(latestEquipmentMetrics::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        if (!equipmentMetrics.isEmpty()) {
                            AggregatedMetricsResponse aggregated = aggregateMetrics(
                                    equipmentMetrics,
                                    info.getType().toString().toLowerCase(),
                                    info.getAggregationId()
                            );

                            String eventName = info.getType().toString().toLowerCase() + "_aggregated_metrics";
                            info.getEmitter().send(SseEmitter.event()
                                    .name(eventName)
                                    .data(aggregated)
                            );
                        }
                    } catch (IOException e) {
                        log.warn("집계 SSE 전송 실패", e);
                    }
                });
    }

    /**
     * 메트릭 집계 (평균 계산)
     */
    private AggregatedMetricsResponse aggregateMetrics(
            List<EquipmentMetricsResponse> equipmentMetrics,
            String aggregationType,
            Long aggregationId) {

        if (equipmentMetrics.isEmpty()) {
            return AggregatedMetricsResponse.empty(aggregationType, aggregationId);
        }

        Double avgCpuUsage = equipmentMetrics.stream()
                .flatMap(em -> em.cpu().stream())
                .mapToDouble(CpuMetricResponse::cpuUsagePercent)
                .average()
                .orElse(0.0);

        Double avgMemoryUsage = equipmentMetrics.stream()
                .flatMap(em -> em.memory().stream())
                .mapToDouble(MemoryMetricResponse::usagePercent)
                .average()
                .orElse(0.0);

        Double avgNetworkUsage = equipmentMetrics.stream()
                .flatMap(em -> em.network().stream())
                .filter(n -> n.totalUsagePercent() != null)
                .mapToDouble(NetworkMetricResponse::totalUsagePercent)
                .average()
                .orElse(0.0);

        Double avgDiskUsage = equipmentMetrics.stream()
                .flatMap(em -> em.disk().stream())
                .filter(d -> d.usagePercent() != null)
                .mapToDouble(DiskMetricResponse::usagePercent)
                .average()
                .orElse(0.0);

        Double avgTemperature = equipmentMetrics.stream()
                .flatMap(em -> em.temperature().stream())
                .mapToDouble(TemperatureMetricResponse::tempCelsius)
                .average()
                .orElse(0.0);

        return AggregatedMetricsResponse.builder()
                .aggregationType(aggregationType)
                .aggregationId(aggregationId)
                .equipmentCount(equipmentMetrics.size())
                .avgCpuUsagePercent(avgCpuUsage)
                .avgMemoryUsagePercent(avgMemoryUsage)
                .avgNetworkUsagePercent(avgNetworkUsage)
                .avgDiskUsagePercent(avgDiskUsage)
                .avgTemperatureCelsius(avgTemperature)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Heartbeat 전송
     */
    public void sendHeartbeat() {
        subscriptions.forEach((clientId, info) -> {
            try {
                info.getEmitter().send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of(
                                "timestamp", System.currentTimeMillis(),
                                "clientId", clientId
                        ))
                );
            } catch (IOException e) {
                log.warn("Heartbeat 전송 실패 - clientId: {}", clientId);
                removeSubscription(clientId);
            }
        });
    }

    /**
     * 구독 제거
     */
    private void removeSubscription(String clientId) {
        subscriptions.remove(clientId);
        log.info("구독 제거 - clientId: {}", clientId);
    }

    /**
     * 전체 연결 수 조회
     */
    public int getTotalConnections() {
        return subscriptions.size();
    }

    /**
     * 연결 상태 조회
     */
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalConnections", subscriptions.size());
        status.put("clients", subscriptions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Map.of(
                                "type", entry.getValue().getType(),
                                "ids", entry.getValue().getIdentifiers()
                        )
                ))
        );
        return status;
    }

    /**
     * 구독 정보 클래스
     */
    public static class SubscriptionInfo {
        private SseEmitter emitter;
        private final SubscriptionType type;
        private final Set<Long> equipmentIds;
        private final Long aggregationId; // rackId, serverRoomId, dataCenterId

        public static SubscriptionInfo forEquipment(Long equipmentId) {
            return new SubscriptionInfo(SubscriptionType.EQUIPMENT, Set.of(equipmentId), null);
        }

        public static SubscriptionInfo forEquipments(Set<Long> equipmentIds) {
            return new SubscriptionInfo(SubscriptionType.EQUIPMENTS, equipmentIds, null);
        }

        public static SubscriptionInfo forRack(Long rackId, Set<Long> equipmentIds) {
            return new SubscriptionInfo(SubscriptionType.RACK, equipmentIds, rackId);
        }

        public static SubscriptionInfo forServerRoom(Long serverRoomId, Set<Long> equipmentIds) {
            return new SubscriptionInfo(SubscriptionType.SERVER_ROOM, equipmentIds, serverRoomId);
        }

        public static SubscriptionInfo forDataCenter(Long dataCenterId, Set<Long> equipmentIds) {
            return new SubscriptionInfo(SubscriptionType.DATA_CENTER, equipmentIds, dataCenterId);
        }

        private SubscriptionInfo(SubscriptionType type, Set<Long> equipmentIds, Long aggregationId) {
            this.type = type;
            this.equipmentIds = equipmentIds != null ? equipmentIds : new HashSet<>();
            this.aggregationId = aggregationId;
        }

        public void setEmitter(SseEmitter emitter) {
            this.emitter = emitter;
        }

        public SseEmitter getEmitter() {
            return emitter;
        }

        public SubscriptionType getType() {
            return type;
        }

        public boolean containsEquipment(Long equipmentId) {
            return equipmentIds.contains(equipmentId);
        }

        public Long getAggregationId() {
            return aggregationId;
        }

        public Object getIdentifiers() {
            if (aggregationId != null) {
                return Map.of(
                        type.toString().toLowerCase() + "Id", aggregationId,
                        "equipmentIds", equipmentIds
                );
            }
            return equipmentIds;
        }

        public Set<Long> getEquipmentIds() {
            return equipmentIds;
        }
    }

    public enum SubscriptionType {
        EQUIPMENT,
        EQUIPMENTS,
        RACK,
        SERVER_ROOM,
        DATA_CENTER
    }
}