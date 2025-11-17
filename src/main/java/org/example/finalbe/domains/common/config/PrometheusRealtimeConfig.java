package org.example.finalbe.domains.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus 실시간 메트릭 수집 설정
 * application.yml의 prometheus.realtime 설정을 바인딩
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "prometheus.realtime")
public class PrometheusRealtimeConfig {

    private Collection collection = new Collection();
    private Retention retention = new Retention();
    private Batch batch = new Batch();

    @Getter
    @Setter
    public static class Collection {
        /**
         * 실시간 메트릭 수집 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 메트릭 수집 주기 (초)
         * 기본값: 15초
         */
        private int intervalSeconds = 15;
    }

    @Getter
    @Setter
    public static class Retention {
        /**
         * 데이터 보관 기간 (일)
         * 기본값: 7일
         */
        private int days = 7;

        /**
         * 오래된 데이터 정리 Cron 표현식
         * 기본값: 매일 새벽 2시
         */
        private String cleanupCron = "0 0 2 * * *";
    }

    @Getter
    @Setter
    public static class Batch {
        /**
         * 배치 처리 크기
         * 기본값: 100
         */
        private int size = 100;
    }

    /**
     * 데이터 보관 기간을 초 단위로 반환
     * @return 보관 기간 (초)
     */
    public long getRetentionSeconds() {
        return retention.getDays() * 24L * 60L * 60L;
    }

    /**
     * 수집 주기를 밀리초 단위로 반환
     * @return 수집 주기 (밀리초)
     */
    public long getCollectionIntervalMillis() {
        return collection.getIntervalSeconds() * 1000L;
    }
}