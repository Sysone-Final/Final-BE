// src/main/java/org/example/finalbe/domains/member/controller/MemberController.java

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

/**
 * 회원 관리 API 컨트롤러
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Validated
public class MemberController {

    private final MemberService memberService;

    /**
     * 회사별 회원 목록 조회
     * GET /api/members/company/{companyId}
     *
     * @param companyId 회사 ID
     * @return 회원 목록
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
     * GET /api/members/{id}
     *
     * @param id 회원 ID
     * @return 회원 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getMemberById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long id) {
        MemberDetailResponse member = memberService.getMemberById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 조회 완료", member));
    }

    /**
     * 회원 정보 수정 (권한 변경 포함)
     * PUT /api/members/{id}
     *
     * @param id 회원 ID
     * @param request 수정할 회원 정보 (권한 변경 포함)
     * @return 수정된 회원 상세 정보
     *
     * 권한:
     * - 본인의 정보는 누구나 수정 가능
     * - OPERATOR는 같은 회사의 다른 회원도 수정 가능
     * - ADMIN은 모든 회원 수정 가능
     * - 권한 변경(role 필드)도 함께 처리 가능
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
     * 회원 삭제 (Soft Delete)
     * DELETE /api/members/{id}
     *
     * @param id 회원 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResDto> deleteMember(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 삭제 완료", null));
    }
}