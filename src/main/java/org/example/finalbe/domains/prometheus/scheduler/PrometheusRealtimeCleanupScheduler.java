package org.example.finalbe.domains.prometheus.scheduler;


import org.example.finalbe.domains.common.config.PrometheusRealtimeConfig;
import org.example.finalbe.domains.prometheus.repository.realtime.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Prometheus 실시간 메트릭 데이터 정리 스케줄러
 * 설정된 보관 기간(기본 7일)이 지난 데이터를 자동으로 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusRealtimeCleanupScheduler {

    private final PrometheusRealtimeConfig config;
    private final PrometheusCpuRealtimeRepository cpuRepository;
    private final PrometheusMemoryRealtimeRepository memoryRepository;
    private final PrometheusDiskRealtimeRepository diskRepository;
    private final PrometheusNetworkRealtimeRepository networkRepository;
    private final PrometheusTemperatureRealtimeRepository temperatureRepository;

    /**
     * 오래된 메트릭 데이터 정리
     * Cron: application.yml의 prometheus.realtime.retention.cleanup-cron 설정값 사용
     * 기본값: 매일 새벽 2시 (0 0 2 * * *)
     */
    @Scheduled(cron = "${prometheus.realtime.retention.cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void cleanupOldMetrics() {
        if (!config.getCollection().isEnabled()) {
            log.debug("Prometheus realtime collection is disabled. Skipping cleanup.");
            return;
        }

        long startTime = System.currentTimeMillis();
        // Instant 타입으로 변경 (Entity의 time 필드가 Instant)
        Instant cutoffTime = Instant.now().minusSeconds(config.getRetentionSeconds());

        log.info("Starting cleanup of metrics older than {} (retention: {} days)",
                cutoffTime, config.getRetention().getDays());

        try {
            // 각 테이블에서 오래된 데이터 삭제
            int cpuDeleted = cpuRepository.deleteByTimeBefore(cutoffTime);
            int memoryDeleted = memoryRepository.deleteByTimeBefore(cutoffTime);
            int diskDeleted = diskRepository.deleteByTimeBefore(cutoffTime);
            int networkDeleted = networkRepository.deleteByTimeBefore(cutoffTime);
            int temperatureDeleted = temperatureRepository.deleteByTimeBefore(cutoffTime);

            int totalDeleted = cpuDeleted + memoryDeleted + diskDeleted + networkDeleted + temperatureDeleted;
            long elapsedTime = System.currentTimeMillis() - startTime;

            log.info("Cleanup completed in {}ms. Deleted records - CPU: {}, Memory: {}, Disk: {}, Network: {}, Temperature: {}, Total: {}",
                    elapsedTime, cpuDeleted, memoryDeleted, diskDeleted, networkDeleted, temperatureDeleted, totalDeleted);

        } catch (Exception e) {
            log.error("Error during metric cleanup", e);
        }
    }

    /**
     * 테이블별 현재 레코드 수 로깅 (매 시간 정각)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void logMetricCounts() {
        if (!config.getCollection().isEnabled()) {
            return;
        }

        try {
            long cpuCount = cpuRepository.count();
            long memoryCount = memoryRepository.count();
            long diskCount = diskRepository.count();
            long networkCount = networkRepository.count();
            long temperatureCount = temperatureRepository.count();
            long totalCount = cpuCount + memoryCount + diskCount + networkCount + temperatureCount;

            log.info("Current metric counts - CPU: {}, Memory: {}, Disk: {}, Network: {}, Temperature: {}, Total: {}",
                    cpuCount, memoryCount, diskCount, networkCount, temperatureCount, totalCount);

        } catch (Exception e) {
            log.error("Error logging metric counts", e);
        }
    }

    /**
     * 메트릭 수집 상태 체크 (5분마다)
     * 최근 1분 동안 데이터가 수집되지 않으면 경고 로그
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void checkMetricCollectionHealth() {
        if (!config.getCollection().isEnabled()) {
            return;
        }

        try {
            Instant oneMinuteAgo = Instant.now().minusSeconds(60);

            // 최근 1분간 수집된 데이터 확인 (메서드명 수정: findByTimeAfter)
            long recentCpuCount = cpuRepository.findByTimeAfter(oneMinuteAgo).size();
            long recentMemoryCount = memoryRepository.findByTimeAfter(oneMinuteAgo).size();
            long recentDiskCount = diskRepository.findByTimeAfter(oneMinuteAgo).size();
            long recentNetworkCount = networkRepository.findByTimeAfter(oneMinuteAgo).size();
            long recentTempCount = temperatureRepository.findByTimeAfter(oneMinuteAgo).size();

            // 데이터 수집이 멈춘 경우 경고
            if (recentCpuCount == 0 || recentMemoryCount == 0) {
                log.warn("Metric collection may have stopped! Recent counts - CPU: {}, Memory: {}, Disk: {}, Network: {}, Temp: {}",
                        recentCpuCount, recentMemoryCount, recentDiskCount, recentNetworkCount, recentTempCount);
            } else {
                log.debug("Metric collection is healthy. Recent 1min counts - CPU: {}, Memory: {}, Disk: {}, Network: {}, Temp: {}",
                        recentCpuCount, recentMemoryCount, recentDiskCount, recentNetworkCount, recentTempCount);
            }

        } catch (Exception e) {
            log.error("Error checking metric collection health", e);
        }
    }
}