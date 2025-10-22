package org.example.finalbe.domains.common.enumdir;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DataCenterStatus {
    ACTIVE("ACTIVE", "운영중"),
    MAINTENANCE("MAINTENANCE", "점검중"),
    CLOSED("CLOSED", "폐쇄");

    private final String key;
    private final String description;
}
