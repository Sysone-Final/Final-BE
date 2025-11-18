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
     * SSE 연결 생성 (과거 데이터 전송 제거 - 실시간 전용)
     */
    public SseEmitter createEmitter(String clientId, SubscriptionInfo subscriptionInfo) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        subscriptionInfo.setEmitter(emitter);
        subscriptions.put(clientId, subscriptionInfo);

        log.info("SSE 연결 생성 - clientId: {}, 구독 타입: {} (실시간 전용)",
                clientId, subscriptionInfo.getType());

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

        // 초기 연결 확인 메시지만 전송 (과거 데이터 전송 제거)
        if (!safeSend(emitter, "connected", Map.of(
                "message", "연결 성공 - 실시간 업데이트 시작",
                "clientId", clientId,
                "subscriptionType", subscriptionInfo.getType(),
                "timestamp", System.currentTimeMillis()
        ))) {
            removeSubscription(clientId);
        }

        return emitter;
    }

    /**
     * 안전한 SSE 전송 메서드 (Broken pipe 방지)
     */
    private boolean safeSend(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data)
            );
            return true;
        } catch (IOException e) {
            // Broken pipe 또는 연결 끊김은 디버그 레벨로만 로깅
            if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                log.debug("클라이언트 연결 끊김 (Broken pipe) - 정상 동작");
            } else {
                log.warn("SSE 전송 실패: {}", e.getMessage());
            }
            return false;
        } catch (Exception e) {
            log.error("SSE 전송 중 예상치 못한 에러", e);
            return false;
        }
    }

    /**
     * 장비별 메트릭 브로드캐스트 (실시간 업데이트용)
     */
    public void broadcastEquipmentMetrics(Long equipmentId, EquipmentMetricsResponse metrics) {
        // 최신 메트릭 저장
        latestEquipmentMetrics.put(equipmentId, metrics);

        // 1. 해당 장비를 직접 구독 중인 클라이언트에게 전송
        subscriptions.entrySet().stream()
                .filter(entry -> entry.getValue().containsEquipment(equipmentId))
                .forEach(entry -> {
                    String clientId = entry.getKey();
                    SubscriptionInfo info = entry.getValue();

                    if (!safeSend(info.getEmitter(), "equipment_metrics", metrics)) {
                        log.debug("실시간 메트릭 전송 실패 - clientId: {}", clientId);
                        removeSubscription(clientId);
                    }
                });

        // 2. 랙/서버실/데이터센터 집계 구독자에게 실시간 집계 전송
        broadcastAggregations(equipmentId);
    }

    /**
     * 집계 브로드캐스트 (랙, 서버실, 데이터센터)
     */
    private void broadcastAggregations(Long equipmentId) {
        subscriptions.entrySet().stream()
                .filter(entry -> {
                    SubscriptionInfo info = entry.getValue();
                    return info.getType() != SubscriptionType.EQUIPMENT &&
                            info.getType() != SubscriptionType.EQUIPMENTS &&
                            info.containsEquipment(equipmentId);
                })
                .forEach(entry -> {
                    String clientId = entry.getKey();
                    SubscriptionInfo info = entry.getValue();

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
                            if (!safeSend(info.getEmitter(), eventName, aggregated)) {
                                removeSubscription(clientId);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("집계 브로드캐스트 실패 - clientId: {}", clientId, e);
                    }
                });
    }

    // PrometheusSSEService.java

    /**
     * 메트릭 집계 (평균 계산) - ✅ NPE 방지 수정
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
                .map(CpuMetricResponse::cpuUsagePercent)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double avgMemoryUsage = equipmentMetrics.stream()
                .flatMap(em -> em.memory().stream())
                .map(MemoryMetricResponse::usagePercent)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double avgNetworkUsage = equipmentMetrics.stream()
                .flatMap(em -> em.network().stream())
                .map(NetworkMetricResponse::totalUsagePercent)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double avgDiskUsage = equipmentMetrics.stream()
                .flatMap(em -> em.disk().stream())
                .map(DiskMetricResponse::usagePercent)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        Double avgTemperature = equipmentMetrics.stream()
                .flatMap(em -> em.temperature().stream())
                .map(TemperatureMetricResponse::tempCelsius)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
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
        List<String> disconnectedClients = new ArrayList<>();

        subscriptions.forEach((clientId, info) -> {
            if (!safeSend(info.getEmitter(), "heartbeat", Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "clientId", clientId
            ))) {
                disconnectedClients.add(clientId);
            }
        });

        // 끊긴 연결 정리
        disconnectedClients.forEach(this::removeSubscription);
    }

    /**
     * 구독 제거
     */
    private void removeSubscription(String clientId) {
        SubscriptionInfo removed = subscriptions.remove(clientId);
        if (removed != null) {
            try {
                if (removed.getEmitter() != null) {
                    removed.getEmitter().complete();
                }
            } catch (IllegalStateException e) {
                // Emitter가 이미 완료되었거나 타임아웃된 경우
                log.debug("Emitter 이미 완료됨 - clientId: {}", clientId);
            } catch (Exception e) {
                // 기타 예외는 조용히 로깅만
                log.debug("Emitter 완료 처리 중 에러 (무시) - clientId: {}, error: {}",
                        clientId, e.getMessage());
            }
            log.info("구독 제거 - clientId: {}", clientId);
        }
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
     * 구독 정보 클래스 (since 필드 제거)
     */
    public static class SubscriptionInfo {
        private SseEmitter emitter;
        private final SubscriptionType type;
        private final Set<Long> equipmentIds;
        private final Long aggregationId;

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