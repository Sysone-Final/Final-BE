package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.PrometheusResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrometheusQueryService {

    private final WebClient prometheusWebClient;

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
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("❌ Prometheus API 오류 [{}]: {}", ex.getStatusCode(), ex.getMessage());
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