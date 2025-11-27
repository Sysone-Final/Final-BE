/**
 * 작성자: 황요한
 * 서버실·데이터센터·랙 통계 계산을 스케줄링하여 SSE로 전송하는 서비스
 */
package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.service.AlertEvaluationService;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.monitoring.dto.DataCenterStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.RackStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.ServerRoomStatisticsDto;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatedMonitoringScheduler {

    private final ServerRoomRepository serverRoomRepository;
    private final DataCenterRepository dataCenterRepository;
    private final ServerRoomMonitoringService serverRoomMonitoringService;
    private final DataCenterMonitoringService dataCenterMonitoringService;
    private final SseService sseService;
    private final AlertEvaluationService alertEvaluationService;
    private final Executor taskExecutor;
    private final EquipmentRepository equipmentRepository;
    private final RackMonitoringService rackMonitoringService;
    private final MonitoringMetricCache monitoringMetricCache;

    // 서버실 통계를 계산하고 SSE로 전송
    @Scheduled(fixedRateString = "${monitoring.scheduler.statistics-interval:5000}")
    public void updateServerRoomStatistics() {
        log.debug("=== ServerRoom 통합 모니터링 시작 ===");
        long totalStartTime = System.currentTimeMillis();

        List<Long> serverRoomIds = serverRoomRepository.findAllByDelYn(DelYN.N)
                .stream()
                .map(serverRoom -> serverRoom.getId())
                .collect(Collectors.toList());

        if (serverRoomIds.isEmpty()) {
            log.debug("처리할 활성 서버실이 없습니다.");
            return;
        }

        // 구독자가 있는 서버실만 처리
        List<Long> subscribedServerRoomIds = serverRoomIds.stream()
                .filter(id -> sseService.hasSubscribers("serverroom-" + id))
                .collect(Collectors.toList());

        if (subscribedServerRoomIds.isEmpty()) {
            log.debug("구독자가 있는 서버실이 없습니다. 스킵합니다.");
            return;
        }

        // 병렬 처리
        List<CompletableFuture<Void>> futures = subscribedServerRoomIds.stream()
                .map(serverRoomId -> CompletableFuture.runAsync(() -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        ServerRoomStatisticsDto statistics =
                                serverRoomMonitoringService.calculateServerRoomStatistics(serverRoomId);

                        sseService.sendToServerRoom(serverRoomId, "serverroom-statistics", statistics);
                        alertEvaluationService.evaluateServerRoomStatistics(statistics);

                        long duration = System.currentTimeMillis() - startTime;
                        if (duration > 3000) {
                            log.warn("⚠️ ServerRoom {} 통계 계산 느림: {}ms", serverRoomId, duration);
                        }
                    } catch (Exception e) {
                        log.error("❌ ServerRoom {} 통합 모니터링 실패: {}", serverRoomId, e.getMessage());
                    }
                }, taskExecutor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        log.info("📊 ServerRoom 통합 모니터링 완료 - 총 소요시간: {}ms, 처리: {}개",
                totalDuration, subscribedServerRoomIds.size());
    }

    // 데이터센터 통계를 계산하고 SSE로 전송
    @Scheduled(fixedRateString = "${monitoring.scheduler.datacenter-interval:5000}")
    public void updateDataCenterStatistics() {
        log.debug("=== DataCenter 통합 모니터링 시작 ===");

        List<Long> dataCenterIds = dataCenterRepository.findAllByDelYn(DelYN.N)
                .stream()
                .map(dc -> dc.getId())
                .collect(Collectors.toList());

        if (dataCenterIds.isEmpty()) {
            log.debug("처리할 활성 데이터센터가 없습니다.");
            return;
        }

        // 구독자 있는 데이터센터만 처리
        List<Long> subscribedDataCenterIds = dataCenterIds.stream()
                .filter(id -> sseService.hasSubscribers("datacenter-" + id))
                .collect(Collectors.toList());

        if (subscribedDataCenterIds.isEmpty()) {
            log.debug("구독자가 있는 데이터센터가 없습니다. 스킵합니다.");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Long dataCenterId : subscribedDataCenterIds) {
            try {
                DataCenterStatisticsDto statistics =
                        dataCenterMonitoringService.calculateDataCenterStatistics(dataCenterId);

                sseService.sendToDataCenter(dataCenterId, "datacenter-statistics", statistics);

                successCount++;
            } catch (Exception e) {
                log.error("DataCenter {} 통합 모니터링 실패: {}", dataCenterId, e.getMessage());
                failCount++;
            }
        }

        log.debug("DataCenter 통합 모니터링 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }

    // 활성 서버실/데이터센터 수를 로그로 출력
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void logStatistics() {
        try {
            int serverRoomCount = (int) serverRoomRepository.countByDelYn(DelYN.N);
            int dataCenterCount = (int) dataCenterRepository.countByDelYn(DelYN.N);

            log.info("📊 모니터링 통계 - 서버실: {}개, 데이터센터: {}개 활성화", serverRoomCount, dataCenterCount);
        } catch (Exception e) {
            log.error("통계 로깅 실패", e);
        }
    }

    // 랙 통계를 계산하고 SSE로 전송
    @Scheduled(fixedRateString = "${monitoring.scheduler.rack-interval:5000}")
    public void updateRackStatistics() {
        log.debug("=== Rack 통합 모니터링 시작 ===");
        long totalStartTime = System.currentTimeMillis();

        // 장비가 있는 랙만 조회
        List<Long> rackIds = equipmentRepository.findAllDistinctRackIds();

        if (rackIds.isEmpty()) {
            log.debug("처리할 활성 랙이 없습니다 (장비가 배치된 랙 없음).");
            return;
        }

        // 구독자가 있는 랙만 처리
        List<Long> subscribedRackIds = rackIds.stream()
                .filter(id -> sseService.hasSubscribers("rack-" + id))
                .collect(Collectors.toList());

        if (subscribedRackIds.isEmpty()) {
            log.debug("구독자가 있는 랙이 없습니다. 스킵합니다.");
            return;
        }

        log.debug("처리 대상 랙: {} (총 {}개, 장비 배치됨)", subscribedRackIds, subscribedRackIds.size());

        List<CompletableFuture<Void>> futures = subscribedRackIds.stream()
                .map(rackId -> CompletableFuture.runAsync(() -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        RackStatisticsDto statistics =
                                rackMonitoringService.calculateRackStatistics(rackId);

                        monitoringMetricCache.updateRackStatistics(statistics);
                        sseService.sendToRack(rackId, "rack-statistics", statistics);

                        long duration = System.currentTimeMillis() - startTime;
                        if (duration > 3000) {
                            log.warn("⚠️ Rack {} 통계 계산 느림: {}ms", rackId, duration);
                        }
                    } catch (Exception e) {
                        log.error("❌ Rack {} 통합 모니터링 실패: {}", rackId, e.getMessage());
                    }
                }, taskExecutor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        log.info("📊 Rack 통합 모니터링 완료 - 총 소요시간: {}ms, 처리 랙: {}개",
                totalDuration, subscribedRackIds.size());
    }
}
