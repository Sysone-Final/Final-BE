package org.example.finalbe.domains.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 서버실 통계 주기적 갱신
     * 5초마다 실행
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void updateServerRoomStatistics() {
        try {
            // 활성화된 모든 서버실 조회
            List<Long> serverRoomIds = serverRoomRepository.findAllByDelYn(DelYN.N)
                    .stream()
                    .map(serverRoom -> serverRoom.getId())
                    .toList();

            if (serverRoomIds.isEmpty()) {
                return;
            }

            log.debug("📊 서버실 통계 갱신 시작 - {} 개 서버실", serverRoomIds.size());

            int successCount = 0;
            int failureCount = 0;

            for (Long serverRoomId : serverRoomIds) {
                try {
                    // 통계 계산
                    ServerRoomStatisticsDto statistics = serverRoomMonitoringService
                            .calculateServerRoomStatistics(serverRoomId);

                    // SSE로 전송 (구독자가 있는 경우에만)
                    sseService.sendToServerRoom(serverRoomId, "serverroom-statistics", statistics);

                    successCount++;
                } catch (Exception e) {
                    log.error("❌ 서버실 통계 갱신 실패: serverRoomId={}", serverRoomId, e);
                    failureCount++;
                }
            }

            if (successCount > 0) {
                log.debug("✅ 서버실 통계 갱신 완료 - 성공: {}, 실패: {}", successCount, failureCount);
            }

        } catch (Exception e) {
            log.error("❌ 서버실 통계 갱신 중 오류 발생", e);
        }
    }

    /**
     * 데이터센터 통계 주기적 갱신
     * 5초마다 실행 (서버실 갱신 후)
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 6000)
    public void updateDataCenterStatistics() {
        try {
            // 활성화된 모든 데이터센터 조회
            List<Long> dataCenterIds =  dataCenterRepository.findAllByDelYn(DelYN.N)
                    .stream()
                    .map(dataCenter -> dataCenter.getId())
                    .toList();

            if (dataCenterIds.isEmpty()) {
                return;
            }

            log.debug("📊 데이터센터 통계 갱신 시작 - {} 개 데이터센터", dataCenterIds.size());

            int successCount = 0;
            int failureCount = 0;

            for (Long dataCenterId : dataCenterIds) {
                try {
                    // 통계 계산
                    DataCenterStatisticsDto statistics = dataCenterMonitoringService
                            .calculateDataCenterStatistics(dataCenterId);

                    // SSE로 전송 (구독자가 있는 경우에만)
                    sseService.sendToDataCenter(dataCenterId, "datacenter-statistics", statistics);

                    successCount++;
                } catch (Exception e) {
                    log.error("❌ 데이터센터 통계 갱신 실패: dataCenterId={}", dataCenterId, e);
                    failureCount++;
                }
            }

            if (successCount > 0) {
                log.debug("✅ 데이터센터 통계 갱신 완료 - 성공: {}, 실패: {}", successCount, failureCount);
            }

        } catch (Exception e) {
            log.error("❌ 데이터센터 통계 갱신 중 오류 발생", e);
        }
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