package kr.swyp.backend.term.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.term.dto.TermDto.TermResponse;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementRequest;
import kr.swyp.backend.term.dto.TermDto.TermsAgreementResponse;
import kr.swyp.backend.term.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @GetMapping
    public ResponseEntity<List<TermResponse>> getAllTerms() {
        List<TermResponse> terms = termService.getAllTerms();
        return ResponseEntity.ok(terms);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TermsAgreementResponse> getMyTermsAgreements(
            @AuthenticationPrincipal MemberDetails memberDetails) {
        UUID memberId = memberDetails.getMemberId();
        TermsAgreementResponse response = termService.getMemberTermsAgreements(memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> saveMyTermsAgreements(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Valid @RequestBody TermsAgreementRequest request) {
        UUID memberId = memberDetails.getMemberId();
        termService.saveMemberTermsAgreements(memberId, request);
        return ResponseEntity.ok().build();
    }
}
