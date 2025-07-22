package kr.swyp.backend.messaging.controller;

import jakarta.validation.Valid;
import java.util.Map;
import kr.swyp.backend.member.dto.MemberDetails;
import kr.swyp.backend.messaging.dto.TokenDto.RegisterAppPushTokenRequest;
import kr.swyp.backend.messaging.dto.TokenDto.UnregisterAppPushTokenRequest;
import kr.swyp.backend.messaging.service.AppPushMessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messaging")
@RequiredArgsConstructor
public class AppPushMessagingController {

    private final AppPushMessagingService appPushMessagingService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerDevice(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Valid @RequestBody RegisterAppPushTokenRequest request) {
        appPushMessagingService.registerDevice(memberDetails.getMemberId(), request);
        return ResponseEntity.ok(Map.of("message", "기기 등록이 완료되었습니다."));
    }

    @DeleteMapping("/unregister")
    public ResponseEntity<Void> unregisterDevice(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Valid @RequestBody UnregisterAppPushTokenRequest request) {
        appPushMessagingService.unregisterDevice(memberDetails.getMemberId(), request);
        return ResponseEntity.noContent().build();
    }
}
