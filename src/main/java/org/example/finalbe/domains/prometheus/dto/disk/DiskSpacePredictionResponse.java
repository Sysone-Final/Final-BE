package org.example.finalbe.domains.prometheus.dto.disk;

import java.time.ZonedDateTime;

public record DiskSpacePredictionResponse(
        ZonedDateTime time,
        Double freeBytes,
        Double usedBytes,
        Double usagePercent,
        Boolean isPrediction,
        Double predictedUsagePercent
) {
    public static DiskSpacePredictionResponse actual(
            ZonedDateTime time,
            Double freeBytes,
            Double usedBytes,
            Double usagePercent
    ) {
        return new DiskSpacePredictionResponse(
                time,
                freeBytes,
                usedBytes,
                usagePercent,
                false,
                null
        );
    }

    public static DiskSpacePredictionResponse predicted(
            ZonedDateTime time,
            Double predictedUsagePercent
    ) {
        return new DiskSpacePredictionResponse(
                time,
                null,
                null,
                null,
                true,
                predictedUsagePercent
        );
    }
}