package org.example.finalbe.domains.prometheus.dto.disk;

public record InodeUsageResponse(
        Integer deviceId,
        Integer mountpointId,
        Double totalInodes,
        Double freeInodes,
        Double usedInodes,
        Double inodeUsagePercent
) {
    public static InodeUsageResponse from(Object[] row) {
        return new InodeUsageResponse(
                (Integer) row[0],
                (Integer) row[1],
                row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                row[3] != null ? ((Number) row[3]).doubleValue() : 0.0,
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0,
                row[5] != null ? ((Number) row[5]).doubleValue() : 0.0
        );
    }
}