package org.example.finalbe.domains.member.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주소 임베디드 타입
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Address {
    private String city; // 도시
    private String street; // 상세 주소
    private String zipcode; // 우편번호
}