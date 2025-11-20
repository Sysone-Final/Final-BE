package org.example.finalbe.domains.common.enumdir;

public enum MetricType {
    CPU("CPU"),
    MEMORY("메모리"),
    DISK("디스크"),
    TEMPERATURE("온도"),
    HUMIDITY("습도"),
    NETWORK("네트워크");

    private final String description;

    MetricType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}