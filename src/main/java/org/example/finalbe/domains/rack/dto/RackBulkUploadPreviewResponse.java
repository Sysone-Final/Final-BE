package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RackBulkUploadPreviewResponse(
        Integer totalRows,
        Integer validRows,
        Integer invalidRows,
        List<PreviewRow> previewData,
        List<ValidationError> errors
) {
    @Builder
    public record PreviewRow(
            Integer rowNumber,
            String rackName,
            String groupNumber,
            String rackLocation,
            Integer totalUnits,
            String status,
            Boolean isValid,
            String errorMessage
    ) {
    }

    @Builder
    public record ValidationError(
            Integer rowNumber,
            String field,
            String message
    ) {
    }
}