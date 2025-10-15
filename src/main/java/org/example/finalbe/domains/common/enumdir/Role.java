package org.example.finalbe.domains.common.enumdir;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN("ROLE_ADMIN", "관리자"),
    OPERATOR("ROLE_OPERATOR", "운영자"),
    VIEWER("ROLE_VIEWER","조회자");

    private final String key;
    private final String string;
}