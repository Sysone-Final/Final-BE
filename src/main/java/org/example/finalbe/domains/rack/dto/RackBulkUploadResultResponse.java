/**
 * 작성자: 황요한
 * 랙 일괄 등록 결과 응답 DTO
 */
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

    /**
     * 업로드 결과 개별 행 정보
     */
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
