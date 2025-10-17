package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RackBulkUploadResultResponse(
        Integer totalRows,
        Integer successCount,
        Integer failCount,
        List<UploadResult> results
) {
    @Builder
    public record UploadResult(
            Integer rowNumber,
            String rackName,
            Boolean success,
            String message,
            Long rackId
    ) {
    }
}