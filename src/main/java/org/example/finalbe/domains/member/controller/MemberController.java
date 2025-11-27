/**
 * 작성자: 황요한
 * 회원 관리 API 컨트롤러
 */
package org.example.finalbe.domains.member.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.member.dto.*;
import org.example.finalbe.domains.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Validated
public class MemberController {

    private final MemberService memberService;

    /**
     * 회사별 회원 목록 조회
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<CommonResDto> getMembersByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId) {
        List<MemberListResponse> members = memberService.getMembersByCompany(companyId);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 목록 조회 완료", members));
    }

    /**
     * 회원 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getMemberById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long id) {
        MemberDetailResponse member = memberService.getMemberById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 조회 완료", member));
    }

    /**
     * 회원 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<CommonResDto> updateMember(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long id,
            @Valid @RequestBody MemberUpdateRequest request) {
        MemberDetailResponse member = memberService.updateMember(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 정보 수정 완료", member));
    }

    /**
     * 회원 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResDto> deleteMember(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 삭제 완료", null));
    }
}