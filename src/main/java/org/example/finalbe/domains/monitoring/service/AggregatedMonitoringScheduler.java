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

/**
 * 서버실/데이터센터 통계 주기적 갱신 스케줄러
 * 5초마다 모든 서버실과 데이터센터의 통계를 계산하고 SSE로 전송
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

    private List<Long> serverRoomIds = List.of(1L, 2L, 3L);
    private List<Long> dataCenterIds = List.of(1L);


    @Scheduled(fixedDelayString = "${monitoring.scheduler.statistics-interval:5000}")
    public void updateServerRoomStatistics() {
        log.debug("=== ServerRoom 통합 모니터링 시작 ===");

        int successCount = 0;
        int failCount = 0;

        for (Long serverRoomId : serverRoomIds) {
            try {
                ServerRoomStatisticsDto statistics = serverRoomMonitoringService
                        .calculateServerRoomStatistics(serverRoomId);

                sseService.sendToServerRoom(serverRoomId, "serverroom-statistics", statistics);

                // ✅ 알림 평가 호출 추가
                alertEvaluationService.evaluateServerRoomStatistics(statistics);

                successCount++;
            } catch (Exception e) {
                log.error("ServerRoom {} 통합 모니터링 실패: {}", serverRoomId, e.getMessage());
                failCount++;
            }
        }

        log.debug("ServerRoom 통합 모니터링 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }


    @Scheduled(fixedDelayString = "${monitoring.scheduler.datacenter-interval:5000}")
    public void updateDataCenterStatistics() {
        log.debug("=== DataCenter 통합 모니터링 시작 ===");

        int successCount = 0;
        int failCount = 0;

        for (Long dataCenterId : dataCenterIds) {
            try {
                DataCenterStatisticsDto statistics = dataCenterMonitoringService
                        .calculateDataCenterStatistics(dataCenterId);

                sseService.sendToDataCenter(dataCenterId, "datacenter-statistics", statistics);

                // ✅ 알림 평가 호출 추가
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