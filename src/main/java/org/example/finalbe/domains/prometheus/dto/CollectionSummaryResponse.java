package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record CollectionSummaryResponse(
        Instant collectionStart,
        Instant collectionEnd,
        Long totalDuration,
        Integer totalRecords,
        Integer successCount,
        Integer failureCount,
        List<CollectionResultResponse> results
) {
    public static CollectionSummaryResponse of(Instant collectionStart, List<CollectionResultResponse> results) {
        Instant collectionEnd = Instant.now();
        long totalDuration = collectionEnd.toEpochMilli() - collectionStart.toEpochMilli();

        int totalRecords = results.stream()
                .mapToInt(r -> r.recordCount() != null ? r.recordCount() : 0)
                .sum();

        long successCount = results.stream()
                .filter(CollectionResultResponse::success)
                .count();

        long failureCount = results.stream()
                .filter(r -> !r.success())
                .count();

        return CollectionSummaryResponse.builder()
                .collectionStart(collectionStart)
                .collectionEnd(collectionEnd)
                .totalDuration(totalDuration)
                .totalRecords(totalRecords)
                .successCount((int) successCount)
                .failureCount((int) failureCount)
                .results(results)
                .build();
    }
}