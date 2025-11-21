package org.example.finalbe.domains.common.enumdir;

public enum AlertLevel {
    WARNING("경고", 1),
    CRITICAL("위험", 2);

    private final String description;
    private final int severity;

    AlertLevel(String description, int severity) {
        this.description = description;
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isMoreSevereThan(AlertLevel other) {
        return this.severity > other.severity;
    }
}