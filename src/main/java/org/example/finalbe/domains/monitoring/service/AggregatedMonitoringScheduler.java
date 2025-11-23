package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.alert.service.AlertEvaluationService;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.monitoring.dto.DataCenterStatisticsDto;
import org.example.finalbe.domains.monitoring.dto.ServerRoomStatisticsDto;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 서버실/데이터센터 통계 주기적 갱신 스케줄러
 * 5초마다 모든 활성 서버실과 데이터센터의 통계를 계산하고 SSE로 전송
 */
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

        // ✅ 병렬 처리
        List<CompletableFuture<Void>> futures = serverRoomIds.stream()
                .map(serverRoomId -> CompletableFuture.runAsync(() -> {
                    long startTime = System.currentTimeMillis();
                    try {
                        ServerRoomStatisticsDto statistics = serverRoomMonitoringService
                                .calculateServerRoomStatistics(serverRoomId);

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

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long totalDuration = System.currentTimeMillis() - totalStartTime;
        log.info("📊 ServerRoom 통합 모니터링 완료 - 총 소요시간: {}ms", totalDuration);
    }
    /**
     * 데이터센터 통계 갱신 스케줄러
     * ✅ fixedRate로 변경: 정확히 5초마다 실행
     */
    @Scheduled(fixedRateString = "${monitoring.scheduler.datacenter-interval:5000}")
    public void updateDataCenterStatistics() {
        log.debug("=== DataCenter 통합 모니터링 시작 ===");

        List<Long> dataCenterIds = dataCenterRepository.findAllByDelYn(DelYN.N)
                .stream()
                .map(dataCenter -> dataCenter.getId())
                .collect(Collectors.toList());

        if (dataCenterIds.isEmpty()) {
            log.debug("처리할 활성 데이터센터가 없습니다.");
            return;
        }

        log.debug("처리 대상 데이터센터: {} (총 {}개)", dataCenterIds, dataCenterIds.size());

        int successCount = 0;
        int failCount = 0;

        for (Long dataCenterId : dataCenterIds) {
            try {
                DataCenterStatisticsDto statistics = dataCenterMonitoringService
                        .calculateDataCenterStatistics(dataCenterId);

                sseService.sendToDataCenter(dataCenterId, "datacenter-statistics", statistics);
                alertEvaluationService.evaluateDataCenterStatistics(statistics);

                successCount++;
            } catch (Exception e) {
                log.error("DataCenter {} 통합 모니터링 실패: {}", dataCenterId, e.getMessage());
                failCount++;
            }
        }

        log.debug("DataCenter 통합 모니터링 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 통계 갱신 상태 로깅 (1분마다)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void logStatistics() {
        try {
            int serverRoomCount = (int) serverRoomRepository.countByDelYn(DelYN.N);
            int dataCenterCount = (int) dataCenterRepository.countByDelYn(DelYN.N);

            log.info("📊 모니터링 통계 - 서버실: {} 개, 데이터센터: {} 개 활성화",
                    serverRoomCount, dataCenterCount);
        } catch (Exception e) {
            log.error("통계 로깅 실패", e);
        }
    }
}