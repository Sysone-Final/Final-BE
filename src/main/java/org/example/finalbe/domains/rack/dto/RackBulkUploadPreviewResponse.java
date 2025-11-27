/**
 * 작성자: 황요한
 * 랙 일괄 등록 미리보기 응답 DTO
 */
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

    /**
     * 미리보기 개별 행 정보
     */
    @Builder
    public record PreviewRow(
            Integer rowNumber,
            String rackName,
            String gridX,
            String gridY,
            Integer totalUnits,
            String status,
            Boolean isValid,
            String errorMessage
    ) {
    }

    /**
     * 유효성 오류 정보
     */
    @Builder
    public record ValidationError(
            Integer rowNumber,
            String field,
            String message
    ) {
    }
}
