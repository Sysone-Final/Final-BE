package org.example.finalbe.domains.common.enumdir;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN("ADMIN", "관리자"),
    OPERATOR("OPERATOR", "운영자"),
    VIEWER("VIEWER","조회자");

    private final String key;
    private final String string;
}