package org.example.finalbe.domains.prometheus.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CollectionResultResponse(
        String metricType,
        boolean success,
        Instant startTime,
        Instant endTime,
        Integer recordCount,
        String errorMessage
) {
    public static CollectionResultResponse success(String metricType, Instant startTime, Instant endTime, Integer recordCount) {
        return CollectionResultResponse.builder()
                .metricType(metricType)
                .success(true)
                .startTime(startTime)
                .endTime(endTime)
                .recordCount(recordCount)
                .build();
    }

}