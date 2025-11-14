package org.example.finalbe.domains.prometheus.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryCompositionResponse {
    private ZonedDateTime time;
    private Double active;
    private Double inactive;
    private Double buffers;
    private Double cached;
    private Double free;
}