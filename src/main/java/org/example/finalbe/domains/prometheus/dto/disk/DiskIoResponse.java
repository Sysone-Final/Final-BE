package org.example.finalbe.domains.prometheus.dto.disk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiskIoResponse {
    private ZonedDateTime time;
    private Double readBytesPerSec;
    private Double writeBytesPerSec;
    private Double readIops;
    private Double writeIops;
    private Double ioUtilizationPercent;
}