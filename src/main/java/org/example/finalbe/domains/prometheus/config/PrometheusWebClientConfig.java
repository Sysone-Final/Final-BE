package org.example.finalbe.domains.prometheus.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PrometheusWebClientConfig {

    private final PrometheusProperties properties;

    @Bean
    public WebClient prometheusWebClient() {
        // ✅ Connection Pool 설정 추가
        ConnectionProvider connectionProvider = ConnectionProvider.builder("prometheus-pool")
                .maxConnections(50)                              // 최대 50개 동시 연결
                .pendingAcquireTimeout(Duration.ofSeconds(45))   // 연결 대기 타임아웃 45초
                .maxIdleTime(Duration.ofSeconds(20))             // 유휴 연결 유지 시간 20초
                .maxLifeTime(Duration.ofSeconds(60))             // 연결 최대 수명 60초
                .evictInBackground(Duration.ofSeconds(120))      // 백그라운드 정리 주기
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        properties.getClient().getConnectTimeout())
                .responseTimeout(Duration.ofMillis(
                        properties.getClient().getReadTimeout()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                properties.getClient().getReadTimeout() / 1000, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                properties.getClient().getReadTimeout() / 1000, TimeUnit.SECONDS)))
                .doOnError((req, err) ->
                                log.error("❌ Prometheus HTTP 연결 에러: {} - {}",
                                        req.uri(), err.getMessage()),
                        (res, err) ->
                                log.error("❌ Prometheus HTTP 응답 에러: Status={}, Error={}",
                                        res.status(), err.getMessage()));

        // ✅ 버퍼 사이즈 증가 (대용량 응답 처리)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(20 * 1024 * 1024))  // 20MB
                .build();

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}