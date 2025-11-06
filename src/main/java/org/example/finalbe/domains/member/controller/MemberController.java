package org.example.finalbe.domains.member.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.member.dto.MemberListResponse;
import org.example.finalbe.domains.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회원 관리 컨트롤러
 * 회원 목록 조회 API 제공
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Validated
public class MemberController {

    private final MemberService memberService;

    /**
     * 로그인한 사용자가 속한 회사의 회원 목록 조회
     * GET /api/members
     *
     * @return 같은 회사에 속한 회원 목록
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getMembersByCompany() {
        List<MemberListResponse> members = memberService.getMembersByCompany();
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 목록 조회 완료", members));
    }

    /**
     * 회원 상세 조회
     * GET /api/members/{id}
     *
     * @param id 회원 ID
     * @return 회원 상세 정보 (같은 회사 회원만 조회 가능)
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getMemberById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long id) {
        MemberListResponse member = memberService.getMemberById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 조회 완료", member));
    }
}