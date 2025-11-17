package org.example.finalbe.domains.prometheus.dto;

import java.time.Instant;
import java.util.List;

public record CollectionSummaryResponse(
        Instant collectionTime,
        List<CollectionResultResponse> results,
        Integer totalRecords,
        Integer successCount,
        Integer failureCount,
        String totalDuration
) {
    public static CollectionSummaryResponse of(Instant collectionTime, List<CollectionResultResponse> results) {
        int total = results.stream()
                .mapToInt(r -> r.recordsCollected() != null ? r.recordsCollected() : 0)
                .sum();

        long successCount = results.stream()
                .filter(CollectionResultResponse::success)
                .count();

        long failureCount = results.stream()
                .filter(r -> !r.success())
                .count();

        long totalMs = results.stream()
                .filter(r -> r.duration() != null)
                .mapToLong(r -> r.duration().toMillis())
                .sum();

        return new CollectionSummaryResponse(
                collectionTime,
                results,
                total,
                (int) successCount,
                (int) failureCount,
                totalMs + "ms"
        );
    }

    public boolean hasFailures() {
        return failureCount > 0;
    }
}