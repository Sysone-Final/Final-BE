package org.example.finalbe.domains.member.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.member.dto.MemberDetailResponse;
import org.example.finalbe.domains.member.dto.MemberListResponse;
import org.example.finalbe.domains.member.dto.MemberRoleChangeRequest;
import org.example.finalbe.domains.member.dto.MemberUpdateRequest;
import org.example.finalbe.domains.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회원 관리 컨트롤러
 * 회원 조회, 수정, 삭제, 권한 변경 API 제공
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
        MemberDetailResponse member = memberService.getMemberById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "회원 조회 완료", member));
    }

    /**
     * 회원 정보 수정
     * PUT /api/members/{id}
     *
     * @param id 회원 ID
     * @param request 수정할 회원 정보
     * @return 수정된 회원 상세 정보
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
     * 회원 권한 변경
     * PUT /api/members/{id}/role
     *
     * @param id 회원 ID
     * @param request 변경할 권한 정보
     * @return 권한이 변경된 회원 상세 정보
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<CommonResDto> changeRole(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") Long id,
            @Valid @RequestBody MemberRoleChangeRequest request) {
        MemberDetailResponse member = memberService.changeRole(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "권한 변경 완료", member));
    }

    /**
     * 회원 삭제 (Soft Delete)
     * DELETE /api/members/{id}

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