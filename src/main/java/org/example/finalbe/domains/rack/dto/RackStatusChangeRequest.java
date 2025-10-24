package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.RackStatus;

@Builder
public record RackStatusChangeRequest(
        RackStatus status,
        String reason
) {
}