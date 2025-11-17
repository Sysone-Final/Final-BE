package org.example.finalbe.domains.prometheus.dto;

import java.time.Duration;
import java.time.Instant;

public record CollectionResultResponse(
        String metricType,
        Instant startTime,
        Instant endTime,
        Duration duration,
        Integer recordsCollected,
        Boolean success,
        String errorMessage
) {
    public static CollectionResultResponse success(
            String metricType,
            Instant startTime,
            Instant endTime,
            Integer recordsCollected
    ) {
        return new CollectionResultResponse(
                metricType,
                startTime,
                endTime,
                Duration.between(startTime, endTime),
                recordsCollected,
                true,
                null
        );
    }

    public static CollectionResultResponse failure(
            String metricType,
            Instant startTime,
            Instant endTime,
            String errorMessage
    ) {
        return new CollectionResultResponse(
                metricType,
                startTime,
                endTime,
                Duration.between(startTime, endTime),
                0,
                false,
                errorMessage
        );
    }

    public String getDurationMs() {
        return duration != null ? duration.toMillis() + "ms" : "N/A";
    }
}