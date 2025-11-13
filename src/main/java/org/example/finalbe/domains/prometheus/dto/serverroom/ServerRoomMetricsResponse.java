package org.example.finalbe.domains.prometheus.dto.serverroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.finalbe.domains.prometheus.dto.cpu.CpuMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.disk.DiskMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.memory.MemoryMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.network.NetworkMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.temperature.TemperatureMetricsResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerRoomMetricsResponse {
    private CpuMetricsResponse cpu;
    private MemoryMetricsResponse memory;
    private NetworkMetricsResponse network;
    private DiskMetricsResponse disk;
    private TemperatureMetricsResponse temperature;
}