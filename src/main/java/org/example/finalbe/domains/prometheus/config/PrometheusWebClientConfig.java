// 작성자: 황요한
// Prometheus WebClient 설정

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

        // 커넥션 풀 설정
        ConnectionProvider pool = ConnectionProvider.builder("prometheus-pool")
                .maxConnections(50)
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // HTTP 설정
        HttpClient httpClient = HttpClient.create(pool)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getClient().getConnectTimeout())
                .responseTimeout(Duration.ofMillis(properties.getClient().getReadTimeout()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(properties.getClient().getReadTimeout() / 1000, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getClient().getReadTimeout() / 1000, TimeUnit.SECONDS)))
                .doOnError((req, err) ->
                                log.error("Prometheus 요청 오류: {} - {}", req.uri(), err.getMessage()),
                        (res, err) ->
                                log.error("Prometheus 응답 오류: Status={}, Error={}", res.status(), err.getMessage()));

        // 버퍼 증가(대용량 응답 대비)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
