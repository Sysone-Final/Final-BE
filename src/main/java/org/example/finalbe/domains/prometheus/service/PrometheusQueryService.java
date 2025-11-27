/**
 * 작성자: 황요한
 * Prometheus 서버에 PromQL 쿼리를 요청하여 메트릭 결과를 조회하는 서비스
 */
package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.config.PrometheusProperties;
import org.example.finalbe.domains.prometheus.dto.PrometheusResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrometheusQueryService {

    private final WebClient prometheusWebClient;
    private final PrometheusProperties properties;

    // PromQL 쿼리를 실행하고 결과를 반환
    public List<PrometheusResponse.PrometheusResult> query(String promql) {
        try {
            log.debug("🔍 PromQL: {}", promql);

            PrometheusResponse response = prometheusWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/query")
                            .queryParam("query", promql)
                            .build())
                    .retrieve()
                    .bodyToMono(PrometheusResponse.class)
                    .timeout(Duration.ofMillis(properties.getClient().getReadTimeout()))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(throwable -> !(throwable instanceof WebClientResponseException.NotFound))
                            .doBeforeRetry(retrySignal ->
                                    log.warn("⚠️ Prometheus 쿼리 재시도 중... ({}회): {}",
                                            retrySignal.totalRetries() + 1, promql))
                            .onRetryExhaustedThrow((spec, signal) -> {
                                log.error("❌ Prometheus 쿼리 재시도 실패: {}", promql);
                                return signal.failure();
                            }))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("❌ Prometheus API 오류 [{}]: {} - Query: {}",
                                ex.getStatusCode(), ex.getMessage(), promql);
                        return Mono.empty();
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("❌ Prometheus 쿼리 실패: {} - {}",
                                promql, ex.getClass().getSimpleName());
                        return Mono.empty();
                    })
                    .block();

            if (response != null && "success".equals(response.status())) {
                List<PrometheusResponse.PrometheusResult> results =
                        response.data() != null ? response.data().result() : Collections.emptyList();
                log.debug("  ✓ 결과: {} 개", results.size());
                return results;
            } else if (response != null) {
                log.warn("❌ Prometheus 쿼리 실패: {} - {}", response.errorType(), response.error());
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Prometheus 쿼리 예외: {} - {}", promql, e.getMessage());
            return Collections.emptyList();
        }
    }
}
