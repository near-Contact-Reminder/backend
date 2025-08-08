package kr.swyp.backend.member.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.member.dto.MemberDto.CheckRateResponse;
import kr.swyp.backend.member.dto.MemberDto.MemberInfoResponse;
import kr.swyp.backend.member.dto.MemberDto.MemberWithdrawRequest;
import kr.swyp.backend.member.dto.MemberDto.MigrationStatusResponse;
import kr.swyp.backend.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberInfoResponse> getMyInfo(
            @AuthenticationPrincipal MemberDetails memberDetails) {
        UUID memberId = memberDetails.getMemberId();
        MemberInfoResponse response = memberService.getMemberInfo(memberId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdrawMember(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Valid @RequestBody MemberWithdrawRequest request) {
        memberService.withdrawMember(memberDetails.getMemberId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-rate")
    public ResponseEntity<CheckRateResponse> calculateAndSaveCheckRate(
            @AuthenticationPrincipal MemberDetails memberDetails) {

        // 회원 체크율을 계산하고 저장
        CheckRateResponse response = memberService.saveMemberCheckRate(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reminder/migration-status")
    public ResponseEntity<MigrationStatusResponse> checkReminderMigrationStatus(
            @AuthenticationPrincipal MemberDetails memberDetails) {
        UUID memberId = memberDetails.getMemberId();
        MigrationStatusResponse response = memberService.checkReminderMigrationStatus(memberId);
        return ResponseEntity.ok(response);
    }
}

