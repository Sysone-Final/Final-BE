package org.example.finalbe.domains.prometheus.dto.disk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InodeUsageResponse {
    private Integer deviceId;
    private Integer mountpointId;
    private Double totalInodes;
    private Double freeInodes;
    private Double usedInodes;
    private Double inodeUsagePercent;
}