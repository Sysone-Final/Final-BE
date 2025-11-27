/**
 * 작성자: 황요한
 * 사용자 권한 Enum (관리자 / 운영자 / 조회자)
 */
package org.example.finalbe.domains.common.enumdir;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    ADMIN("ADMIN", "관리자"),
    OPERATOR("OPERATOR", "운영자"),
    VIEWER("VIEWER", "조회자");

    private final String key;    // 권한 키
    private final String string; // 권한 설명
}
