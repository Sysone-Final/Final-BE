package org.example.finalbe.domains.prometheus.dto.serverroom;

import org.example.finalbe.domains.prometheus.dto.cpu.CpuMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.disk.DiskMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.memory.MemoryMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.network.NetworkMetricsResponse;
import org.example.finalbe.domains.prometheus.dto.temperature.TemperatureMetricsResponse;

public record ServerRoomMetricsResponse(
        CpuMetricsResponse cpu,
        MemoryMetricsResponse memory,
        NetworkMetricsResponse network,
        DiskMetricsResponse disk,
        TemperatureMetricsResponse temperature
) {
}